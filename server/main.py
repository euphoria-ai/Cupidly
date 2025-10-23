import os
import base64
import io
from typing import List, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from PIL import Image
from google import genai
from google.genai import types
from dotenv import load_dotenv

load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    raise ValueError("GEMINI_API_KEY environment variable is required")

app = FastAPI(
    title="L0V3",
    description="Backend service",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], 
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class UserPreferences(BaseModel):
    style: str
    tone: str
    flirt_level: str
    reply_length: str
    emoji_use: str
    profile_name: Optional[str] = ""
    profile_gender: Optional[str] = ""
    profile_pronouns: Optional[str] = ""
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

MODEL_NAME = "gemini-2.5-flash"

def build_prompt(preferences: UserPreferences) -> str:
    prompt_parts = [
        "You are an AI texting assistant helping to generate replies for chat conversations.",
        "",
        "Analyze the chat screenshot and generate EXACTLY 3 different reply options.",
        "",
        "USER PREFERENCES:",
        f"- Style: {preferences.style}",
        f"- Tone: {preferences.tone}",
        f"- Flirt Level: {preferences.flirt_level}",
        f"- Reply Length: {preferences.reply_length}",
        f"- Emoji Use: {preferences.emoji_use}",
    ]
    
    if preferences.profile_name:
        prompt_parts.append(f"- Name: {preferences.profile_name}")
    if preferences.profile_gender:
        prompt_parts.append(f"- Gender: {preferences.profile_gender}")
    if preferences.profile_pronouns:
        prompt_parts.append(f"- Pronouns: {preferences.profile_pronouns}")
    if preferences.profile_bio:
        prompt_parts.append(f"- About: {preferences.profile_bio}")
    
    prompt_parts.extend([
        "",
        "IMPORTANT INSTRUCTIONS:",
        "1. Generate EXACTLY 3 distinct reply options",
        "2. Make each reply unique in approach",
        "3. Match the conversation context and tone",
        "4. Keep replies natural and conversational",
        "5. Apply the user's style preferences to each reply",
        "6. In messaging apps: the messages on right side are the user's and the messages on left side are the incoming",
        "7. Always answer from the user's perspective",
        "",
        "Generate three distinct replies that would work well in this conversation."
    ])
    
    return "\n".join(prompt_parts)

@app.get("/")
async def root():
    return {
        "status": "online",
        "service": "L0V3",
        "version": "1.0.0"
    }

@app.post("/generate-replies", response_model=GenerateRepliesResponse)
async def generate_replies(request: GenerateRepliesRequest):
    try:
        try:
            image_data = base64.b64decode(request.screenshot_base64)
            image = Image.open(io.BytesIO(image_data))
        except Exception as e:
            raise HTTPException(
                status_code=400,
                detail=f"Failed to decode screenshot: {str(e)}"
            )
        
        prompt = build_prompt(request.preferences)
        
        try:
            img_byte_arr = io.BytesIO()
            image.save(img_byte_arr, format='JPEG')
            img_byte_arr = img_byte_arr.getvalue()
            
            contents = [
                types.Part.from_bytes(data=img_byte_arr, mime_type="image/jpeg"),
                types.Part.from_text(text=prompt)
            ]
            
            async with genai.Client(api_key=GEMINI_API_KEY).aio as aclient:
                response = await aclient.models.generate_content(
                    model=MODEL_NAME,
                    contents=contents,
                    config={
                        "response_mime_type": "application/json",
                        "response_schema": ReplySuggestions,
                    }
                )
            
            if not response.parsed:
                raise HTTPException(
                    status_code=500,
                    detail="Empty response from AI model"
                )
            
            reply_suggestions: ReplySuggestions = response.parsed
            suggestions = [
                reply_suggestions.option_1.reply,
                reply_suggestions.option_2.reply,
                reply_suggestions.option_3.reply
            ]
            
            return GenerateRepliesResponse(suggestions=suggestions)
            
        except Exception as e:
            raise HTTPException(
                status_code=500,
                detail=f"AI generation failed: {str(e)}"
            )
            
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
    uvicorn.run(app, host="0.0.0.0", port=port)

