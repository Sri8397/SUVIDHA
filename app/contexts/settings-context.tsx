import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { getData, setData } from '@/services/storage';
import { SHARED_STORAGE_KEYS, UserProfile, StoreSettings } from '@/types';
import { getKeysForCurrentMode } from '@/services/modeStorage';
import { useAppMode } from './ModeContext';

// Default values
const DEFAULT_USER_PROFILE: UserProfile = {
    name: '',
    phone: '',
    email: '',
    language: 'hi',
};

const DEFAULT_STORE_SETTINGS: StoreSettings = {
    name: '',
    address: '',
    phone: '',
    gstin: '',
    ownerName: '',
    currency: '₹',
    businessType: 'खुदरा व्यापार',
    profitCalculationMethod: 'accrual', // Default: count full sales
};

interface SettingsContextType {
    userProfile: UserProfile;
    storeSettings: StoreSettings;
    isLoading: boolean;
    updateUserProfile: (updates: Partial<UserProfile>) => Promise<boolean>;
    updateStoreSettings: (updates: Partial<StoreSettings>) => Promise<boolean>;
    refreshSettings: () => Promise<void>;
}

const SettingsContext = createContext<SettingsContextType | undefined>(undefined);

export function SettingsProvider({ children }: { children: ReactNode }) {
    const [userProfile, setUserProfile] = useState<UserProfile>(DEFAULT_USER_PROFILE);
    const [storeSettings, setStoreSettings] = useState<StoreSettings>(DEFAULT_STORE_SETTINGS);
    const [isLoading, setIsLoading] = useState(true);
    const { appMode } = useAppMode();

    // Load settings on mount
    useEffect(() => {
        loadSettings();
    }, []);

    // Reload store settings when mode changes (store settings are per-mode)
    useEffect(() => {
        if (appMode) {
            loadSettings();
        }
    }, [appMode]);

    const loadSettings = async () => {
        try {
            setIsLoading(true);
            const modeKeys = getKeysForCurrentMode();
            const [profile, settings] = await Promise.all([
                getData<UserProfile>(SHARED_STORAGE_KEYS.USER_PROFILE), // User profile is shared
                getData<StoreSettings>(modeKeys.STORE_SETTINGS), // Store settings are per-mode
            ]);

            if (profile) {
                setUserProfile({ ...DEFAULT_USER_PROFILE, ...profile });
            }
            if (settings) {
                setStoreSettings({ ...DEFAULT_STORE_SETTINGS, ...settings });
            } else {
                setStoreSettings(DEFAULT_STORE_SETTINGS); // Reset to defaults for new mode
            }
        } catch (error) {
            console.error('Error loading settings:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const refreshSettings = useCallback(async () => {
        await loadSettings();
    }, []);

    const updateUserProfile = useCallback(async (updates: Partial<UserProfile>): Promise<boolean> => {
        try {
            const newProfile = { ...userProfile, ...updates };
            const success = await setData(SHARED_STORAGE_KEYS.USER_PROFILE, newProfile);
            if (success) {
                setUserProfile(newProfile);
            }
            return success;
        } catch (error) {
            console.error('Error updating user profile:', error);
            return false;
        }
    }, [userProfile]);

    const updateStoreSettings = useCallback(async (updates: Partial<StoreSettings>): Promise<boolean> => {
        try {
            const modeKeys = getKeysForCurrentMode();
            const newSettings = { ...storeSettings, ...updates };
            const success = await setData(modeKeys.STORE_SETTINGS, newSettings);
            if (success) {
                setStoreSettings(newSettings);
            }
            return success;
        } catch (error) {
            console.error('Error updating store settings:', error);
            return false;
        }
    }, [storeSettings]);

    return (
        <SettingsContext.Provider
            value={{
                userProfile,
                storeSettings,
                isLoading,
                updateUserProfile,
                updateStoreSettings,
                refreshSettings,
            }}
        >
            {children}
        </SettingsContext.Provider>
    );
}

export function useSettings() {
    const context = useContext(SettingsContext);
    if (context === undefined) {
        throw new Error('useSettings must be used within a SettingsProvider');
    }
    return context;
}

