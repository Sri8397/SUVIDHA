import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, Image, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import i18n from '@/i18n';
import { Colors } from '@/constants/theme';

import { LanguageModal } from './language-modal';
import { NotificationList } from './notification-list';
import { useData } from '@/contexts/DataContext';

const languageLabels: Record<string, string> = {
    'hi': 'अ',
    'en': 'En',
    'mr': 'म',
    'gu': 'ગ',
    'bn': 'ব',
    'ta': 'த',
    'te': 'తె',
    'kn': 'ಕ',
    'ml': 'മ',
    'pa': 'ਪ',
    'or': 'ଓ',
};

interface HeaderProps {
    title: string;
    showOnlineStatus?: boolean;
    showLanguageToggle?: boolean;
    showBackButton?: boolean;
}

export function Header({ title, showOnlineStatus = false, showLanguageToggle = true, showBackButton = false }: HeaderProps) {
    const { t, i18n: i18nInstance } = useTranslation();
    const router = useRouter();
    const { getVisibleNotifications } = useData();
    const [languageModalVisible, setLanguageModalVisible] = useState(false);
    const [notificationVisible, setNotificationVisible] = useState(false);
    const [selectedLanguage, setSelectedLanguage] = useState(i18n.language);

    // Listen to language change events
    useEffect(() => {
        const handleLanguageChange = (lng: string) => {
            setSelectedLanguage(lng);
        };

        i18nInstance.on('languageChanged', handleLanguageChange);

        // Set initial language
        setSelectedLanguage(i18nInstance.language);

        return () => {
            i18nInstance.off('languageChanged', handleLanguageChange);
        };
    }, [i18nInstance]);

    // Get notification count from context (consistent across all pages)
    const visibleNotifications = getVisibleNotifications();
    const notificationCount = Math.min(visibleNotifications.length, 99);
    const hasNotifications = visibleNotifications.length > 0;

    return (
        <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
            <View style={styles.header}>
                <View style={styles.headerLeft}>
                    {showBackButton ? (
                        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                            <FontAwesome name="arrow-left" size={20} color={Colors.light.text} />
                        </TouchableOpacity>
                    ) : (
                        <View style={styles.profileImage}>
                            <Image
                                source={require('@/assets/images/icon.png')}
                                style={styles.logo}
                                resizeMode="contain"
                            />
                        </View>
                    )}
                    <View>
                        <Text style={styles.headerTitle}>{title}</Text>
                        {showOnlineStatus && (
                            <View style={styles.onlineStatus}>
                                <View style={styles.onlineDot} />
                                <Text style={styles.onlineText}>{t('common.online')}</Text>
                            </View>
                        )}
                    </View>
                </View>
                <View style={styles.headerRight}>
                    {showLanguageToggle && (
                        <TouchableOpacity
                            style={styles.langButton}
                            onPress={() => setLanguageModalVisible(true)}
                        >
                            <Text style={styles.langText}>
                                {languageLabels[selectedLanguage] || selectedLanguage?.substring(0, 2).toUpperCase()}
                            </Text>
                        </TouchableOpacity>
                    )}
                    <TouchableOpacity
                        style={styles.bellButton}
                        onPress={() => setNotificationVisible(true)}
                    >
                        <FontAwesome name="bell-o" size={24} color={Colors.light.text} />
                        {hasNotifications && (
                            <View style={styles.notificationBadge}>
                                <Text style={styles.badgeText}>
                                    {notificationCount > 9 ? '9+' : notificationCount}
                                </Text>
                            </View>
                        )}
                    </TouchableOpacity>
                </View>
            </View>

            <LanguageModal
                visible={languageModalVisible}
                onClose={() => setLanguageModalVisible(false)}
                selectedLanguage={selectedLanguage}
                onSelectLanguage={(lang) => {
                    // LanguageModal already calls changeLanguage() which persists to AsyncStorage
                    // Just update local state here
                    setSelectedLanguage(lang);
                }}
            />

            <NotificationList
                visible={notificationVisible}
                onClose={() => setNotificationVisible(false)}
            />
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    headerSafeArea: {
        backgroundColor: Colors.light.card,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 12,
    },
    headerLeft: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    profileImage: {
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: Colors.light.primaryLight,
        overflow: 'hidden',
        justifyContent: 'center',
        alignItems: 'center',
    },
    logo: {
        width: 36,
        height: 36,
    },
    headerTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
    },
    onlineStatus: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
        marginTop: 2,
    },
    onlineDot: {
        width: 8,
        height: 8,
        borderRadius: 4,
        backgroundColor: Colors.light.primary,
    },
    onlineText: {
        fontSize: 12,
        color: Colors.light.textSecondary,
    },
    headerRight: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    langButton: {
        padding: 8,
        backgroundColor: Colors.light.background,
        borderRadius: 8,
    },
    langText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.text,
    },
    bellButton: {
        position: 'relative',
        padding: 4,
    },
    notificationBadge: {
        position: 'absolute',
        top: -4,
        right: -6,
        minWidth: 18,
        height: 18,
        borderRadius: 9,
        backgroundColor: Colors.light.error,
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 4,
        borderWidth: 2,
        borderColor: Colors.light.white,
    },
    badgeText: {
        fontSize: 10,
        fontWeight: '700',
        color: Colors.light.white,
    },
    backButton: {
        width: 40,
        height: 40,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 8,
    },
});
