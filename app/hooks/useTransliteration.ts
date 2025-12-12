import { useState, useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import Sanscript from '@indic-transliteration/sanscript';

// Language code to Sanscript scheme mapping
const LANGUAGE_TO_SCHEME: Record<string, string> = {
    hi: 'devanagari',
    mr: 'devanagari',
    bn: 'bengali',
    ta: 'tamil',
    te: 'telugu',
    kn: 'kannada',
    ml: 'malayalam',
    gu: 'gujarati',
    pa: 'gurmukhi',
    or: 'oriya',
};

export function useTransliteration() {
    const { i18n } = useTranslation();
    const currentLanguage = i18n.language;
    const currentScheme = LANGUAGE_TO_SCHEME[currentLanguage];
    const isEnabled = currentScheme !== undefined;

    const transliterate = useCallback((text: string): string => {
        if (!text || !currentScheme) return text;

        try {
            // Normalize to lowercase to avoid ITRANS special character issues
            // (e.g., uppercase 'H' means visarga 'ः' in ITRANS)
            const normalizedText = text.toLowerCase();
            // Transliterate from ITRANS (phonetic English) to target script
            return Sanscript.t(normalizedText, 'itrans', currentScheme);
        } catch (error) {
            console.error('Transliteration error:', error);
            return text;
        }
    }, [currentScheme]);

    const getSuggestions = useCallback((word: string): string[] => {
        if (!word || !currentScheme) return [word];

        const suggestions: string[] = [];

        // Option 1: Keep English
        suggestions.push(word);

        // Option 2: Primary transliteration (normalized to lowercase)
        try {
            const normalizedWord = word.toLowerCase();
            const transliterated = Sanscript.t(normalizedWord, 'itrans', currentScheme);
            if (transliterated !== normalizedWord) {
                suggestions.push(transliterated);
            }
        } catch (error) {
            console.error('Transliteration error:', error);
        }

        return suggestions.slice(0, 3); // Max 3 suggestions
    }, [currentScheme]);

    return {
        transliterate,
        getSuggestions,
        isEnabled,
        currentLanguage,
        currentScheme,
    };
}
