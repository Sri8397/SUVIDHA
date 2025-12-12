import React, { useState, useRef, useEffect } from 'react';
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
import { router, useLocalSearchParams } from 'expo-router';
import { useTranslation } from 'react-i18next';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useAuth } from '@/contexts/auth-context';
import { Colors } from '@/constants/theme';

export default function OTPScreen() {
    const { t } = useTranslation();
    const { phone, name } = useLocalSearchParams<{ phone: string; name: string }>();
    const { signup } = useAuth();
    const [otp, setOtp] = useState(['', '', '', '', '', '']);
    const [resendTimer, setResendTimer] = useState(30);
    const inputRefs = useRef<(TextInput | null)[]>([]);

    useEffect(() => {
        if (resendTimer > 0) {
            const timer = setTimeout(() => setResendTimer(resendTimer - 1), 1000);
            return () => clearTimeout(timer);
        }
    }, [resendTimer]);

    const handleOtpChange = (value: string, index: number) => {
        if (value.length > 1) {
            value = value[value.length - 1];
        }

        const newOtp = [...otp];
        newOtp[index] = value;
        setOtp(newOtp);

        // Auto focus next input
        if (value && index < 5) {
            inputRefs.current[index + 1]?.focus();
        }
    };

    const handleKeyPress = (e: any, index: number) => {
        if (e.nativeEvent.key === 'Backspace' && !otp[index] && index > 0) {
            inputRefs.current[index - 1]?.focus();
        }
    };

    const isOtpComplete = otp.every(digit => digit !== '');

    const handleVerify = async () => {
        if (isOtpComplete) {
            // Any 6-digit OTP works
            await signup(name || '', phone || '');
            // Navigate to mode selection after successful signup
            router.replace('/mode-select');
        }
    };

    const handleResend = () => {
        if (resendTimer === 0) {
            setResendTimer(30);
            // In real app, would trigger OTP resend
        }
    };

    const handleBack = () => {
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
                        <TouchableOpacity onPress={handleBack} style={styles.backButton}>
                            <FontAwesome name="arrow-left" size={20} color={Colors.light.white} />
                        </TouchableOpacity>
                        <Text style={styles.headerTitle}>{t('auth.yourStore')}</Text>
                    </View>

                    {/* Title Section */}
                    <View style={styles.titleSection}>
                        <Text style={styles.titleText}>{t('auth.otpVerification')}</Text>
                        <Text style={styles.subtitleText}>
                            {t('auth.otpSentMessage', { phone: phone })}
                        </Text>
                    </View>

                    {/* OTP Form */}
                    <View style={styles.formContainer}>
                        <View style={styles.otpContainer}>
                            {otp.map((digit, index) => (
                                <TextInput
                                    key={index}
                                    ref={(ref) => { inputRefs.current[index] = ref; }}
                                    style={[styles.otpInput, digit && styles.otpInputFilled]}
                                    value={digit}
                                    onChangeText={(value) => handleOtpChange(value, index)}
                                    onKeyPress={(e) => handleKeyPress(e, index)}
                                    keyboardType="number-pad"
                                    maxLength={1}
                                    selectTextOnFocus
                                />
                            ))}
                        </View>

                        <TouchableOpacity
                            style={[styles.verifyButton, !isOtpComplete && styles.verifyButtonDisabled]}
                            onPress={handleVerify}
                            disabled={!isOtpComplete}
                        >
                            <Text style={styles.verifyButtonText}>{t('auth.verifyButton')}</Text>
                        </TouchableOpacity>

                        <TouchableOpacity
                            onPress={handleResend}
                            style={styles.resendLink}
                            disabled={resendTimer > 0}
                        >
                            <Text style={styles.resendLinkText}>
                                {t('auth.otpNotReceived')}{' '}
                                <Text style={[styles.resendLinkHighlight, resendTimer > 0 && styles.resendDisabled]}>
                                    {t('auth.resend')} {resendTimer > 0 ? `(${resendTimer}s)` : ''}
                                </Text>
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
        marginBottom: 8,
    },
    subtitleText: {
        fontSize: 14,
        color: Colors.light.textSecondary,
        lineHeight: 22,
    },
    formContainer: {
        flex: 1,
        paddingHorizontal: 24,
        paddingTop: 32,
    },
    otpContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 32,
    },
    otpInput: {
        width: 48,
        height: 56,
        borderWidth: 1,
        borderColor: Colors.light.border,
        borderRadius: 8,
        textAlign: 'center',
        fontSize: 24,
        fontWeight: '600',
        color: Colors.light.text,
        backgroundColor: Colors.light.white,
    },
    otpInputFilled: {
        borderColor: Colors.light.primary,
        backgroundColor: Colors.light.primaryContainer,
    },
    verifyButton: {
        backgroundColor: Colors.light.primary,
        paddingVertical: 16,
        borderRadius: 8,
        alignItems: 'center',
        marginBottom: 16,
    },
    verifyButtonDisabled: {
        backgroundColor: Colors.light.primaryDisabled,
    },
    verifyButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.white,
    },
    resendLink: {
        alignItems: 'center',
        paddingVertical: 8,
    },
    resendLinkText: {
        fontSize: 14,
        color: Colors.light.textSecondary,
    },
    resendLinkHighlight: {
        color: Colors.light.primary,
        fontWeight: '600',
    },
    resendDisabled: {
        color: Colors.light.textTertiary,
    },
});
