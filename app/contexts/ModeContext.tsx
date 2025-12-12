import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { updateModeCache } from '@/services/modeStorage';

export type AppMode = 'shopkeeper' | 'personal';

interface ModeContextType {
    appMode: AppMode | null;
    isLoading: boolean;
    setAppMode: (mode: AppMode) => Promise<void>;
    clearMode: () => Promise<void>;
}

const ModeContext = createContext<ModeContextType | undefined>(undefined);

const MODE_STORAGE_KEY = '@suvidha_app_mode';

export function ModeProvider({ children }: { children: ReactNode }) {
    const [appMode, setAppModeState] = useState<AppMode | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // Load mode on mount
    useEffect(() => {
        loadMode();
    }, []);

    const loadMode = async () => {
        try {
            const stored = await AsyncStorage.getItem(MODE_STORAGE_KEY);
            if (stored) {
                const mode = stored as AppMode;
                setAppModeState(mode);
                updateModeCache(mode); // Sync with modeStorage cache
            }
        } catch (error) {
            console.error('Failed to load app mode:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const setAppMode = useCallback(async (mode: AppMode) => {
        try {
            await AsyncStorage.setItem(MODE_STORAGE_KEY, mode);
            setAppModeState(mode);
            updateModeCache(mode); // Sync with modeStorage cache
        } catch (error) {
            console.error('Failed to save app mode:', error);
        }
    }, []);

    const clearMode = useCallback(async () => {
        try {
            await AsyncStorage.removeItem(MODE_STORAGE_KEY);
            setAppModeState(null);
            updateModeCache(null); // Sync with modeStorage cache
        } catch (error) {
            console.error('Failed to clear app mode:', error);
        }
    }, []);

    return (
        <ModeContext.Provider value={{ appMode, isLoading, setAppMode, clearMode }}>
            {children}
        </ModeContext.Provider>
    );
}

export function useAppMode() {
    const context = useContext(ModeContext);
    if (context === undefined) {
        throw new Error('useAppMode must be used within a ModeProvider');
    }
    return context;
}
