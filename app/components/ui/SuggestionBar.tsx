import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

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
        backgroundColor: '#F5F5F5',
        borderTopWidth: 1,
        borderTopColor: '#E0E0E0',
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
        backgroundColor: '#FFFFFF',
        borderWidth: 1,
        borderColor: '#E0E0E0',
        minWidth: 80,
        alignItems: 'center',
    },
    englishChip: {
        backgroundColor: '#F0F0F0',
        borderColor: '#D0D0D0',
    },
    primaryChip: {
        backgroundColor: '#4CAF50',
        borderColor: '#4CAF50',
    },
    suggestionText: {
        fontSize: 16,
        color: '#333333',
    },
    englishText: {
        color: '#666666',
        fontStyle: 'italic',
    },
    primaryText: {
        color: '#FFFFFF',
        fontWeight: '600',
    },
});
