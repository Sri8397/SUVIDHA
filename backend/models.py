from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional

class UserVisit(BaseModel):
    id: Optional[str] = Field(None, alias="_id")
    userIdentifier: str
    ipAddress: str
    firstSeen: datetime
    lastSeen: datetime
    visitCount: int

    class Config:
        populate_by_name = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }
