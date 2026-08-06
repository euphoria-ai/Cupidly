import os
import base64
import io
from typing import List, Literal, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from PIL import Image
from google import genai
from google.genai import types
from dotenv import load_dotenv
import time

load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    raise ValueError("GEMINI_API_KEY environment variable is required")

# Initialize global client
_client = genai.Client(api_key=GEMINI_API_KEY)

def get_client():
    return _client

app = FastAPI(
    title="L0V3",
    description="Backend service",
    version="1.1.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# PreferencesDto(style=LOWERCASE, tone=GEN_Z_SLANG, flirtLevel=MEDIUM, replyLength=SHORT, emojiUse=EXPRESSIVE, profileName=, profileGender=Male, profilePronouns=he/him, profileBio=)
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

class ReplyOption(BaseModel):
    """A single reply suggestion."""
    reply: str = Field(description="The suggested reply text")

class ReplySuggestions(BaseModel):
    option_1: ReplyOption = Field(description="First reply option")
    option_2: ReplyOption = Field(description="Second reply option")
    option_3: ReplyOption = Field(description="Third reply option")

MODEL_NAMES = ["gemini-3.5-flash-lite", "gemini-1.5-flash"]

# Pre-built prompt template for speed
PROMPT_TEMPLATE = """You are an flirting assistant helping to generate flirty replies for chat conversations.

Analyze the chat screenshot and generate EXACTLY 3 different flirty reply options.
Make sure to follow the user's preferences strictly.

USER PREFERENCES:
- Style: {style} preferred
- Tone: {tone} preferred
- Flirt Level: {flirt_level} preferred
- Reply Length: {reply_length} preferred
- Emoji Use: {emoji_use} preferred{profile_info}

IMPORTANT INSTRUCTIONS:
1. FOLLOW THE USER'S PREFERENCES STRICTLY!
2. Flirt level: LESS means less flirty, MEDIUM means moderate flirty, BOLD means too much flirty
3. Reply length: SHORT means very short (3-4 words), NORMAL means average message length, EXTENDED means 1-2 sentences
4. Emoji use: NEVER means never use emojis, MINIMAL means some replies can have one emoji, EXPRESSIVE means most replies can have emojis
5. Return JSON format strictly like: {"suggestions": ["reply 1", "reply 2", "reply 3"]}
6. Make each reply unique
7. Match the conversation context and tone
8. Keep replies natural and short

Follow the user's preferences strictly and generate three distinct replies now:"""

def build_prompt(preferences: UserPreferences) -> str:
    # Build profile info string efficiently
    profile_parts = []
    if preferences.profile_name:
        profile_parts.append(f"- Name: {preferences.profile_name}")
    if preferences.profile_gender:
        profile_parts.append(f"- Gender: {preferences.profile_gender}")
    if preferences.profile_pronouns:
        profile_parts.append(f"- Pronouns: {preferences.profile_pronouns}")
    if preferences.profile_bio:
        profile_parts.append(f"- About: {preferences.profile_bio}")

    profile_info = "\n" + "\n".join(profile_parts) if profile_parts else ""

    return PROMPT_TEMPLATE.format(
        style=preferences.style,
        tone=preferences.tone,
        flirt_level=preferences.flirt_level,
        reply_length=preferences.reply_length,
        emoji_use=preferences.emoji_use,
        profile_info=profile_info
    )

@app.get("/")
async def root():
    return {
        "status": "online",
        "service": "L0V3",
        "version": "1.1.0"
    }

@app.post("/generate-replies", response_model=GenerateRepliesResponse)
async def generate_replies(request: GenerateRepliesRequest):
    start_time = time.time()

    try:
        decode_start = time.time()
        try:
            image_data = base64.b64decode(request.screenshot_base64)
            image = Image.open(io.BytesIO(image_data))

            # Only resize if Android client sent a larger image (safety net)
            if image.width > 640:
                ratio = 640 / image.width
                new_height = int(image.height * ratio)
                image = image.resize((640, new_height), Image.Resampling.BILINEAR)

        except Exception as e:
            raise HTTPException(
                status_code=400,
                detail=f"Failed to decode screenshot: {str(e)}"
            )

        prompt = build_prompt(request.preferences)

        img_byte_arr = io.BytesIO()
        image.save(img_byte_arr, format='JPEG', quality=70)
        img_byte_arr = img_byte_arr.getvalue()
        decode_time = time.time() - decode_start

        contents = [
            types.Part.from_bytes(data=img_byte_arr, mime_type="image/jpeg"),
            types.Part.from_text(text=prompt)
        ]

        client = get_client()

        suggestions = []
        last_error = None

        for model in MODEL_NAMES:
            try:
                gemini_start = time.time()
                response = await client.aio.models.generate_content(
                    model=model,
                    contents=contents,
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json",
                        response_schema=ReplySuggestions,
                        thinking_config=types.ThinkingConfig(thinking_budget=0),
                        max_output_tokens=150,
                        temperature=0.7,
                    )
                )
                gemini_time = time.time() - gemini_start

                if response.parsed:
                    reply_suggestions: ReplySuggestions = response.parsed
                    suggestions = [
                        reply_suggestions.option_1.reply,
                        reply_suggestions.option_2.reply,
                        reply_suggestions.option_3.reply
                    ]
                elif response.text:
                    import json
                    try:
                        data = json.loads(response.text)
                        if isinstance(data, dict):
                            if "suggestions" in data and isinstance(data["suggestions"], list):
                                suggestions = data["suggestions"]
                            elif "option_1" in data:
                                o1 = data["option_1"].get("reply", "") if isinstance(data["option_1"], dict) else str(data["option_1"])
                                o2 = data["option_2"].get("reply", "") if isinstance(data["option_2"], dict) else str(data["option_2"])
                                o3 = data["option_3"].get("reply", "") if isinstance(data["option_3"], dict) else str(data["option_3"])
                                suggestions = [o1, o2, o3]
                        elif isinstance(data, list):
                            suggestions = [str(x) for x in data]
                    except Exception:
                        pass

                if suggestions and len(suggestions) >= 3:
                    print(f"[PERF] Model {model}: gemini={gemini_time:.3f}s | decode={decode_time:.3f}s")
                    break
            except Exception as e:
                print(f"Model {model} failed: {e}")
                last_error = e
                continue

        if not suggestions or len(suggestions) < 3:
            raise HTTPException(
                status_code=500,
                detail=f"AI generation failed: {str(last_error) if last_error else 'No valid suggestions generated'}"
            )

        # Log performance for monitoring
        processing_time = time.time() - start_time
        print(f"[PERF] Total request: {processing_time:.3f}s")

        return GenerateRepliesResponse(suggestions=suggestions)

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Unexpected error: {str(e)}"
        )

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    # Optimized uvicorn settings for speed
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=port,
        workers=1,  # Single worker for this use case
        loop="asyncio",
        http="httptools",  # Faster HTTP parser
        log_level="warning"  # Reduce logging overhead
    )
