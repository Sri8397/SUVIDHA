from fastapi import APIRouter, Query
from datetime import datetime
from database import activity_logs_collection, user_visits_collection
from models import AnalyticsOverview, UserActivityTimeline
from typing import Optional

router = APIRouter()

@router.get("/overview")
async def get_analytics_overview(
    startDate: Optional[str] = Query(None),
    endDate: Optional[str] = Query(None),
    userId: Optional[str] = Query(None)
):
    """Get analytics dashboard overview"""
    
    # Build query filter
    query_filter = {}
    if userId:
        query_filter["userId"] = userId
    if startDate or endDate:
        query_filter["timestamp"] = {}
        if startDate:
            query_filter["timestamp"]["$gte"] = datetime.fromisoformat(startDate)
        if endDate:
            query_filter["timestamp"]["$lte"] = datetime.fromisoformat(endDate)
    
    # Get total unique users
    total_users = await user_visits_collection.count_documents({})
    
    # Get total activities
    total_activities = await activity_logs_collection.count_documents(query_filter)
    
    # Get top events
    top_events_pipeline = [
        {"$match": query_filter} if query_filter else {"$match": {}},
        {
            "$group": {
                "_id": "$eventName",
                "count": {"$sum": 1},
                "uniqueUsers": {"$addToSet": "$userId"}
            }
        },
        {
            "$project": {
                "eventName": "$_id",
                "count": 1,
                "uniqueUsers": {"$size": "$uniqueUsers"},
                "_id": 0
            }
        },
        {"$sort": {"count": -1}},
        {"$limit": 10}
    ]
    top_events = await activity_logs_collection.aggregate(top_events_pipeline).to_list(10)
    
    # Get device breakdown
    device_pipeline = [
        {"$match": query_filter} if query_filter else {"$match": {}},
        {
            "$group": {
                "_id": "$deviceType",
                "count": {"$sum": 1}
            }
        }
    ]
    device_results = await activity_logs_collection.aggregate(device_pipeline).to_list(10)
    device_breakdown = {item["_id"] or "unknown": item["count"] for item in device_results}
    
    # Get browser breakdown
    browser_pipeline = [
        {"$match": query_filter} if query_filter else {"$match": {}},
        {
            "$group": {
                "_id": "$browser",
                "count": {"$sum": 1}
            }
        }
    ]
    browser_results = await activity_logs_collection.aggregate(browser_pipeline).to_list(20)
    browser_breakdown = {item["_id"] or "unknown": item["count"] for item in browser_results}
    
    return {
        "totalUsers": total_users,
        "totalActivities": total_activities,
        "topEvents": top_events,
        "deviceBreakdown": device_breakdown,
        "browserBreakdown": browser_breakdown
    }

@router.get("/user/{userId}")
async def get_user_activity_timeline(userId: str, limit: int = Query(50, le=200)):
    """Get specific user's activity timeline"""
    
    # Get user's activities
    activities = await activity_logs_collection.find(
        {"userId": userId}
    ).sort("timestamp", -1).limit(limit).to_list(limit)
    
    # Convert ObjectId to string and format datetime
    for activity in activities:
        activity["_id"] = str(activity["_id"])
        activity["timestamp"] = activity["timestamp"].isoformat()
    
    # Get total activity count
    total_activities = await activity_logs_collection.count_documents({"userId": userId})
    
    return {
        "userId": userId,
        "totalActivities": total_activities,
        "activities": activities
    }

@router.get("/events")
async def get_event_statistics():
    """Get all event types and their frequency"""
    
    pipeline = [
        {
            "$group": {
                "_id": "$eventName",
                "count": {"$sum": 1},
                "uniqueUsers": {"$addToSet": "$userId"}
            }
        },
        {
            "$project": {
                "eventName": "$_id",
                "count": 1,
                "uniqueUsers": {"$size": "$uniqueUsers"},
                "_id": 0
            }
        },
        {"$sort": {"count": -1}}
    ]
    
    events = await activity_logs_collection.aggregate(pipeline).to_list(100)
    
    return {"events": events}
