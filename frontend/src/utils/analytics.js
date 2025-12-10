/**
 * Analytics API Client
 * Handles communication with backend analytics endpoints
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Log an activity/event to the backend
 */
export const logActivity = async (activityData) => {
    try {
        const response = await fetch(`${API_BASE_URL}/api/activity`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(activityData)
        });

        if (!response.ok) {
            throw new Error('Failed to log activity');
        }

        return await response.json();
    } catch (error) {
        console.error('Error logging activity:', error);
        return null;
    }
};

/**
 * Track a page view event
 */
export const trackPageView = (userId, sessionId, pageName, url) => {
    return logActivity({
        userId,
        sessionId,
        eventType: 'page_view',
        eventName: `${pageName}_viewed`,
        eventData: {},
        url: url || window.location.pathname,
        referrer: document.referrer,
        screenResolution: `${window.screen.width}x${window.screen.height}`
    });
};

/**
 * Track a click event
 */
export const trackClick = (userId, sessionId, buttonName, eventData = {}) => {
    return logActivity({
        userId,
        sessionId,
        eventType: 'click',
        eventName: buttonName,
        eventData,
        url: window.location.pathname,
        referrer: document.referrer,
        screenResolution: `${window.screen.width}x${window.screen.height}`
    });
};

/**
 * Track a generic event
 */
export const trackEvent = (userId, sessionId, eventType, eventName, eventData = {}) => {
    return logActivity({
        userId,
        sessionId,
        eventType,
        eventName,
        eventData,
        url: window.location.pathname,
        referrer: document.referrer,
        screenResolution: `${window.screen.width}x${window.screen.height}`
    });
};

/**
 * Get analytics overview
 */
export const getAnalyticsOverview = async (params = {}) => {
    try {
        const queryParams = new URLSearchParams(params);
        const response = await fetch(`${API_BASE_URL}/api/analytics/overview?${queryParams}`);

        if (!response.ok) {
            throw new Error('Failed to fetch analytics');
        }

        return await response.json();
    } catch (error) {
        console.error('Error fetching analytics:', error);
        return null;
    }
};

/**
 * Get user activity timeline
 */
export const getUserTimeline = async (userId, limit = 50) => {
    try {
        const response = await fetch(`${API_BASE_URL}/api/analytics/user/${userId}?limit=${limit}`);

        if (!response.ok) {
            throw new Error('Failed to fetch user timeline');
        }

        return await response.json();
    } catch (error) {
        console.error('Error fetching user timeline:', error);
        return null;
    }
};
