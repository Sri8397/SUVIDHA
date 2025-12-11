import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, Image, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import i18n from '@/utils/i18n';

import { LanguageModal } from './language-modal';
import { NotificationList } from './notification-list';
import { useData } from '@/contexts/DataContext';

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
                            <FontAwesome name="arrow-left" size={20} color="#212121" />
                        </TouchableOpacity>
                    ) : (
                        <View style={styles.profileImage}>
                            <Image
                                source={require('@/assets/images/logo.png')}
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
                                {selectedLanguage === 'hi' ? 'हि' : selectedLanguage === 'en' ? 'En' :
                                    selectedLanguage === 'mr' ? 'मर' : selectedLanguage === 'gu' ? 'ગુ' :
                                        selectedLanguage === 'bn' ? 'বা' : selectedLanguage === 'ta' ? 'த' :
                                            selectedLanguage === 'te' ? 'తె' : selectedLanguage === 'kn' ? 'ಕ' :
                                                selectedLanguage === 'ml' ? 'മ' : selectedLanguage === 'pa' ? 'ਪੰ' :
                                                    selectedLanguage === 'or' ? 'ଓ' : 'हि'}
                            </Text>
                        </TouchableOpacity>
                    )}
                    <TouchableOpacity
                        style={styles.bellButton}
                        onPress={() => setNotificationVisible(true)}
                    >
                        <FontAwesome name="bell-o" size={24} color="#212121" />
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
                onSelectLanguage={setSelectedLanguage}
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
        backgroundColor: '#FFFFFF',
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
        backgroundColor: '#E8F5E9',
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
        color: '#212121',
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
        backgroundColor: '#2E7D32',
    },
    onlineText: {
        fontSize: 12,
        color: '#757575',
    },
    headerRight: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    langButton: {
        padding: 8,
        backgroundColor: '#F5F5F5',
        borderRadius: 8,
    },
    langText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#212121',
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
        backgroundColor: '#D32F2F',
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 4,
        borderWidth: 2,
        borderColor: '#FFFFFF',
    },
    badgeText: {
        fontSize: 10,
        fontWeight: '700',
        color: '#FFFFFF',
    },
    backButton: {
        width: 40,
        height: 40,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 8,
    },
});
