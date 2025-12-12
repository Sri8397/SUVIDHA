import React from 'react';
import { Stack } from 'expo-router';
import { StyleSheet } from 'react-native';
import { Colors } from '@/constants/theme';

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
        backgroundColor: Colors.light.white,
    },
});
