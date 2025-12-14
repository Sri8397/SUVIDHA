import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, StatusBar, Image, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useAppMode, AppMode } from '@/contexts/ModeContext';
import { Colors } from '@/constants/theme';

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
            <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />
            <ScrollView contentContainerStyle={styles.optionsScrollContainer}>
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
                            <View style={[styles.iconContainer, { backgroundColor: Colors.light.infoLight || '#E3F2FD' }]}>
                                <MaterialIcons name="storefront" size={40} color={Colors.light.info || '#1976D2'} />
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
                            <View style={[styles.iconContainer, { backgroundColor: Colors.light.primaryLight }]}>
                                <FontAwesome name="user" size={36} color={Colors.light.primary} />
                            </View>
                            <Text style={styles.optionTitle}>{t('modeSelect.personal')}</Text>
                            <Text style={styles.optionDescription}>{t('modeSelect.personalDesc')}</Text>
                        </TouchableOpacity>
                    </View>

                    {/* Footer Note */}
                    <Text style={styles.footerNote}>{t('modeSelect.switchLater')}</Text>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    optionsScrollContainer: {
        flexGrow: 1,
    },
    container: {
        flex: 1,
        backgroundColor: Colors.light.white,
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
        backgroundColor: Colors.light.primaryLight,
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
        color: Colors.light.text,
        textAlign: 'center',
        marginBottom: 8,
    },
    subtitle: {
        fontSize: 16,
        color: Colors.light.textSecondary,
        textAlign: 'center',
        marginBottom: 40,
    },
    optionsContainer: {
        width: '100%',
        gap: 16,
    },
    optionCard: {
        backgroundColor: Colors.light.background, // Used FAFAFA which is close to background
        borderRadius: 16,
        padding: 24,
        alignItems: 'center',
        borderWidth: 2,
        borderColor: Colors.light.border,
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
        color: Colors.light.text,
        marginBottom: 8,
    },
    optionDescription: {
        fontSize: 14,
        color: Colors.light.textSecondary,
        textAlign: 'center',
        lineHeight: 20,
    },
    footerNote: {
        marginTop: 40,
        marginBottom: 20,
        fontSize: 13,
        color: Colors.light.textTertiary,
        textAlign: 'center',
    },
});
