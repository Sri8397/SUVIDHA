import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

// Import shared locale files (common to both modes)
import sharedEn from './locales/shared/en.json';
import sharedHi from './locales/shared/hi.json';

// Import shop-specific locale files
import shopEn from './locales/shop/en.json';
import shopHi from './locales/shop/hi.json';
// import shopBn from './locales/shop/bn.json';
// import shopGu from './locales/shop/gu.json';
// import shopKn from './locales/shop/kn.json';
// import shopMl from './locales/shop/ml.json';
// import shopMr from './locales/shop/mr.json';
// import shopOr from './locales/shop/or.json';
// import shopPa from './locales/shop/pa.json';
// import shopTa from './locales/shop/ta.json';
// import shopTe from './locales/shop/te.json';

// Import finance-specific locale files
import financeHi from './locales/finance/hi.json';
import financeEn from './locales/finance/en.json';

// Helper to deep merge translation objects
const deepMerge = (...objects: any[]): any => {
    const result: any = {};
    for (const obj of objects) {
        for (const key in obj) {
            if (obj[key] && typeof obj[key] === 'object' && !Array.isArray(obj[key])) {
                result[key] = deepMerge(result[key] || {}, obj[key]);
            } else {
                result[key] = obj[key];
            }
        }
    }
    return result;
};

// Merge shared + shop + finance translations
// Shop translations take precedence, then finance, then shared
const resources = {
    en: { translation: deepMerge(sharedEn, shopEn, financeEn) },
    hi: { translation: deepMerge(sharedHi, shopHi, financeHi) },
    // bn: { translation: deepMerge(sharedHi, shopBn) }, // Fallback shared to Hindi until Bengali shared is created
    // gu: { translation: deepMerge(sharedHi, shopGu) },
    // kn: { translation: deepMerge(sharedHi, shopKn) },
    // ml: { translation: deepMerge(sharedHi, shopMl) },
    // mr: { translation: deepMerge(sharedHi, shopMr) },
    // or: { translation: deepMerge(sharedHi, shopOr) },
    // pa: { translation: deepMerge(sharedHi, shopPa) },
    // ta: { translation: deepMerge(sharedHi, shopTa) },
    // te: { translation: deepMerge(sharedHi, shopTe) },
};

i18n
    .use(initReactI18next)
    .init({
        // @ts-ignore
        compatibilityJSON: 'v3',
        resources,
        lng: 'hi', // default language
        fallbackLng: 'en',
        interpolation: {
            escapeValue: false,
        },
        react: {
            useSuspense: false,
        }
    });

// Helper function to change language
export const changeLanguage = async (languageCode: string) => {
    await i18n.changeLanguage(languageCode);
};

export default i18n;
