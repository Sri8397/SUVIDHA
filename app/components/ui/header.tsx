import React, { useState } from 'react';
import { View, Text, TouchableOpacity, Image, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';

import { LanguageModal } from './language-modal';
import { NotificationList } from './notification-list';

interface HeaderProps {
    title: string;
    showOnlineStatus?: boolean;
    showLanguageToggle?: boolean;
}

export function Header({ title, showOnlineStatus = false, showLanguageToggle = true }: HeaderProps) {
    const [languageModalVisible, setLanguageModalVisible] = useState(false);
    const [notificationVisible, setNotificationVisible] = useState(false);
    const [selectedLanguage, setSelectedLanguage] = useState('hi');

    return (
        <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
            <View style={styles.header}>
                <View style={styles.headerLeft}>
                    <View style={styles.profileImage}>
                        <Image
                            source={require('@/assets/images/logo.png')}
                            style={styles.logo}
                            resizeMode="contain"
                        />
                    </View>
                    <View>
                        <Text style={styles.headerTitle}>{title}</Text>
                        {showOnlineStatus && (
                            <View style={styles.onlineStatus}>
                                <View style={styles.onlineDot} />
                                <Text style={styles.onlineText}>ऑनलाइन</Text>
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
                                {selectedLanguage === 'hi' ? 'अ' : selectedLanguage === 'en' ? 'En' : 'अ'}
                            </Text>
                        </TouchableOpacity>
                    )}
                    <TouchableOpacity
                        style={styles.bellButton}
                        onPress={() => setNotificationVisible(true)}
                    >
                        <FontAwesome name="bell" size={22} color="#D32F2F" />
                        <View style={styles.notificationBadge} />
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
        top: 2,
        right: 2,
        width: 10,
        height: 10,
        borderRadius: 5,
        backgroundColor: '#D32F2F',
    },
});
