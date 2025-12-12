import React, { useCallback, useRef } from 'react';
import { TextInput, TextInputProps } from 'react-native';
import { useTransliteration } from '../../hooks/useTransliteration';

interface TransliterationInputProps extends TextInputProps {
    onChangeText?: (text: string) => void;
}

export function TransliterationInput(props: TransliterationInputProps) {
    const { transliterate, isEnabled } = useTransliteration();
    const { onChangeText, ...otherProps } = props;
    const previousTextRef = useRef('');

    const handleChangeText = useCallback((text: string) => {
        if (!onChangeText) return;

        // If transliteration is disabled (English), pass through as-is
        if (!isEnabled) {
            previousTextRef.current = text;
            onChangeText(text);
            return;
        }

        const previousText = previousTextRef.current;

        // Check if user just typed a space (word completed)
        if (text.length > previousText.length && text.endsWith(' ')) {
            // Extract the last word (before the space that was just typed)
            const beforeSpace = text.slice(0, -1); // Remove the trailing space
            const words = beforeSpace.split(' ');
            const lastWord = words[words.length - 1];

            if (lastWord && lastWord.trim().length > 0) {
                // Transliterate only the last word
                const transliteratedWord = transliterate(lastWord);

                // Replace the last word with transliterated version
                words[words.length - 1] = transliteratedWord;
                const newText = words.join(' ') + ' '; // Add back the space

                previousTextRef.current = newText;
                onChangeText(newText);
                return;
            }
        }

        // For all other cases (typing, deleting, etc.), pass through as-is
        previousTextRef.current = text;
        onChangeText(text);
    }, [onChangeText, isEnabled, transliterate]);

    return (
        <TextInput
            {...otherProps}
            onChangeText={handleChangeText}
        />
    );
}
