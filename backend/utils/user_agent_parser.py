"""User Agent Parser Utility

Parses user agent strings to extract browser, OS, and device type information.
"""

def parse_user_agent(user_agent: str) -> dict:
    """
    Parse user agent string to extract device, browser, and OS information.
    
    Args:
        user_agent: User agent string from HTTP request
        
    Returns:
        dict with keys: deviceType, browser, os
    """
    if not user_agent:
        return {
            "deviceType": "unknown",
            "browser": "unknown",
            "os": "unknown"
        }
    
    user_agent_lower = user_agent.lower()
    
    # Determine device type
    device_type = "desktop"
    if any(mobile in user_agent_lower for mobile in ['mobile', 'android', 'iphone', 'ipod', 'blackberry', 'windows phone']):
        device_type = "mobile"
    elif any(tablet in user_agent_lower for tablet in ['ipad', 'tablet', 'kindle']):
        device_type = "tablet"
    
    # Determine browser
    browser = "unknown"
    if 'edg' in user_agent_lower:
        browser = "Edge"
    elif 'chrome' in user_agent_lower and 'edg' not in user_agent_lower:
        browser = "Chrome"
    elif 'firefox' in user_agent_lower:
        browser = "Firefox"
    elif 'safari' in user_agent_lower and 'chrome' not in user_agent_lower:
        browser = "Safari"
    elif 'opera' in user_agent_lower or 'opr' in user_agent_lower:
        browser = "Opera"
    elif 'msie' in user_agent_lower or 'trident' in user_agent_lower:
        browser = "Internet Explorer"
    
    # Determine OS
    os_name = "unknown"
    if 'windows' in user_agent_lower:
        os_name = "Windows"
    elif 'mac os' in user_agent_lower or 'macos' in user_agent_lower:
        os_name = "MacOS"
    elif 'iphone' in user_agent_lower or 'ipad' in user_agent_lower:
        os_name = "iOS"
    elif 'android' in user_agent_lower:
        os_name = "Android"
    elif 'linux' in user_agent_lower:
        os_name = "Linux"
    elif 'cros' in user_agent_lower:
        os_name = "Chrome OS"
    
    return {
        "deviceType": device_type,
        "browser": browser,
        "os": os_name
    }
