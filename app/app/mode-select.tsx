import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, StatusBar, Image } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useAppMode, AppMode } from '@/contexts/ModeContext';

export default function ModeSelectScreen() {
    const { t } = useTranslation();
    const router = useRouter();
    const { setAppMode } = useAppMode();

    const handleModeSelect = async (mode: AppMode) => {
        await setAppMode(mode);
        router.replace('/(tabs)');
    };

    return (
        <SafeAreaView style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            <View style={styles.content}>
                {/* Logo */}
                <View style={styles.logoContainer}>
                    <Image
                        source={require('@/assets/images/icon.png')}
                        style={styles.logo}
                        resizeMode="contain"
                    />
                </View>

                {/* Title */}
                <Text style={styles.title}>{t('modeSelect.title')}</Text>
                <Text style={styles.subtitle}>{t('modeSelect.subtitle')}</Text>

                {/* Mode Options */}
                <View style={styles.optionsContainer}>
                    {/* Shopkeeper Mode */}
                    <TouchableOpacity
                        style={styles.optionCard}
                        onPress={() => handleModeSelect('shopkeeper')}
                        activeOpacity={0.8}
                    >
                        <View style={[styles.iconContainer, { backgroundColor: '#E3F2FD' }]}>
                            <MaterialIcons name="storefront" size={40} color="#1976D2" />
                        </View>
                        <Text style={styles.optionTitle}>{t('modeSelect.shopkeeper')}</Text>
                        <Text style={styles.optionDescription}>{t('modeSelect.shopkeeperDesc')}</Text>
                    </TouchableOpacity>

                    {/* Personal Finance Mode */}
                    <TouchableOpacity
                        style={styles.optionCard}
                        onPress={() => handleModeSelect('personal')}
                        activeOpacity={0.8}
                    >
                        <View style={[styles.iconContainer, { backgroundColor: '#E8F5E9' }]}>
                            <FontAwesome name="user" size={36} color="#2E7D32" />
                        </View>
                        <Text style={styles.optionTitle}>{t('modeSelect.personal')}</Text>
                        <Text style={styles.optionDescription}>{t('modeSelect.personalDesc')}</Text>
                    </TouchableOpacity>
                </View>

                {/* Footer Note */}
                <Text style={styles.footerNote}>{t('modeSelect.switchLater')}</Text>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    content: {
        flex: 1,
        paddingHorizontal: 24,
        paddingTop: 40,
        alignItems: 'center',
    },
    logoContainer: {
        width: 80,
        height: 80,
        borderRadius: 20,
        backgroundColor: '#E8F5E9',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 24,
    },
    logo: {
        width: 60,
        height: 60,
    },
    title: {
        fontSize: 28,
        fontWeight: '700',
        color: '#212121',
        textAlign: 'center',
        marginBottom: 8,
    },
    subtitle: {
        fontSize: 16,
        color: '#757575',
        textAlign: 'center',
        marginBottom: 40,
    },
    optionsContainer: {
        width: '100%',
        gap: 16,
    },
    optionCard: {
        backgroundColor: '#FAFAFA',
        borderRadius: 16,
        padding: 24,
        alignItems: 'center',
        borderWidth: 2,
        borderColor: '#E0E0E0',
    },
    iconContainer: {
        width: 80,
        height: 80,
        borderRadius: 40,
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 16,
    },
    optionTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 8,
    },
    optionDescription: {
        fontSize: 14,
        color: '#757575',
        textAlign: 'center',
        lineHeight: 20,
    },
    footerNote: {
        position: 'absolute',
        bottom: 40,
        fontSize: 13,
        color: '#9E9E9E',
        textAlign: 'center',
    },
});
