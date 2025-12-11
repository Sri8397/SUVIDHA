import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { getData, setData } from '@/services/storage';
import { STORAGE_KEYS, UserProfile, StoreSettings } from '@/types';

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

    // Load settings on mount
    useEffect(() => {
        loadSettings();
    }, []);

    const loadSettings = async () => {
        try {
            setIsLoading(true);
            const [profile, settings] = await Promise.all([
                getData<UserProfile>(STORAGE_KEYS.USER_PROFILE),
                getData<StoreSettings>(STORAGE_KEYS.STORE_SETTINGS),
            ]);

            if (profile) {
                setUserProfile({ ...DEFAULT_USER_PROFILE, ...profile });
            }
            if (settings) {
                setStoreSettings({ ...DEFAULT_STORE_SETTINGS, ...settings });
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
            const success = await setData(STORAGE_KEYS.USER_PROFILE, newProfile);
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
            const newSettings = { ...storeSettings, ...updates };
            const success = await setData(STORAGE_KEYS.STORE_SETTINGS, newSettings);
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
