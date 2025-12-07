from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional, Dict, Any, List

class UserVisit(BaseModel):
    id: Optional[str] = Field(None, alias="_id")
    userIdentifier: str
    ipAddress: str
    firstSeen: datetime
    lastSeen: datetime
    visitCount: int
    lastUserAgent: Optional[str] = None
    lastDeviceType: Optional[str] = None
    totalActivities: int = 0

    class Config:
        populate_by_name = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }

class ActivityLog(BaseModel):
    id: Optional[str] = Field(None, alias="_id")
    userId: str
    sessionId: str
    eventType: str  # 'page_view', 'click', 'scroll', 'form_interaction'
    eventName: str  # Specific event name
    eventData: Optional[Dict[str, Any]] = {}
    timestamp: datetime
    ipAddress: str
    userAgent: str
    deviceType: Optional[str] = None
    browser: Optional[str] = None
    os: Optional[str] = None
    referrer: Optional[str] = ""
    url: str
    screenResolution: Optional[str] = None

    class Config:
        populate_by_name = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }

class ActivityLogRequest(BaseModel):
    userId: str
    sessionId: str
    eventType: str
    eventName: str
    eventData: Optional[Dict[str, Any]] = {}
    url: str
    referrer: Optional[str] = ""
    screenResolution: Optional[str] = None

class AnalyticsOverview(BaseModel):
    totalUsers: int
    totalActivities: int
    topEvents: List[Dict[str, Any]]
    deviceBreakdown: Dict[str, int]
    browserBreakdown: Dict[str, int]

class UserActivityTimeline(BaseModel):
    userId: str
    totalActivities: int
    activities: List[Dict[str, Any]]
