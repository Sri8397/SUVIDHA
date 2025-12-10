import React from 'react';
import { TouchableOpacity, StyleSheet } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';

interface FABProps {
    onPress: () => void;
}

export function FAB({ onPress }: FABProps) {
    return (
        <TouchableOpacity
            onPress={onPress}
            style={styles.fab}
            activeOpacity={0.8}
        >
            <FontAwesome name="plus" size={24} color="#FFFFFF" />
        </TouchableOpacity>
    );
}

const styles = StyleSheet.create({
    fab: {
        position: 'absolute',
        bottom: 100,
        right: 20,
        width: 56,
        height: 56,
        borderRadius: 28,
        backgroundColor: '#2E7D32',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 6,
        elevation: 8,
    },
});
