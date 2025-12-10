import React from 'react';
import { Stack } from 'expo-router';
import { View, StyleSheet } from 'react-native';

export default function AuthLayout() {
    return (
        <Stack
            screenOptions={{
                headerShown: false,
                contentStyle: styles.content,
            }}
        >
            <Stack.Screen name="login" />
            <Stack.Screen name="signup" />
            <Stack.Screen name="otp" />
        </Stack>
    );
}

const styles = StyleSheet.create({
    content: {
        backgroundColor: '#FFFFFF',
    },
});
