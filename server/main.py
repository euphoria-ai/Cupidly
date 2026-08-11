import os
import json
from typing import List, Literal, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from openai import AsyncOpenAI
import httpx
from dotenv import load_dotenv
import time

load_dotenv()

GROQ_API_KEY = os.getenv("GROQ_API_KEY")
if not GROQ_API_KEY:
    raise ValueError("GROQ_API_KEY environment variable is required")

# Global async client at startup — reuses the connection pool across requests
_client = AsyncOpenAI(
    api_key=GROQ_API_KEY,
    base_url=os.getenv("GROQ_BASE_URL", "https://api.groq.com/openai/v1"),
    max_retries=0,  # we do our own model fallback; retries only add latency
    timeout=httpx.Timeout(connect=3.0, read=20.0, write=10.0, pool=3.0),
    http_client=httpx.AsyncClient(
        limits=httpx.Limits(max_connections=100, max_keepalive_connections=50,
                            keepalive_expiry=300.0),
        timeout=httpx.Timeout(connect=3.0, read=20.0, write=10.0, pool=3.0),
    ),
)

app = FastAPI(
    title="Cupidly",
    description="Backend service",
    version="1.3.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# --- Request/Response Models ---

class UserPreferences(BaseModel):
    style: Literal["LOWERCASE", "SENTENCE_CASE"]
    tone: Literal["GEN_Z_SLANG", "RESPECTFUL", "FUNNY", "SMOOTH"]
    flirt_level: Literal["LESS", "MEDIUM", "BOLD"]
    reply_length: Literal["SHORT", "NORMAL", "EXTENDED"]
    emoji_use: Literal["NEVER", "MINIMAL", "EXPRESSIVE"]
    profile_name: Optional[str] = ""
    profile_gender: Optional[str] = "Male"
    profile_pronouns: Optional[str] = "he/him"
    profile_bio: Optional[str] = ""

class GenerateRepliesRequest(BaseModel):
    screenshot_base64: str
    preferences: UserPreferences

class GenerateRepliesResponse(BaseModel):
    suggestions: List[str]


# --- Structured Output Schema for Groq ---

class ReplySuggestions(BaseModel):
    """Structured output schema passed to Groq via response_format json_schema."""
    suggestion_1: str = Field(description="First reply suggestion")
    suggestion_2: str = Field(description="Second reply suggestion")
    suggestion_3: str = Field(description="Third reply suggestion")


RESPONSE_FORMAT = {
    "type": "json_schema",
    "json_schema": {
        "name": "reply_suggestions",
        "strict": True,
        "schema": {
            "type": "object",
            "properties": {
                "suggestion_1": {"type": "string"},
                "suggestion_2": {"type": "string"},
                "suggestion_3": {"type": "string"},
            },
            "required": ["suggestion_1", "suggestion_2", "suggestion_3"],
            "additionalProperties": False,
        },
    },
}


# --- Model Config ---
# llama-4-scout on Groq LPUs: vision-capable, ~10x the tok/s of GPU inference.
MODEL = os.getenv("GROQ_MODEL", "meta-llama/llama-4-scout-17b-16e-instruct")
FALLBACK_MODEL = os.getenv("GROQ_FALLBACK_MODEL", "meta-llama/llama-4-maverick-17b-128e-instruct")

# Compact prompt — fewer input tokens = faster processing.
# "JSON" must appear literally for Groq's json_object fallback mode.
PROMPT_TEMPLATE = """Look at this chat screenshot. Generate 3 flirty reply suggestions.

Preferences: style={style}, tone={tone}, flirt={flirt_level}, length={reply_length}, emoji={emoji_use}{profile_info}

Rules:
- SHORT=3-4 words, NORMAL=1 sentence, EXTENDED=1-2 sentences
- NEVER/MINIMAL/EXPRESSIVE controls emoji usage
- LESS/MEDIUM/BOLD controls flirt intensity

Reply with JSON only: {{"suggestion_1": "...", "suggestion_2": "...", "suggestion_3": "..."}}"""

def build_prompt(p: UserPreferences) -> str:
    parts = []
    if p.profile_name: parts.append(f", name={p.profile_name}")
    if p.profile_gender: parts.append(f", gender={p.profile_gender}")
    profile_info = "".join(parts)
    return PROMPT_TEMPLATE.format(
        style=p.style, tone=p.tone, flirt_level=p.flirt_level,
        reply_length=p.reply_length, emoji_use=p.emoji_use, profile_info=profile_info
    )


# --- Routes ---

@app.get("/")
async def root():
    return {"status": "online", "service": "Cupidly", "version": "1.3.0"}

@app.post("/generate-replies", response_model=GenerateRepliesResponse)
async def generate_replies(request: GenerateRepliesRequest):
    start_time = time.time()

    try:
        # Pass the base64 payload straight through as a data URL — no re-encoding.
        # Android client already sends 640px 70% JPEG.
        b64 = request.screenshot_base64
        if b64.startswith("data:"):
            b64 = b64.split(",", 1)[-1]
        prompt = build_prompt(request.preferences)

        messages = [{
            "role": "user",
            "content": [
                {
                    "type": "image_url",
                    "image_url": {"url": f"data:image/jpeg;base64,{b64}"},
                },
                {"type": "text", "text": prompt},
            ],
        }]

        suggestions = []
        last_error = None

        # (model, response_format) attempts. json_schema is strict but only some
        # Groq models accept it; json_object is the universal fallback.
        attempts = [
            (MODEL, RESPONSE_FORMAT),
            (MODEL, {"type": "json_object"}),
            (FALLBACK_MODEL, {"type": "json_object"}),
        ]

        for model, response_format in attempts:
            try:
                groq_start = time.time()
                response = await _client.chat.completions.create(
                    model=model,
                    messages=messages,
                    response_format=response_format,
                    max_tokens=200,
                    temperature=0.7,
                )
                groq_time = time.time() - groq_start

                text = response.choices[0].message.content
                if text:
                    data = json.loads(text)
                    suggestions = [
                        data.get("suggestion_1", ""),
                        data.get("suggestion_2", ""),
                        data.get("suggestion_3", ""),
                    ]

                if suggestions and len(suggestions) >= 3 and all(suggestions):
                    total = time.time() - start_time
                    print(f"[PERF] {model}: groq={groq_time:.2f}s total={total:.2f}s")
                    break
                else:
                    suggestions = []
            except Exception as e:
                print(f"[ERR] {model}: {e}")
                last_error = e
                continue

        if not suggestions or len(suggestions) < 3:
            raise HTTPException(
                status_code=500,
                detail=f"AI generation failed: {str(last_error) if last_error else 'No valid suggestions'}"
            )

        return GenerateRepliesResponse(suggestions=suggestions[:3])

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Unexpected error: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=port,
        workers=1,
        loop="uvloop" if os.name != "nt" else "asyncio",
        http="httptools",
        log_level="warning"
    )
