import { useEffect, useRef, useCallback } from 'react';
import { trackClick, trackEvent, trackPageView } from '../utils/analytics';

const generateSessionId = () => {
    return 'session-' + Date.now() + '-' + Math.random().toString(36).substring(7);
};

/**
 * Hook for tracking user activities
 * Provides methods to track clicks, page views, and custom events
 */
const useActivityTracker = () => {
    const sessionId = useRef(null);
    const userId = useRef(null);

    useEffect(() => {
        // Initialize session ID
        if (!sessionId.current) {
            sessionId.current = sessionStorage.getItem('suvidha_session_id');
            if (!sessionId.current) {
                sessionId.current = generateSessionId();
                sessionStorage.setItem('suvidha_session_id', sessionId.current);
            }
        }

        // Get user ID from localStorage
        userId.current = localStorage.getItem('suvidha_user_id');
    }, []);

    /**
     * Track a click event
     * @param {string} eventName - Name of the event (e.g., 'hero_cta_click')
     * @param {object} eventData - Additional data about the event
     */
    const trackClickEvent = useCallback((eventName, eventData = {}) => {
        if (!userId.current || !sessionId.current) {
            console.warn('User or session ID not available for tracking');
            return;
        }

        trackClick(userId.current, sessionId.current, eventName, eventData);
        console.log(`Tracked click: ${eventName}`, eventData);
    }, []);

    /**
     * Track a page view
     * @param {string} pageName - Name of the page (e.g., 'home', 'pricing')
     */
    const trackPage = useCallback((pageName) => {
        if (!userId.current || !sessionId.current) {
            console.warn('User or session ID not available for tracking');
            return;
        }

        trackPageView(userId.current, sessionId.current, pageName, window.location.pathname);
        console.log(`Tracked page view: ${pageName}`);
    }, []);

    /**
     * Track a custom event
     * @param {string} eventType - Type of event (e.g., 'scroll', 'form_interaction')
     * @param {string} eventName - Name of the event
     * @param {object} eventData - Additional event data
     */
    const trackCustomEvent = useCallback((eventType, eventName, eventData = {}) => {
        if (!userId.current || !sessionId.current) {
            console.warn('User or session ID not available for tracking');
            return;
        }

        trackEvent(userId.current, sessionId.current, eventType, eventName, eventData);
        console.log(`Tracked ${eventType}: ${eventName}`, eventData);
    }, []);

    return {
        trackClick: trackClickEvent,
        trackPageView: trackPage,
        trackEvent: trackCustomEvent,
        getUserId: () => userId.current,
        getSessionId: () => sessionId.current
    };
};

export default useActivityTracker;
