import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import AsyncStorage from '@react-native-async-storage/async-storage';

// Import all locale files
import en from '../locales/en.json';
import hi from '../locales/hi.json';
import mr from '../locales/mr.json';
import gu from '../locales/gu.json';
import bn from '../locales/bn.json';
import ta from '../locales/ta.json';
import te from '../locales/te.json';
import kn from '../locales/kn.json';
import ml from '../locales/ml.json';
import pa from '../locales/pa.json';
import or from '../locales/or.json';

const LANGUAGE_KEY = '@suvidha_language';

// Language detection from AsyncStorage
const languageDetector = {
    type: 'languageDetector' as const,
    async: true,
    detect: async (callback: (lang: string) => void) => {
        try {
            // Check if we're in a browser/native environment
            if (typeof window === 'undefined') {
                callback('hi'); // SSR: use default
                return;
            }
            const savedLanguage = await AsyncStorage.getItem(LANGUAGE_KEY);
            callback(savedLanguage || 'hi'); // Default to Hindi
        } catch (error) {
            // Silently fall back to default on SSR/web
            callback('hi');
        }
    },
    init: () => { },
    cacheUserLanguage: async (language: string) => {
        try {
            // Check if we're in a browser/native environment
            if (typeof window === 'undefined') {
                return; // Skip caching during SSR
            }
            await AsyncStorage.setItem(LANGUAGE_KEY, language);
        } catch (error) {
            // Silently fail on SSR/web - not critical
        }
    },
};

i18n
    .use(languageDetector as any)
    .use(initReactI18next)
    .init({
        resources: {
            en: { translation: en },
            hi: { translation: hi },
            mr: { translation: mr },
            gu: { translation: gu },
            bn: { translation: bn },
            ta: { translation: ta },
            te: { translation: te },
            kn: { translation: kn },
            ml: { translation: ml },
            pa: { translation: pa },
            or: { translation: or },
        },
        fallbackLng: 'hi',
        // Note: Don't set 'lng' here - let the languageDetector load the saved language
        interpolation: {
            escapeValue: false, // React already does escaping
        },
    });

// Helper function to change language and persist
export const changeLanguage = async (languageCode: string) => {
    await i18n.changeLanguage(languageCode);
    try {
        if (typeof window !== 'undefined') {
            await AsyncStorage.setItem(LANGUAGE_KEY, languageCode);
        }
    } catch (error) {
        // Silently fail - language change still works, just won't persist on web/SSR
    }
};

export default i18n;
