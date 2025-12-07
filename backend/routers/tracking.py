from fastapi import APIRouter, Request
from datetime import datetime
from database import user_visits_collection
from models import UserVisit
from bson import ObjectId

router = APIRouter()

@router.post("/track")
async def track_user(userIdentifier: str, request: Request):
    """Track user visit by identifier"""
    # Get IP address from request
    ip_address = request.client.host
    
    # Check if user already exists
    existing_visit = await user_visits_collection.find_one({"userIdentifier": userIdentifier})
    
    if existing_visit:
        # Update existing visit
        updated_visit = await user_visits_collection.find_one_and_update(
            {"userIdentifier": userIdentifier},
            {
                "$set": {
                    "lastSeen": datetime.now(),
                    "ipAddress": ip_address
                },
                "$inc": {"visitCount": 1}
            },
            return_document=True
        )
        # Convert ObjectId to string
        updated_visit["_id"] = str(updated_visit["_id"])
        return updated_visit
    else:
        # Create new visit
        new_visit = {
            "userIdentifier": userIdentifier,
            "ipAddress": ip_address,
            "firstSeen": datetime.now(),
            "lastSeen": datetime.now(),
            "visitCount": 1
        }
        result = await user_visits_collection.insert_one(new_visit)
        new_visit["_id"] = str(result.inserted_id)
        return new_visit
