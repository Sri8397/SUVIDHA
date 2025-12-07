from fastapi import APIRouter, Request
from datetime import datetime
from database import user_visits_collection, activity_logs_collection
from models import UserVisit, ActivityLogRequest
from bson import ObjectId
from utils.user_agent_parser import parse_user_agent

router = APIRouter()

@router.post("/track")
async def track_user(userIdentifier: str, request: Request):
    """Track user visit by identifier"""
    # Get IP address and user agent from request
    ip_address = request.client.host
    user_agent = request.headers.get("user-agent", "")
    
    # Parse user agent
    ua_info = parse_user_agent(user_agent)
    
    # Check if user already exists
    existing_visit = await user_visits_collection.find_one({"userIdentifier": userIdentifier})
    
    if existing_visit:
        # Update existing visit
        updated_visit = await user_visits_collection.find_one_and_update(
            {"userIdentifier": userIdentifier},
            {
                "$set": {
                    "lastSeen": datetime.now(),
                    "ipAddress": ip_address,
                    "lastUserAgent": user_agent,
                    "lastDeviceType": ua_info["deviceType"]
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
            "visitCount": 1,
            "lastUserAgent": user_agent,
            "lastDeviceType": ua_info["deviceType"],
            "totalActivities": 0
        }
        result = await user_visits_collection.insert_one(new_visit)
        new_visit["_id"] = str(result.inserted_id)
        return new_visit

@router.post("/activity")
async def log_activity(activity: ActivityLogRequest, request: Request):
    """Log user activity/event"""
    # Get IP address and user agent from request
    ip_address = request.client.host
    user_agent = request.headers.get("user-agent", "")
    
    # Parse user agent
    ua_info = parse_user_agent(user_agent)
    
    # Create activity log
    activity_log = {
        "userId": activity.userId,
        "sessionId": activity.sessionId,
        "eventType": activity.eventType,
        "eventName": activity.eventName,
        "eventData": activity.eventData or {},
        "timestamp": datetime.now(),
        "ipAddress": ip_address,
        "userAgent": user_agent,
        "deviceType": ua_info["deviceType"],
        "browser": ua_info["browser"],
        "os": ua_info["os"],
        "referrer": activity.referrer or "",
        "url": activity.url,
        "screenResolution": activity.screenResolution
    }
    
    # Insert activity log
    result = await activity_logs_collection.insert_one(activity_log)
    
    # Update user's total activities count
    await user_visits_collection.update_one(
        {"userIdentifier": activity.userId},
        {"$inc": {"totalActivities": 1}}
    )
    
    return {
        "success": True,
        "activityId": str(result.inserted_id)
    }
