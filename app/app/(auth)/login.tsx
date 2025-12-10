import React, { useState } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    StyleSheet,
    StatusBar,
    KeyboardAvoidingView,
    Platform,
    ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { useAuth } from '@/contexts/auth-context';

export default function LoginScreen() {
    const [phone, setPhone] = useState('');
    const { login } = useAuth();

    const handleLogin = async () => {
        if (phone.length >= 10) {
            await login(phone);
            router.replace('/(tabs)');
        }
    };

    const handleSignupPress = () => {
        router.push('/(auth)/signup');
    };

    return (
        <SafeAreaView style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />
            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                style={styles.keyboardView}
            >
                <ScrollView
                    contentContainerStyle={styles.scrollContent}
                    keyboardShouldPersistTaps="handled"
                    showsVerticalScrollIndicator={false}
                >
                    {/* Header */}
                    <View style={styles.header}>
                        <Text style={styles.headerTitle}>अपना स्टोर</Text>
                    </View>

                    {/* Welcome Section */}
                    <View style={styles.welcomeSection}>
                        <Text style={styles.welcomeText}>
                            नमस्ते, अपना स्टोर में{'\n'}आपका स्वागत है!
                        </Text>
                    </View>

                    {/* Login Form */}
                    <View style={styles.formContainer}>
                        <Text style={styles.inputLabel}>फोन नंबर दर्ज करें</Text>
                        <View style={styles.phoneInputContainer}>
                            <View style={styles.countryCode}>
                                <Text style={styles.countryCodeText}>+91</Text>
                                <Text style={styles.dropdownArrow}>▼</Text>
                            </View>
                            <TextInput
                                style={styles.phoneInput}
                                placeholder="9876543210"
                                placeholderTextColor="#9E9E9E"
                                keyboardType="phone-pad"
                                maxLength={10}
                                value={phone}
                                onChangeText={setPhone}
                            />
                        </View>

                        <TouchableOpacity
                            style={[styles.loginButton, phone.length < 10 && styles.loginButtonDisabled]}
                            onPress={handleLogin}
                            disabled={phone.length < 10}
                        >
                            <Text style={styles.loginButtonText}>लॉग इन करें</Text>
                        </TouchableOpacity>

                        <TouchableOpacity onPress={handleSignupPress} style={styles.signupLink}>
                            <Text style={styles.signupLinkText}>
                                नया उपयोगकर्ता? <Text style={styles.signupLinkHighlight}>रजिस्टर करें</Text>
                            </Text>
                        </TouchableOpacity>
                    </View>
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    keyboardView: {
        flex: 1,
    },
    scrollContent: {
        flexGrow: 1,
    },
    header: {
        backgroundColor: '#2E7D32',
        paddingVertical: 16,
        paddingHorizontal: 20,
        borderBottomLeftRadius: 0,
        borderBottomRightRadius: 0,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: '#FFFFFF',
    },
    welcomeSection: {
        backgroundColor: '#4CAF50',
        paddingVertical: 40,
        paddingHorizontal: 24,
        borderBottomLeftRadius: 24,
        borderBottomRightRadius: 24,
    },
    welcomeText: {
        fontSize: 28,
        fontWeight: '700',
        color: '#FFFFFF',
        lineHeight: 40,
    },
    formContainer: {
        flex: 1,
        paddingHorizontal: 24,
        paddingTop: 32,
    },
    inputLabel: {
        fontSize: 14,
        color: '#757575',
        marginBottom: 8,
    },
    phoneInputContainer: {
        flexDirection: 'row',
        borderWidth: 1,
        borderColor: '#E0E0E0',
        borderRadius: 8,
        overflow: 'hidden',
        marginBottom: 24,
    },
    countryCode: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 12,
        backgroundColor: '#F5F5F5',
        borderRightWidth: 1,
        borderRightColor: '#E0E0E0',
        gap: 4,
    },
    countryCodeText: {
        fontSize: 16,
        fontWeight: '500',
        color: '#212121',
    },
    dropdownArrow: {
        fontSize: 10,
        color: '#757575',
    },
    phoneInput: {
        flex: 1,
        paddingHorizontal: 16,
        paddingVertical: 14,
        fontSize: 16,
        color: '#212121',
    },
    loginButton: {
        backgroundColor: '#2E7D32',
        paddingVertical: 16,
        borderRadius: 8,
        alignItems: 'center',
        marginBottom: 16,
    },
    loginButtonDisabled: {
        backgroundColor: '#A5D6A7',
    },
    loginButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    signupLink: {
        alignItems: 'center',
        paddingVertical: 8,
    },
    signupLinkText: {
        fontSize: 14,
        color: '#757575',
    },
    signupLinkHighlight: {
        color: '#2E7D32',
        fontWeight: '600',
    },
});
