from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import tracking
from dotenv import load_dotenv
import os

load_dotenv()

app = FastAPI(title=os.getenv("APP_NAME", "backend"))

# CORS configuration - allow all origins for development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(tracking.router, prefix="/api")

@app.get("/")
async def root():
    return {"message": "Suvidha Backend API", "status": "running"}
