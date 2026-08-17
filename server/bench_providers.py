"""One-shot Groq vs Gemini latency check for Hook's generate-replies job.

Uses the same vision + JSON contract as production. Does not start the API.
Reads GROQ_API_KEY from the environment or server/.env, and GEMINI_API_KEY
from the process environment (Windows user env is fine).

Usage (from server/):
    uv run python bench_providers.py
"""

from __future__ import annotations

import base64
import io
import json
import os
import sys
import time
from pathlib import Path

from dotenv import load_dotenv
from openai import OpenAI
from PIL import Image, ImageDraw, ImageFont

load_dotenv(Path(__file__).resolve().parent / ".env")

GROQ_MODEL = os.getenv("GROQ_MODEL", "qwen/qwen3.6-27b")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-3.5-flash-lite")

PROMPT = """You are an expert flirting agent. Your task is to analyse the attached screenshot and generate 3 flirty chat reply suggestions keeping the parameters described below.

Preferences: style=LOWERCASE, tone=FUNNY, flirt=MEDIUM, length=SHORT, emoji=MINIMAL

Rules:
- SHORT=3-4 words, NORMAL=1 sentence, EXTENDED=1-2 sentences
- NEVER/MINIMAL/EXPRESSIVE controls emoji usage
- LESS/MEDIUM/BOLD controls flirt intensity
- updated_context_summary: rewrite the running conversation summary by folding in this newest screenshot without losing earlier facts (who the match is, tone, key topics/facts); keep it concise, a few sentences, max ~150 words

Reply with JSON only: {"suggestion_1": "...", "suggestion_2": "...", "suggestion_3": "...", "updated_context_summary": "..."}"""

# Gemini rejects JSON Schema's additionalProperties field.
JSON_SCHEMA = {
    "type": "object",
    "properties": {
        "suggestion_1": {"type": "string"},
        "suggestion_2": {"type": "string"},
        "suggestion_3": {"type": "string"},
        "updated_context_summary": {"type": "string"},
    },
    "required": [
        "suggestion_1",
        "suggestion_2",
        "suggestion_3",
        "updated_context_summary",
    ],
}


def _fake_chat_jpeg() -> bytes:
    """512px-wide fake dating chat, similar size to the Android client upload."""
    width, height = 512, 900
    img = Image.new("RGB", (width, height), (18, 18, 20))
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 22)
        small = ImageFont.truetype("arial.ttf", 16)
    except OSError:
        font = ImageFont.load_default()
        small = font

    draw.rectangle((0, 0, width, 64), fill=(28, 28, 32))
    draw.text((24, 20), "Alex", fill=(240, 240, 245), font=font)

    bubbles = [
        (False, "hey you free friday?"),
        (True, "depends, what did you have in mind"),
        (False, "drinks, nothing crazy"),
        (True, "i could be convinced"),
        (False, "so that's a yes then"),
    ]
    y = 96
    for mine, text in bubbles:
        tw = int(draw.textlength(text, font=small))
        pad_x, pad_y = 16, 12
        bw, bh = tw + pad_x * 2, 28 + pad_y
        if mine:
            x0 = width - 24 - bw
            fill = (40, 90, 220)
        else:
            x0 = 24
            fill = (42, 42, 48)
        draw.rounded_rectangle((x0, y, x0 + bw, y + bh), radius=14, fill=fill)
        draw.text((x0 + pad_x, y + 8), text, fill=(245, 245, 248), font=small)
        y += bh + 18

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=60)
    return buf.getvalue()


def _parse(text: str) -> dict | None:
    if not text:
        return None
    raw = text.strip()
    if raw.startswith("```"):
        raw = raw.split("\n", 1)[-1]
        if "```" in raw:
            raw = raw.rsplit("```", 1)[0]
    if not raw.lstrip().startswith("{") and "{" in raw and "}" in raw:
        raw = raw[raw.index("{") : raw.rindex("}") + 1]
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return None
    if not all(data.get(k) for k in ("suggestion_1", "suggestion_2", "suggestion_3")):
        return None
    return data


def _print_result(name: str, result: dict) -> None:
    print(f"\n=== {name} ===")
    if not result.get("ok"):
        print(f"error: {result.get('error')}")
        return
    print(f"model:     {result['model']}")
    print(f"wall:      {result['seconds']:.2f}s")
    print(f"tokens:    prompt={result.get('prompt_tokens')} "
          f"completion={result.get('completion_tokens')}")
    print(f"json_ok:   {result['json_ok']}")
    if result.get("suggestion_1"):
        print(f"sample:    {result['suggestion_1']!r}")


def run_groq(jpeg: bytes) -> dict:
    key = os.getenv("GROQ_API_KEY")
    if not key:
        return {"ok": False, "error": "GROQ_API_KEY is not set"}
    try:
        client = OpenAI(
            api_key=key,
            base_url=os.getenv("GROQ_BASE_URL", "https://api.groq.com/openai/v1"),
            max_retries=0,
            timeout=30.0,
        )
        b64 = base64.b64encode(jpeg).decode("ascii")
        t0 = time.perf_counter()
        response = client.chat.completions.create(
            model=GROQ_MODEL,
            messages=[{
                "role": "user",
                "content": [
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:image/jpeg;base64,{b64}"},
                    },
                    {"type": "text", "text": PROMPT},
                ],
            }],
            response_format={"type": "json_object"},
            max_tokens=600,
            temperature=0.7,
            extra_body={"reasoning_effort": "none"},
        )
        elapsed = time.perf_counter() - t0
        text = response.choices[0].message.content or ""
        parsed = _parse(text)
        usage = response.usage
        return {
            "ok": True,
            "model": GROQ_MODEL,
            "seconds": elapsed,
            "json_ok": parsed is not None,
            "suggestion_1": (parsed or {}).get("suggestion_1"),
            "prompt_tokens": getattr(usage, "prompt_tokens", None),
            "completion_tokens": getattr(usage, "completion_tokens", None),
        }
    except Exception as e:
        return {"ok": False, "error": str(e)}


def run_gemini(jpeg: bytes, model: str | None = None) -> dict:
    key = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY")
    if not key:
        return {"ok": False, "error": "GEMINI_API_KEY is not set"}

    from google import genai
    from google.genai import types

    # Pass the key explicitly so the SDK does not silently prefer GOOGLE_API_KEY.
    client = genai.Client(api_key=key)
    model_used = model or GEMINI_MODEL
    config_kwargs = {
        "response_mime_type": "application/json",
        "response_schema": JSON_SCHEMA,
        "max_output_tokens": 600,
        "temperature": 0.7,
        "automatic_function_calling": types.AutomaticFunctionCallingConfig(
            disable=True
        ),
    }
    thinking = None
    try:
        thinking = types.ThinkingConfig(thinking_level="minimal")
    except (TypeError, ValueError, AttributeError):
        try:
            thinking = types.ThinkingConfig(thinking_budget=0)
        except (TypeError, ValueError, AttributeError):
            thinking = None
    if thinking is not None:
        config_kwargs["thinking_config"] = thinking

    t0 = time.perf_counter()
    try:
        response = client.models.generate_content(
            model=model_used,
            contents=[
                types.Part.from_bytes(data=jpeg, mime_type="image/jpeg"),
                PROMPT,
            ],
            config=types.GenerateContentConfig(**config_kwargs),
        )
    except Exception as e:
        return {"ok": False, "error": str(e), "model": model_used}
    elapsed = time.perf_counter() - t0
    text = response.text or ""
    parsed = _parse(text)
    usage = getattr(response, "usage_metadata", None)
    return {
        "ok": True,
        "model": model_used,
        "seconds": elapsed,
        "json_ok": parsed is not None,
        "suggestion_1": (parsed or {}).get("suggestion_1"),
        "prompt_tokens": getattr(usage, "prompt_token_count", None),
        "completion_tokens": getattr(usage, "candidates_token_count", None),
    }


def main() -> int:
    jpeg = _fake_chat_jpeg()
    print(f"image: {len(jpeg)} bytes JPEG (512px fake chat)")

    if "--flash-lites" in sys.argv:
        models = [
            "gemini-2.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash-lite",
        ]
        print("comparing Flash-Lite models once each (full JSON, no streaming)")
        any_ok = False
        for model in models:
            result = run_gemini(jpeg, model=model)
            _print_result(model, result)
            any_ok = any_ok or bool(result.get("ok"))
        return 0 if any_ok else 1

    print("running Groq then Gemini once each (full JSON, no streaming)")

    groq = run_groq(jpeg)
    _print_result("Groq", groq)

    gemini = run_gemini(jpeg)
    _print_result("Gemini", gemini)

    if groq.get("ok") and gemini.get("ok"):
        g, m = groq["seconds"], gemini["seconds"]
        faster = "Groq" if g <= m else "Gemini"
        ratio = max(g, m) / max(min(g, m), 0.001)
        print(f"\n{faster} was faster on this run ({ratio:.1f}x). "
              f"Hook waits for the full JSON, so this wall time is the number that matters.")
    return 0 if (groq.get("ok") or gemini.get("ok")) else 1


if __name__ == "__main__":
    raise SystemExit(main())
