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
import { Colors } from '@/constants/theme';

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
            <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />
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
                            <FontAwesome name="arrow-left" size={20} color={Colors.light.white} />
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
                                placeholderTextColor={Colors.light.textTertiary}
                                value={name}
                                onChangeText={setName}
                            />
                        </View>

                        <View style={styles.inputContainer}>
                            <TextInput
                                style={styles.input}
                                placeholder={t('auth.enterPhonePlaceholder')}
                                placeholderTextColor={Colors.light.textTertiary}
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
                                placeholderTextColor={Colors.light.textTertiary}
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
        backgroundColor: Colors.light.white,
    },
    keyboardView: {
        flex: 1,
    },
    scrollContent: {
        flexGrow: 1,
    },
    header: {
        backgroundColor: Colors.light.primary,
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
        color: Colors.light.white,
    },
    titleSection: {
        paddingVertical: 24,
        paddingHorizontal: 24,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.border,
    },
    titleText: {
        fontSize: 24,
        fontWeight: '700',
        color: Colors.light.text,
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
        borderColor: Colors.light.border,
        borderRadius: 8,
        paddingHorizontal: 16,
        paddingVertical: 14,
        fontSize: 16,
        color: Colors.light.text,
        backgroundColor: Colors.light.white,
    },
    signupButton: {
        backgroundColor: Colors.light.primary,
        paddingVertical: 16,
        borderRadius: 8,
        alignItems: 'center',
        marginTop: 8,
        marginBottom: 16,
    },
    signupButtonDisabled: {
        backgroundColor: Colors.light.primaryDisabled,
    },
    signupButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.white,
    },
    loginLink: {
        alignItems: 'center',
        paddingVertical: 8,
    },
    loginLinkText: {
        fontSize: 14,
        color: Colors.light.textSecondary,
    },
    loginLinkHighlight: {
        color: Colors.light.primary,
        fontWeight: '600',
    },
});
