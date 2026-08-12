"""Tests for API-key auth, the free-generation allowance, and Pro entitlements.

Nothing here touches the network: Groq, RevenueCat and Supabase are all faked.
"""

import json
import os
import sys
import tempfile

import httpx
import pytest

# Must be set before importing main — it reads config at import time.
# load_dotenv() does not override already-set variables, so a developer's real
# .env cannot leak into these tests.
os.environ["GROQ_API_KEY"] = "test-groq-key"
os.environ["APP_API_KEY"] = "test-app-api-key"
os.environ["FREE_GENERATION_LIMIT"] = "3"
os.environ["ALLOWANCE_DB_PATH"] = os.path.join(
    tempfile.mkdtemp(prefix="cupidly-test-"), "allowance.db"
)
os.environ.pop("SUPABASE_URL", None)
os.environ.pop("SUPABASE_SERVICE_ROLE_KEY", None)
os.environ.pop("ALLOWED_ORIGINS", None)

from fastapi.testclient import TestClient  # noqa: E402

import entitlements  # noqa: E402
import main  # noqa: E402
from store import (  # noqa: E402
    SqliteAllowanceStore,
    SupabaseAllowanceStore,
    get_store,
)

API_KEY = "test-app-api-key"
USER = "11111111-2222-3333-4444-555555555555"
FREE_LIMIT = 3


# --- Fakes ---------------------------------------------------------------

class _FakeMessage:
    def __init__(self, content):
        self.content = content


class _FakeChoice:
    def __init__(self, content):
        self.message = _FakeMessage(content)


class _FakeCompletion:
    def __init__(self, content):
        self.choices = [_FakeChoice(content)]


GOOD_PAYLOAD = json.dumps({
    "suggestion_1": "bold of you to assume i'd say no",
    "suggestion_2": "you had me at hello",
    "suggestion_3": "prove it",
    "updated_context_summary": "Playful banter with the match.",
})


def _mock_groq(monkeypatch, *, content=GOOD_PAYLOAD, error=None):
    """Replace the Groq call. Returns a list that records each invocation."""
    calls = []

    async def fake_create(*args, **kwargs):
        calls.append(kwargs)
        if error is not None:
            raise error
        return _FakeCompletion(content)

    monkeypatch.setattr(main._client.chat.completions, "create", fake_create)
    return calls


def _mock_is_pro(monkeypatch, value):
    async def fake_is_pro(app_user_id):
        return value

    monkeypatch.setattr(main, "is_pro", fake_is_pro)


def _headers(api_key=API_KEY, user_id=USER):
    headers = {}
    if api_key is not None:
        headers["X-Api-Key"] = api_key
    if user_id is not None:
        headers["X-App-User-Id"] = user_id
    return headers


def _body():
    return {
        "screenshot_base64": "ZmFrZS1qcGVn",
        "preferences": {
            "style": "LOWERCASE",
            "tone": "FUNNY",
            "flirt_level": "MEDIUM",
            "reply_length": "SHORT",
            "emoji_use": "MINIMAL",
        },
    }


@pytest.fixture
def store(monkeypatch):
    """A throwaway in-memory allowance store for each test."""
    fresh = SqliteAllowanceStore(":memory:")
    monkeypatch.setattr(main, "store", fresh)
    return fresh


@pytest.fixture
def client(store):
    entitlements.clear_cache()
    with TestClient(main.app) as test_client:
        yield test_client


# --- Auth ----------------------------------------------------------------

def test_missing_api_key_is_401(client, monkeypatch):
    _mock_is_pro(monkeypatch, False)
    calls = _mock_groq(monkeypatch)
    response = client.post("/generate-replies", json=_body(),
                           headers=_headers(api_key=None))
    assert response.status_code == 401
    assert calls == []  # never reached Groq


def test_wrong_api_key_is_401(client, monkeypatch):
    _mock_is_pro(monkeypatch, False)
    calls = _mock_groq(monkeypatch)
    response = client.post("/generate-replies", json=_body(),
                           headers=_headers(api_key="not-the-key"))
    assert response.status_code == 401
    assert calls == []


def test_api_key_check_is_skipped_when_unconfigured(client, monkeypatch):
    """Local dev with no APP_API_KEY still works."""
    monkeypatch.setattr(main, "APP_API_KEY", None)
    _mock_is_pro(monkeypatch, False)
    _mock_groq(monkeypatch)
    response = client.post("/generate-replies", json=_body(),
                           headers=_headers(api_key=None))
    assert response.status_code == 200


@pytest.mark.parametrize("user_id", [
    None,          # header absent
    "",            # empty
    "   ",         # whitespace only
    "not a uuid",  # spaces
    "user@host",   # illegal charset
    "../etc",      # path traversal attempt
    "x" * 129,     # too long
])
def test_bad_app_user_id_is_400(client, monkeypatch, user_id):
    _mock_is_pro(monkeypatch, False)
    calls = _mock_groq(monkeypatch)
    response = client.post("/generate-replies", json=_body(),
                           headers=_headers(user_id=user_id))
    assert response.status_code == 400
    assert calls == []


def test_max_length_app_user_id_is_accepted(client, monkeypatch):
    _mock_is_pro(monkeypatch, False)
    _mock_groq(monkeypatch)
    response = client.post("/generate-replies", json=_body(),
                           headers=_headers(user_id="a" * 128))
    assert response.status_code == 200


# --- Allowance -----------------------------------------------------------

def test_free_user_under_limit_succeeds_and_increments(client, store, monkeypatch):
    _mock_is_pro(monkeypatch, False)
    _mock_groq(monkeypatch)

    first = client.post("/generate-replies", json=_body(), headers=_headers())
    assert first.status_code == 200
    payload = first.json()
    assert len(payload["suggestions"]) == 3
    assert payload["is_pro"] is False
    assert payload["free_used"] == 1
    assert payload["free_limit"] == FREE_LIMIT
    assert payload["remaining"] == FREE_LIMIT - 1
    # Existing fields are untouched for older clients.
    assert payload["context"]["summary"] == "Playful banter with the match."

    second = client.post("/generate-replies", json=_body(), headers=_headers())
    assert second.status_code == 200
    assert second.json()["free_used"] == 2
    assert second.json()["remaining"] == FREE_LIMIT - 2

    assert client.get("/me", headers=_headers()).json()["free_used"] == 2


def test_free_user_at_limit_gets_402(client, store, monkeypatch):
    _mock_is_pro(monkeypatch, False)
    calls = _mock_groq(monkeypatch)

    for _ in range(FREE_LIMIT):
        assert client.post("/generate-replies", json=_body(),
                           headers=_headers()).status_code == 200
    assert len(calls) == FREE_LIMIT

    blocked = client.post("/generate-replies", json=_body(), headers=_headers())
    assert blocked.status_code == 402
    assert blocked.json() == {
        "error": "allowance_exhausted",
        "free_limit": FREE_LIMIT,
        "used": FREE_LIMIT,
    }
    # Blocked before spending any Groq quota, and the counter did not move.
    assert len(calls) == FREE_LIMIT


def test_pro_user_past_the_limit_is_unlimited(client, store, monkeypatch):
    _mock_is_pro(monkeypatch, True)
    _mock_groq(monkeypatch)

    import asyncio
    asyncio.run(_spend(store, USER, FREE_LIMIT + 10))

    response = client.post("/generate-replies", json=_body(), headers=_headers())
    assert response.status_code == 200
    payload = response.json()
    assert payload["is_pro"] is True
    assert len(payload["suggestions"]) == 3
    # Pro generations never touch the free counter.
    assert payload["free_used"] == FREE_LIMIT + 10


def test_allowance_not_spent_when_generation_fails(client, store, monkeypatch):
    _mock_is_pro(monkeypatch, False)
    _mock_groq(monkeypatch, error=RuntimeError("groq exploded"))

    response = client.post("/generate-replies", json=_body(), headers=_headers())
    assert response.status_code == 500

    me = client.get("/me", headers=_headers()).json()
    assert me["free_used"] == 0
    assert me["remaining"] == FREE_LIMIT


def test_allowance_not_spent_when_model_returns_garbage(client, store, monkeypatch):
    _mock_is_pro(monkeypatch, False)
    _mock_groq(monkeypatch, content="not json at all")

    response = client.post("/generate-replies", json=_body(), headers=_headers())
    assert response.status_code == 500
    assert client.get("/me", headers=_headers()).json()["free_used"] == 0


# --- /me -----------------------------------------------------------------

def test_me_reports_remaining(client, store, monkeypatch):
    _mock_is_pro(monkeypatch, False)

    fresh = client.get("/me", headers=_headers()).json()
    assert fresh == {
        "app_user_id": USER,
        "is_pro": False,
        "free_used": 0,
        "free_limit": FREE_LIMIT,
        "remaining": FREE_LIMIT,
    }

    import asyncio
    asyncio.run(_spend(store, USER, 2))

    used = client.get("/me", headers=_headers()).json()
    assert used["free_used"] == 2
    assert used["remaining"] == FREE_LIMIT - 2

    # Never negative, even if the counter overshoots.
    asyncio.run(_spend(store, USER, 5))
    assert client.get("/me", headers=_headers()).json()["remaining"] == 0


def test_me_requires_auth(client, monkeypatch):
    _mock_is_pro(monkeypatch, False)
    assert client.get("/me", headers=_headers(api_key="nope")).status_code == 401
    assert client.get("/me", headers=_headers(user_id="bad id")).status_code == 400


def test_me_reports_pro(client, monkeypatch):
    _mock_is_pro(monkeypatch, True)
    assert client.get("/me", headers=_headers()).json()["is_pro"] is True


# --- CORS ----------------------------------------------------------------

def test_cors_is_locked_down_by_default():
    assert main.ALLOWED_ORIGINS == []


# --- SQLite store --------------------------------------------------------

async def _spend(store_obj, user, times):
    for _ in range(times):
        await store_obj.increment(user)


async def test_sqlite_store_counts_per_user():
    store_obj = SqliteAllowanceStore(":memory:")
    try:
        assert await store_obj.get_used("nobody") == 0
        assert await store_obj.increment("a") == 1
        assert await store_obj.increment("a") == 2
        assert await store_obj.increment("b") == 1
        assert await store_obj.get_used("a") == 2
        assert await store_obj.get_used("b") == 1
    finally:
        await store_obj.close()


async def test_sqlite_store_increments_are_not_lost_concurrently():
    import asyncio
    store_obj = SqliteAllowanceStore(":memory:")
    try:
        await asyncio.gather(*(store_obj.increment("racer") for _ in range(50)))
        assert await store_obj.get_used("racer") == 50
    finally:
        await store_obj.close()


async def test_sqlite_store_persists_across_reopen(tmp_path):
    path = str(tmp_path / "allowance.db")
    first = SqliteAllowanceStore(path)
    await first.increment("persisted")
    await first.close()

    second = SqliteAllowanceStore(path)
    try:
        assert await second.get_used("persisted") == 1
    finally:
        await second.close()


def test_get_store_defaults_to_sqlite(monkeypatch, tmp_path):
    monkeypatch.delenv("SUPABASE_URL", raising=False)
    monkeypatch.delenv("SUPABASE_SERVICE_ROLE_KEY", raising=False)
    monkeypatch.setenv("ALLOWANCE_DB_PATH", str(tmp_path / "a.db"))
    assert isinstance(get_store(), SqliteAllowanceStore)


def test_get_store_picks_supabase_when_fully_configured(monkeypatch, tmp_path):
    monkeypatch.setenv("SUPABASE_URL", "https://example.supabase.co")
    monkeypatch.setenv("SUPABASE_SERVICE_ROLE_KEY", "service-role-key")
    monkeypatch.setenv("ALLOWANCE_DB_PATH", str(tmp_path / "a.db"))
    assert isinstance(get_store(), SupabaseAllowanceStore)


def test_get_store_falls_back_when_half_configured(monkeypatch, tmp_path):
    monkeypatch.setenv("SUPABASE_URL", "https://example.supabase.co")
    monkeypatch.delenv("SUPABASE_SERVICE_ROLE_KEY", raising=False)
    monkeypatch.setenv("ALLOWANCE_DB_PATH", str(tmp_path / "a.db"))
    assert isinstance(get_store(), SqliteAllowanceStore)


# --- Supabase store (SDK faked, never hits the network) ------------------

class _FakeResponse:
    def __init__(self, data):
        self.data = data


class _FakeQuery:
    def __init__(self, data):
        self._data = data

    def select(self, *args, **kwargs):
        return self

    def eq(self, *args, **kwargs):
        return self

    def limit(self, *args, **kwargs):
        return self

    async def execute(self):
        return _FakeResponse(self._data)


class _FakeSupabaseClient:
    def __init__(self, rows=None, rpc_result=None):
        self.rows = rows if rows is not None else []
        self.rpc_result = rpc_result
        self.rpc_calls = []

    def table(self, name):
        return _FakeQuery(self.rows)

    def rpc(self, fn, params):
        self.rpc_calls.append((fn, params))
        return _FakeQuery(self.rpc_result)


def _fake_supabase_module(monkeypatch, create):
    import types
    module = types.ModuleType("supabase")
    module.create_async_client = create
    monkeypatch.setitem(sys.modules, "supabase", module)
    return module


async def test_supabase_store_reads_and_increments(monkeypatch):
    fake = _FakeSupabaseClient(rows=[{"used": 4}], rpc_result=5)

    async def create(url, key):
        return fake

    _fake_supabase_module(monkeypatch, create)

    store_obj = SupabaseAllowanceStore("https://example.supabase.co", "key")
    assert await store_obj.get_used(USER) == 4
    assert await store_obj.increment(USER) == 5
    # Atomic increment goes through the Postgres function, not a read/write pair.
    assert fake.rpc_calls == [("increment_allowance", {"p_app_user_id": USER})]


async def test_supabase_store_returns_zero_for_unknown_user(monkeypatch):
    async def create(url, key):
        return _FakeSupabaseClient(rows=[])

    _fake_supabase_module(monkeypatch, create)
    store_obj = SupabaseAllowanceStore("https://example.supabase.co", "key")
    assert await store_obj.get_used("never-seen") == 0


async def test_supabase_store_unwraps_listed_rpc_result(monkeypatch):
    async def create(url, key):
        return _FakeSupabaseClient(rpc_result=[7])

    _fake_supabase_module(monkeypatch, create)
    store_obj = SupabaseAllowanceStore("https://example.supabase.co", "key")
    assert await store_obj.increment(USER) == 7


async def test_supabase_store_falls_back_to_sqlite_on_connect_failure(
        monkeypatch, tmp_path):
    async def create(url, key):
        raise RuntimeError("DNS is having a day")

    _fake_supabase_module(monkeypatch, create)
    store_obj = SupabaseAllowanceStore(
        "https://example.supabase.co", "key", str(tmp_path / "fallback.db"))

    # Degrades instead of raising, and the counter still works.
    assert await store_obj.increment(USER) == 1
    assert await store_obj.get_used(USER) == 1
    assert isinstance(store_obj._fallback, SqliteAllowanceStore)
    await store_obj.close()


async def test_supabase_store_falls_back_when_sdk_missing(monkeypatch, tmp_path):
    monkeypatch.setitem(sys.modules, "supabase", None)  # forces ImportError
    store_obj = SupabaseAllowanceStore(
        "https://example.supabase.co", "key", str(tmp_path / "fallback.db"))
    assert await store_obj.get_used(USER) == 0
    assert isinstance(store_obj._fallback, SqliteAllowanceStore)
    await store_obj.close()


# --- RevenueCat entitlements (httpx faked) -------------------------------

def _mock_revenuecat(monkeypatch, handler):
    monkeypatch.setattr(entitlements, "REVENUECAT_SECRET_KEY", "sk_test")
    monkeypatch.setattr(entitlements, "_client",
                        httpx.AsyncClient(transport=httpx.MockTransport(handler)))
    entitlements.clear_cache()


def _subscriber(entitlement):
    return {"subscriber": {"entitlements": {"pro": entitlement}}}


async def test_entitlement_active_when_expiry_is_in_the_future(monkeypatch):
    _mock_revenuecat(monkeypatch, lambda request: httpx.Response(
        200, json=_subscriber({"expires_date": "2099-01-01T00:00:00Z"})))
    assert await entitlements.is_pro(USER) is True


async def test_entitlement_active_when_expiry_is_null(monkeypatch):
    _mock_revenuecat(monkeypatch, lambda request: httpx.Response(
        200, json=_subscriber({"expires_date": None})))
    assert await entitlements.is_pro(USER) is True


async def test_entitlement_inactive_when_expired(monkeypatch):
    _mock_revenuecat(monkeypatch, lambda request: httpx.Response(
        200, json=_subscriber({"expires_date": "2020-01-01T00:00:00Z"})))
    assert await entitlements.is_pro(USER) is False


async def test_unknown_subscriber_404_is_not_pro(monkeypatch):
    _mock_revenuecat(monkeypatch,
                     lambda request: httpx.Response(404, json={}))
    assert await entitlements.is_pro(USER) is False


async def test_entitlement_result_is_cached(monkeypatch):
    calls = []

    def handler(request):
        calls.append(request.url.path)
        return httpx.Response(200, json=_subscriber({"expires_date": None}))

    _mock_revenuecat(monkeypatch, handler)
    assert await entitlements.is_pro(USER) is True
    assert await entitlements.is_pro(USER) is True
    assert len(calls) == 1


async def test_stale_cache_is_served_when_revenuecat_is_down(monkeypatch):
    state = {"down": False}

    def handler(request):
        if state["down"]:
            return httpx.Response(500, text="boom")
        return httpx.Response(200, json=_subscriber({"expires_date": None}))

    _mock_revenuecat(monkeypatch, handler)
    monkeypatch.setattr(entitlements, "CACHE_TTL_SECONDS", 0.0)  # force refetch

    assert await entitlements.is_pro(USER) is True
    state["down"] = True
    # Cache is stale but a paying subscriber must not lose access.
    assert await entitlements.is_pro(USER) is True


async def test_no_cache_and_revenuecat_down_is_not_pro(monkeypatch):
    def handler(request):
        raise httpx.ConnectError("network unreachable")

    _mock_revenuecat(monkeypatch, handler)
    # Fails closed: the user drops to the free allowance, not free unlimited.
    assert await entitlements.is_pro("brand-new-user") is False


async def test_no_secret_key_means_not_pro(monkeypatch):
    monkeypatch.setattr(entitlements, "REVENUECAT_SECRET_KEY", None)
    entitlements.clear_cache()
    assert await entitlements.is_pro(USER) is False


async def test_entitlement_id_is_configurable(monkeypatch):
    _mock_revenuecat(monkeypatch, lambda request: httpx.Response(
        200, json={"subscriber": {"entitlements": {
            "hook_pro": {"expires_date": None}}}}))
    assert await entitlements.is_pro(USER) is False

    monkeypatch.setattr(entitlements, "ENTITLEMENT_ID", "hook_pro")
    entitlements.clear_cache()
    assert await entitlements.is_pro(USER) is True


async def test_secret_key_is_sent_as_bearer_token(monkeypatch):
    seen = {}

    def handler(request):
        seen["auth"] = request.headers.get("Authorization")
        seen["path"] = request.url.path
        return httpx.Response(404, json={})

    _mock_revenuecat(monkeypatch, handler)
    await entitlements.is_pro(USER)
    assert seen["auth"] == "Bearer sk_test"
    assert seen["path"] == f"/v1/subscribers/{USER}"
