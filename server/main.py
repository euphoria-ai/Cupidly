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

class ConversationContext(BaseModel):
    """Session-only, hidden understanding of the whole conversation.

    Held by the client between screenshots; the server stays stateless and only
    reads the incoming context and returns an updated one.
    """
    summary: str = ""          # rolling, concise understanding of the chat so far
    sent_replies: List[str] = []  # replies the user actually picked & sent (client-managed)

class GenerateRepliesRequest(BaseModel):
    screenshot_base64: str
    preferences: UserPreferences
    context: Optional[ConversationContext] = None

class GenerateRepliesResponse(BaseModel):
    suggestions: List[str]
    context: ConversationContext


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
                "updated_context_summary": {"type": "string"},
            },
            "required": [
                "suggestion_1", "suggestion_2", "suggestion_3",
                "updated_context_summary",
            ],
            "additionalProperties": False,
        },
    },
}


# --- Model Config ---
# qwen3.6-27b is currently the only vision-capable model Groq serves us; the
# llama-4 scout/maverick pair it replaced now returns model_not_found.
MODEL = os.getenv("GROQ_MODEL", "qwen/qwen3.6-27b")
FALLBACK_MODEL = os.getenv("GROQ_FALLBACK_MODEL", "qwen/qwen3.6-27b")

# It is a reasoning model: left to itself it emits a <think> block before the
# answer, which makes both JSON modes fail outright. "none" turns that off.
# Groq only accepts "none" or "default" here.
REASONING_EFFORT = os.getenv("GROQ_REASONING_EFFORT", "none")

# Compact prompt — fewer input tokens = faster processing.
# "JSON" must appear literally for Groq's json_object fallback mode.
PROMPT_TEMPLATE = """Look at this chat screenshot. Generate 3 flirty reply suggestions.
{context_block}
Preferences: style={style}, tone={tone}, flirt={flirt_level}, length={reply_length}, emoji={emoji_use}{profile_info}

Rules:
- SHORT=3-4 words, NORMAL=1 sentence, EXTENDED=1-2 sentences
- NEVER/MINIMAL/EXPRESSIVE controls emoji usage
- LESS/MEDIUM/BOLD controls flirt intensity
- updated_context_summary: rewrite the running conversation summary by folding in this newest screenshot without losing earlier facts (who the match is, tone, key topics/facts); keep it concise, a few sentences, max ~150 words

Reply with JSON only: {{"suggestion_1": "...", "suggestion_2": "...", "suggestion_3": "...", "updated_context_summary": "..."}}"""

def strip_reasoning(text: str) -> str:
    """Drop any <think> block and code fences a reasoning model may still emit."""
    if "</think>" in text:
        text = text.split("</think>", 1)[1]
    text = text.strip()
    if text.startswith("```"):
        text = text.split("\n", 1)[-1]
        if "```" in text:
            text = text.rsplit("```", 1)[0]
    # Fall back to the outermost JSON object if prose crept in around it.
    if not text.lstrip().startswith("{") and "{" in text and "}" in text:
        text = text[text.index("{"):text.rindex("}") + 1]
    return text.strip()


def build_context_block(context: Optional[ConversationContext]) -> str:
    """Render the prior context so the model can carry the conversation forward.

    Empty when there is nothing yet, so the first request is unchanged.
    """
    if context is None:
        return ""
    summary = (context.summary or "").strip()
    sent = [r for r in (context.sent_replies or []) if r and r.strip()]
    if not summary and not sent:
        return ""
    lines = ["\nConversation so far (context only — do not repeat verbatim):"]
    if summary:
        lines.append(f"Summary: {summary}")
    if sent:
        joined = "\n".join(f"- {r}" for r in sent)
        lines.append("Replies the user has already sent in this conversation:\n" + joined)
    return "\n".join(lines) + "\n"


def build_prompt(p: UserPreferences, context: Optional[ConversationContext] = None) -> str:
    parts = []
    if p.profile_name: parts.append(f", name={p.profile_name}")
    if p.profile_gender: parts.append(f", gender={p.profile_gender}")
    profile_info = "".join(parts)
    return PROMPT_TEMPLATE.format(
        context_block=build_context_block(context),
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
        prompt = build_prompt(request.preferences, request.context)

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
        updated_summary = ""
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
                kwargs = {}
                if REASONING_EFFORT:
                    kwargs["reasoning_effort"] = REASONING_EFFORT
                response = await _client.chat.completions.create(
                    model=model,
                    messages=messages,
                    response_format=response_format,
                    # Room for 3 replies plus the ~150-word rolling summary.
                    max_tokens=600,
                    temperature=0.7,
                    **kwargs,
                )
                groq_time = time.time() - groq_start

                text = response.choices[0].message.content
                candidate_summary = ""
                if text:
                    data = json.loads(strip_reasoning(text))
                    suggestions = [
                        data.get("suggestion_1", ""),
                        data.get("suggestion_2", ""),
                        data.get("suggestion_3", ""),
                    ]
                    # Present in the json_schema branch and, when the model
                    # cooperates, in the json_object fallback too.
                    raw_summary = data.get("updated_context_summary", "")
                    candidate_summary = strip_reasoning(raw_summary) if raw_summary else ""

                if suggestions and len(suggestions) >= 3 and all(suggestions):
                    updated_summary = candidate_summary
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

        # Client holds the context; we pass its sent_replies through unchanged
        # and hand back either the freshly updated summary or the prior one.
        prior_summary = request.context.summary if request.context else ""
        passthrough_sent = request.context.sent_replies if request.context else []
        result_context = ConversationContext(
            summary=updated_summary or prior_summary,
            sent_replies=passthrough_sent,
        )

        return GenerateRepliesResponse(suggestions=suggestions[:3], context=result_context)

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
