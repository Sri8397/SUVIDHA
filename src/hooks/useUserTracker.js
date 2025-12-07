import { useEffect, useRef } from 'react';

const generateUUID = () => {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
};

const useUserTracker = () => {
    const initialized = useRef(false);

    useEffect(() => {
        if (initialized.current) return;
        initialized.current = true;

        const trackUser = async () => {
            try {
                let userId = localStorage.getItem('suvidha_user_id');
                if (!userId) {
                    userId = generateUUID();
                    localStorage.setItem('suvidha_user_id', userId);
                }

                // Call backend API
                // Note: Ensure backend is running and CORS is configured
                await fetch(`http://localhost:8080/api/track?userIdentifier=${userId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                console.log('User tracked:', userId);
            } catch (error) {
                console.error('Failed to track user:', error);
            }
        };

        trackUser();
    }, []);
};

export default useUserTracker;
