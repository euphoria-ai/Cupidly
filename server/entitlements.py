"""RevenueCat entitlement lookups for the "Hook Pro" subscription.

The Android app is the source of purchases (Google Play Billing) and RevenueCat
is the source of truth for whether those purchases are still active. The server
never trusts the client's word for it — it asks RevenueCat directly, keyed by
the same stable per-install app user id the client sends in X-App-User-Id.

Answers are cached for a short TTL so a chatty client cannot turn one Groq call
into one RevenueCat call as well.
"""

import asyncio
import os
import time
from datetime import datetime, timezone
from typing import Dict, Optional

import httpx

# --- Config (module-level so tests can monkeypatch) ---

REVENUECAT_API_BASE = os.getenv("REVENUECAT_API_BASE", "https://api.revenuecat.com/v1")
REVENUECAT_SECRET_KEY = os.getenv("REVENUECAT_SECRET_KEY")
ENTITLEMENT_ID = os.getenv("REVENUECAT_ENTITLEMENT_ID", "Hook Pro")

try:
    CACHE_TTL_SECONDS = float(os.getenv("ENTITLEMENT_CACHE_TTL_SECONDS", "60"))
except ValueError:
    print("[WARN] ENTITLEMENT_CACHE_TTL_SECONDS is not a number — using 60s")
    CACHE_TTL_SECONDS = 60.0

REQUEST_TIMEOUT = httpx.Timeout(connect=3.0, read=5.0, write=5.0, pool=3.0)


# --- Cache ---

class _CacheEntry:
    """A remembered answer. Kept past its TTL so it can be served stale."""

    __slots__ = ("is_pro", "fetched_at")

    def __init__(self, is_pro: bool, fetched_at: float):
        self.is_pro = is_pro
        self.fetched_at = fetched_at


_cache: Dict[str, _CacheEntry] = {}
_client: Optional[httpx.AsyncClient] = None
_client_lock = asyncio.Lock()
_warned_missing_key = False


def clear_cache() -> None:
    """Drop every cached entitlement. Used by tests and manual re-checks."""
    _cache.clear()


async def _get_client() -> httpx.AsyncClient:
    """One shared client so the TLS handshake to RevenueCat is paid once."""
    global _client
    if _client is None:
        async with _client_lock:
            if _client is None:
                _client = httpx.AsyncClient(timeout=REQUEST_TIMEOUT)
    return _client


async def aclose() -> None:
    global _client
    if _client is not None:
        await _client.aclose()
        _client = None


# --- Parsing ---

def _parse_rc_datetime(value: str) -> Optional[datetime]:
    """RevenueCat sends ISO-8601 UTC, e.g. 2026-01-31T12:00:00Z."""
    if not isinstance(value, str) or not value.strip():
        return None
    text = value.strip()
    if text.endswith("Z"):
        text = text[:-1] + "+00:00"
    try:
        parsed = datetime.fromisoformat(text)
    except ValueError:
        return None
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def _single_entitlement_active(entitlement: object) -> bool:
    """True when one entitlement record is present and unexpired.

    A null expires_date means a non-expiring (lifetime) grant, which is how
    RevenueCat reports both lifetime products and sandbox grants.
    """
    if not isinstance(entitlement, dict):
        return False

    expires_raw = entitlement.get("expires_date")
    if expires_raw is None:
        return True

    expires_at = _parse_rc_datetime(expires_raw)
    if expires_at is None:
        # Unparseable date — treat as expired rather than handing out Pro.
        print(f"[ERR] revenuecat: unparseable expires_date {expires_raw!r}")
        return False
    return expires_at > datetime.now(timezone.utc)


def entitlement_active(subscriber: Optional[dict]) -> bool:
    """True when this subscriber currently holds Pro.

    Prefers the configured ENTITLEMENT_ID, but for this single-entitlement app
    also treats *any* active entitlement as Pro. That guards against a
    RevenueCat dashboard identifier that doesn't match ENTITLEMENT_ID, which
    would otherwise leave a paying subscriber looking un-entitled to the server.
    """
    entitlements = (subscriber or {}).get("entitlements") or {}
    if not isinstance(entitlements, dict):
        return False

    if _single_entitlement_active(entitlements.get(ENTITLEMENT_ID)):
        return True

    return any(_single_entitlement_active(e) for e in entitlements.values())


# --- Public API ---

async def is_pro(app_user_id: str) -> bool:
    """Is this install entitled to Hook Pro?

    Never raises: on any RevenueCat trouble we fall back to a stale cached
    answer, and failing that to False (the user drops back to the free
    allowance instead of getting unlimited generations for free).
    """
    global _warned_missing_key

    if not app_user_id:
        return False

    cached = _cache.get(app_user_id)
    if cached is not None and (time.monotonic() - cached.fetched_at) < CACHE_TTL_SECONDS:
        return cached.is_pro

    if not REVENUECAT_SECRET_KEY:
        if not _warned_missing_key:
            _warned_missing_key = True
            print("[WARN] REVENUECAT_SECRET_KEY is not set — nobody will be treated "
                  "as Pro; every user is limited to the free allowance.")
        return False

    url = f"{REVENUECAT_API_BASE.rstrip('/')}/subscribers/{app_user_id}"
    headers = {
        "Authorization": f"Bearer {REVENUECAT_SECRET_KEY}",
        "Accept": "application/json",
    }

    try:
        client = await _get_client()
        response = await client.get(url, headers=headers)
    except Exception as e:
        print(f"[ERR] revenuecat unreachable for {app_user_id}: {e}")
        return _stale_or_false(cached, app_user_id)

    # 404 = subscriber RevenueCat has never seen. Perfectly normal for a fresh
    # install that has not opened the paywall yet, so not an error.
    if response.status_code == 404:
        _cache[app_user_id] = _CacheEntry(False, time.monotonic())
        return False

    if response.status_code != 200:
        print(f"[ERR] revenuecat HTTP {response.status_code} for {app_user_id}: "
              f"{response.text[:200]}")
        return _stale_or_false(cached, app_user_id)

    try:
        payload = response.json()
        active = entitlement_active(payload.get("subscriber"))
    except Exception as e:
        print(f"[ERR] revenuecat bad payload for {app_user_id}: {e}")
        return _stale_or_false(cached, app_user_id)

    _cache[app_user_id] = _CacheEntry(active, time.monotonic())
    return active


def _stale_or_false(cached: Optional[_CacheEntry], app_user_id: str) -> bool:
    """Prefer a stale answer over guessing; guess False when there is none."""
    if cached is not None:
        print(f"[WARN] revenuecat: serving stale entitlement "
              f"(is_pro={cached.is_pro}) for {app_user_id}")
        return cached.is_pro
    return False
