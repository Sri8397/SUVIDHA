import { useEffect, useRef } from 'react';

const generateUUID = () => {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
};

const generateSessionId = () => {
    return 'session-' + Date.now() + '-' + Math.random().toString(36).substring(7);
};

const useUserTracker = () => {
    const initialized = useRef(false);

    useEffect(() => {
        if (initialized.current) return;
        initialized.current = true;

        const trackUser = async () => {
            try {
                // Get or create user ID
                let userId = localStorage.getItem('suvidha_user_id');
                if (!userId) {
                    userId = generateUUID();
                    localStorage.setItem('suvidha_user_id', userId);
                }

                // Get or create session ID
                let sessionId = sessionStorage.getItem('suvidha_session_id');
                if (!sessionId) {
                    sessionId = generateSessionId();
                    sessionStorage.setItem('suvidha_session_id', sessionId);
                }

                // Track initial visit
                await fetch(`http://localhost:8080/api/track?userIdentifier=${userId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                console.log('User tracked:', userId);

                // Log initial page view activity
                await fetch('http://localhost:8080/api/activity', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        userId: userId,
                        sessionId: sessionId,
                        eventType: 'page_view',
                        eventName: 'initial_page_load',
                        eventData: {},
                        url: window.location.pathname,
                        referrer: document.referrer,
                        screenResolution: `${window.screen.width}x${window.screen.height}`
                    })
                });
                console.log('Initial page view tracked');
            } catch (error) {
                console.error('Failed to track user:', error);
            }
        };

        trackUser();
    }, []);
};

export default useUserTracker;
