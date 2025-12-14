import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Colors } from '@/constants/theme';

interface SuggestionBarProps {
    suggestions: string[];
    onSelect: (suggestion: string) => void;
    visible: boolean;
}

export function SuggestionBar({ suggestions, onSelect, visible }: SuggestionBarProps) {
    if (!visible || suggestions.length === 0) {
        return null;
    }

    return (
        <View style={styles.container}>
            <View style={styles.suggestionsRow}>
                {suggestions.map((suggestion, index) => (
                    <TouchableOpacity
                        key={index}
                        style={[
                            styles.suggestionChip,
                            index === 0 && styles.englishChip,
                            index === 1 && styles.primaryChip,
                        ]}
                        onPress={() => onSelect(suggestion)}
                    >
                        <Text
                            style={[
                                styles.suggestionText,
                                index === 0 && styles.englishText,
                                index === 1 && styles.primaryText,
                            ]}
                            numberOfLines={1}
                        >
                            {suggestion}
                        </Text>
                    </TouchableOpacity>
                ))}
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        backgroundColor: Colors.light.background,
        borderTopWidth: 1,
        borderTopColor: Colors.light.border,
        paddingVertical: 8,
        paddingHorizontal: 12,
    },
    suggestionsRow: {
        flexDirection: 'row',
        gap: 8,
    },
    suggestionChip: {
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 20,
        backgroundColor: Colors.light.card,
        borderWidth: 1,
        borderColor: Colors.light.border,
        minWidth: 80,
        alignItems: 'center',
    },
    englishChip: {
        backgroundColor: Colors.light.background,
        borderColor: Colors.light.border,
    },
    primaryChip: {
        backgroundColor: Colors.light.primary,
        borderColor: Colors.light.primary,
    },
    suggestionText: {
        fontSize: 16,
        color: Colors.light.text,
    },
    englishText: {
        color: Colors.light.textSecondary,
        fontStyle: 'italic',
    },
    primaryText: {
        color: Colors.light.white,
        fontWeight: '600',
    },
});
