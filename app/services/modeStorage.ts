// ============================================
// Mode-Aware Storage Keys Utility
// ============================================
// 
// This module provides mode-prefixed storage keys so that
// 'shopkeeper' and 'personal' modes have completely separate data.

import AsyncStorage from '@react-native-async-storage/async-storage';
import { getStorageKeys } from '@/types';

export type AppMode = 'shopkeeper' | 'personal';

const MODE_STORAGE_KEY = '@suvidha_app_mode';

// Cache for current mode to avoid async lookups in hot paths
let cachedMode: AppMode | null = null;

/**
 * Get the current app mode from storage
 */
export async function getCurrentMode(): Promise<AppMode | null> {
    if (cachedMode) return cachedMode;
    try {
        const mode = await AsyncStorage.getItem(MODE_STORAGE_KEY);
        cachedMode = mode as AppMode | null;
        return cachedMode;
    } catch {
        return null;
    }
}

/**
 * Set the current app mode (also updates cache)
 */
export async function setCurrentMode(mode: AppMode): Promise<void> {
    cachedMode = mode;
    await AsyncStorage.setItem(MODE_STORAGE_KEY, mode);
}

/**
 * Clear mode cache (call when mode changes externally)
 */
export function clearModeCache(): void {
    cachedMode = null;
}

/**
 * Update the cached mode (call from ModeContext when mode changes)
 */
export function updateModeCache(mode: AppMode | null): void {
    cachedMode = mode;
}

/**
 * Get storage keys for the current mode
 * This is a synchronous function that uses the cached mode
 */
export function getKeysForCurrentMode(): ReturnType<typeof getStorageKeys> {
    return getStorageKeys(cachedMode);
}

/**
 * Get storage keys for a specific mode
 */
export function getKeysForMode(mode: AppMode | null): ReturnType<typeof getStorageKeys> {
    return getStorageKeys(mode);
}
