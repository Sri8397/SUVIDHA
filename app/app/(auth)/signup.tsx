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
import { useTranslation } from 'react-i18next';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { TransliterationInput } from '../../components/ui/TransliterationInput';

export default function SignupScreen() {
    const { t } = useTranslation();
    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');

    const isFormValid = name.length > 0 && phone.length >= 10 && password.length >= 6;

    const handleSignup = () => {
        if (isFormValid) {
            // Navigate to OTP screen with phone number
            router.push({
                pathname: '/(auth)/otp',
                params: { phone, name },
            });
        }
    };

    const handleLoginPress = () => {
        router.back();
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
                        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                            <FontAwesome name="arrow-left" size={20} color="#FFFFFF" />
                        </TouchableOpacity>
                        <Text style={styles.headerTitle}>{t('auth.yourStore')}</Text>
                    </View>

                    {/* Title Section */}
                    <View style={styles.titleSection}>
                        <Text style={styles.titleText}>{t('auth.signupTitle')}</Text>
                    </View>

                    {/* Signup Form */}
                    <View style={styles.formContainer}>
                        <View style={styles.inputContainer}>
                            <TransliterationInput
                                style={styles.input}
                                placeholder={t('auth.enterNamePlaceholder')}
                                placeholderTextColor="#9E9E9E"
                                value={name}
                                onChangeText={setName}
                            />
                        </View>

                        <View style={styles.inputContainer}>
                            <TextInput
                                style={styles.input}
                                placeholder={t('auth.enterPhonePlaceholder')}
                                placeholderTextColor="#9E9E9E"
                                keyboardType="phone-pad"
                                maxLength={10}
                                value={phone}
                                onChangeText={setPhone}
                            />
                        </View>

                        <View style={styles.inputContainer}>
                            <TextInput
                                style={styles.input}
                                placeholder={t('auth.createPasswordPlaceholder')}
                                placeholderTextColor="#9E9E9E"
                                secureTextEntry
                                value={password}
                                onChangeText={setPassword}
                            />
                        </View>

                        <TouchableOpacity
                            style={[styles.signupButton, !isFormValid && styles.signupButtonDisabled]}
                            onPress={handleSignup}
                            disabled={!isFormValid}
                        >
                            <Text style={styles.signupButtonText}>{t('auth.signupButton')}</Text>
                        </TouchableOpacity>

                        <TouchableOpacity onPress={handleLoginPress} style={styles.loginLink}>
                            <Text style={styles.loginLinkText}>
                                {t('auth.existingUserLogin')} <Text style={styles.loginLinkHighlight}>{t('auth.loginLink')}</Text>
                            </Text>
                        </TouchableOpacity>
                    </View>
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView >
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
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 16,
        paddingHorizontal: 16,
        gap: 12,
    },
    backButton: {
        padding: 4,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: '#FFFFFF',
    },
    titleSection: {
        paddingVertical: 24,
        paddingHorizontal: 24,
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    titleText: {
        fontSize: 24,
        fontWeight: '700',
        color: '#212121',
    },
    formContainer: {
        flex: 1,
        paddingHorizontal: 24,
        paddingTop: 24,
    },
    inputContainer: {
        marginBottom: 16,
    },
    input: {
        borderWidth: 1,
        borderColor: '#E0E0E0',
        borderRadius: 8,
        paddingHorizontal: 16,
        paddingVertical: 14,
        fontSize: 16,
        color: '#212121',
        backgroundColor: '#FFFFFF',
    },
    signupButton: {
        backgroundColor: '#2E7D32',
        paddingVertical: 16,
        borderRadius: 8,
        alignItems: 'center',
        marginTop: 8,
        marginBottom: 16,
    },
    signupButtonDisabled: {
        backgroundColor: '#A5D6A7',
    },
    signupButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    loginLink: {
        alignItems: 'center',
        paddingVertical: 8,
    },
    loginLinkText: {
        fontSize: 14,
        color: '#757575',
    },
    loginLinkHighlight: {
        color: '#2E7D32',
        fontWeight: '600',
    },
});
