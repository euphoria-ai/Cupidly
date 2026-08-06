import os
import base64
import json
from typing import List, Literal, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from google import genai
from google.genai import types
from dotenv import load_dotenv
import time

load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    raise ValueError("GEMINI_API_KEY environment variable is required")

# Initialize global client at startup — reuses connection pool across requests
_client = genai.Client(api_key=GEMINI_API_KEY)

app = FastAPI(
    title="L0V3",
    description="Backend service",
    version="1.2.0"
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


# --- Structured Output Schema for Gemini ---

class ReplySuggestions(BaseModel):
    """Structured output schema passed to Gemini via response_schema."""
    suggestion_1: str = Field(description="First reply suggestion")
    suggestion_2: str = Field(description="Second reply suggestion")
    suggestion_3: str = Field(description="Third reply suggestion")


# --- Model Config ---

MODEL = "gemini-3.5-flash-lite"
FALLBACK_MODEL = "gemini-2.0-flash-lite"

# Compact prompt — fewer input tokens = faster processing
PROMPT_TEMPLATE = """Look at this chat screenshot. Generate 3 flirty reply suggestions.

Preferences: style={style}, tone={tone}, flirt={flirt_level}, length={reply_length}, emoji={emoji_use}{profile_info}

Rules:
- SHORT=3-4 words, NORMAL=1 sentence, EXTENDED=1-2 sentences
- NEVER/MINIMAL/EXPRESSIVE controls emoji usage
- LESS/MEDIUM/BOLD controls flirt intensity"""

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
    return {"status": "online", "service": "L0V3", "version": "1.2.0"}

@app.post("/generate-replies", response_model=GenerateRepliesResponse)
async def generate_replies(request: GenerateRepliesRequest):
    start_time = time.time()

    try:
        # Pass raw bytes directly to Gemini — no PIL re-processing needed
        # Android client already sends 640px 70% JPEG
        image_data = base64.b64decode(request.screenshot_base64)
        prompt = build_prompt(request.preferences)

        contents = [
            types.Part.from_bytes(data=image_data, mime_type="image/jpeg"),
            types.Part.from_text(text=prompt)
        ]

        # Gemini structured output config — guarantees valid JSON matching ReplySuggestions schema
        config = types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=ReplySuggestions,
            max_output_tokens=200,
            temperature=0.7,
        )

        suggestions = []
        last_error = None

        for model in [MODEL, FALLBACK_MODEL]:
            try:
                gemini_start = time.time()
                response = await _client.aio.models.generate_content(
                    model=model,
                    contents=contents,
                    config=config,
                )
                gemini_time = time.time() - gemini_start

                # Structured output: response.parsed gives us the Pydantic model directly
                if response.parsed:
                    parsed: ReplySuggestions = response.parsed
                    suggestions = [parsed.suggestion_1, parsed.suggestion_2, parsed.suggestion_3]
                elif response.text:
                    # Fallback: manual JSON parse
                    data = json.loads(response.text)
                    suggestions = [
                        data.get("suggestion_1", ""),
                        data.get("suggestion_2", ""),
                        data.get("suggestion_3", ""),
                    ]

                if suggestions and len(suggestions) >= 3 and all(suggestions):
                    total = time.time() - start_time
                    print(f"[PERF] {model}: gemini={gemini_time:.2f}s total={total:.2f}s")
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
        loop="asyncio",
        http="httptools",
        log_level="warning"
    )
