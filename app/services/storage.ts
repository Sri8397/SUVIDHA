// ============================================
// Generic Storage Service - AsyncStorage Wrapper
// ============================================

import AsyncStorage from '@react-native-async-storage/async-storage';

/**
 * Get typed data from AsyncStorage
 */
export async function getData<T>(key: string): Promise<T | null> {
    try {
        const jsonValue = await AsyncStorage.getItem(key);
        // Try parsing JSON, but if it fails (e.g. simple string stored without quotes), return raw value
        try {
            return jsonValue != null ? JSON.parse(jsonValue) : null;
        } catch {
            return jsonValue as unknown as T;
        }
    } catch (error) {
        console.error(`Error reading ${key}:`, error);
        return null;
    }
}

/**
 * Save data to AsyncStorage
 */
export async function setData<T>(key: string, data: T): Promise<boolean> {
    try {
        const jsonValue = JSON.stringify(data);
        await AsyncStorage.setItem(key, jsonValue);
        return true;
    } catch (error) {
        console.error(`Error saving ${key}:`, error);
        return false;
    }
}

/**
 * Update data using a callback function
 */
export async function updateData<T>(
    key: string,
    updater: (current: T | null) => T
): Promise<boolean> {
    try {
        const current = await getData<T>(key);
        const updated = updater(current);
        return setData(key, updated);
    } catch (error) {
        console.error(`Error updating ${key}:`, error);
        return false;
    }
}

/**
 * Remove data from AsyncStorage
 */
export async function removeData(key: string): Promise<boolean> {
    try {
        await AsyncStorage.removeItem(key);
        return true;
    } catch (error) {
        console.error(`Error removing ${key}:`, error);
        return false;
    }
}

/**
 * Clear all app data (for reset/logout)
 */
export async function clearAllData(): Promise<boolean> {
    try {
        const keys = await AsyncStorage.getAllKeys();
        const appKeys = keys.filter(k => k.startsWith('@suvidha/'));
        await AsyncStorage.multiRemove(appKeys);
        return true;
    } catch (error) {
        console.error('Error clearing all data:', error);
        return false;
    }
}

/**
 * Generate unique ID
 */
export function generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substr(2, 9);
}

/**
 * Get current ISO timestamp
 */
export function getTimestamp(): string {
    return new Date().toISOString();
}
