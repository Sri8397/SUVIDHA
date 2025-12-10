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
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useAuth } from '@/contexts/auth-context';

export default function OTPScreen() {
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
            router.replace('/(tabs)');
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
                        <TouchableOpacity onPress={handleBack} style={styles.backButton}>
                            <FontAwesome name="arrow-left" size={20} color="#FFFFFF" />
                        </TouchableOpacity>
                        <Text style={styles.headerTitle}>अपना स्टोर</Text>
                    </View>

                    {/* Title Section */}
                    <View style={styles.titleSection}>
                        <Text style={styles.titleText}>OTP सत्यापन</Text>
                        <Text style={styles.subtitleText}>
                            कृपया +91 {phone} पर भेजा गया{'\n'}6-अंकीय OTP दर्ज करें।
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
                            <Text style={styles.verifyButtonText}>सत्यापित करें</Text>
                        </TouchableOpacity>

                        <TouchableOpacity
                            onPress={handleResend}
                            style={styles.resendLink}
                            disabled={resendTimer > 0}
                        >
                            <Text style={styles.resendLinkText}>
                                OTP प्राप्त नहीं हुआ?{' '}
                                <Text style={[styles.resendLinkHighlight, resendTimer > 0 && styles.resendDisabled]}>
                                    पुनः भेजें {resendTimer > 0 ? `(${resendTimer}s)` : ''}
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
        marginBottom: 8,
    },
    subtitleText: {
        fontSize: 14,
        color: '#757575',
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
        borderColor: '#E0E0E0',
        borderRadius: 8,
        textAlign: 'center',
        fontSize: 24,
        fontWeight: '600',
        color: '#212121',
        backgroundColor: '#FFFFFF',
    },
    otpInputFilled: {
        borderColor: '#2E7D32',
        backgroundColor: '#F1F8E9',
    },
    verifyButton: {
        backgroundColor: '#2E7D32',
        paddingVertical: 16,
        borderRadius: 8,
        alignItems: 'center',
        marginBottom: 16,
    },
    verifyButtonDisabled: {
        backgroundColor: '#A5D6A7',
    },
    verifyButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    resendLink: {
        alignItems: 'center',
        paddingVertical: 8,
    },
    resendLinkText: {
        fontSize: 14,
        color: '#757575',
    },
    resendLinkHighlight: {
        color: '#2E7D32',
        fontWeight: '600',
    },
    resendDisabled: {
        color: '#9E9E9E',
    },
});
