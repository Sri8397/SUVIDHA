import React from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, Linking } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router } from 'expo-router';

export default function SupportScreen() {
    const { t } = useTranslation();
    const handleCall = () => {
        Linking.openURL('tel:+911800123456');
    };

    const handleEmail = () => {
        Linking.openURL('mailto:support@suvidha.app');
    };

    const handleWhatsApp = () => {
        Linking.openURL('https://wa.me/911800123456');
    };

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            {/* Header */}
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color="#212121" />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>{t('support.title')}</Text>
                    <View style={styles.backButton} />
                </View>
            </SafeAreaView>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Contact Support Section */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>{t('support.contactUs')}</Text>

                    <TouchableOpacity style={styles.fieldCard} onPress={handleCall}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="phone" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.phoneSupport')}</Text>
                            <Text style={styles.fieldValue}>1800-123-456 {t('support.tollFree')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard} onPress={handleEmail}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="email" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.emailSupport')}</Text>
                            <Text style={styles.fieldValue}>support@suvidha.app</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard} onPress={handleWhatsApp}>
                        <View style={styles.iconContainer}>
                            <FontAwesome name="whatsapp" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.whatsappSupport')}</Text>
                            <Text style={styles.fieldValue}>{t('support.startChat')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>
                </View>

                {/* FAQ Section */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>{t('support.faq')}</Text>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="help-outline" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.faq1')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="help-outline" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.faq2')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="help-outline" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.faq3')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="help-outline" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.faq4')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>
                </View>

                {/* App Info Section */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>{t('support.appInfo')}</Text>

                    <View style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="info-outline" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.appVersion')}</Text>
                            <Text style={styles.fieldValue}>1.0.0</Text>
                        </View>
                    </View>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="description" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.termsAndConditions')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="privacy-tip" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.privacyPolicy')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F5F5F5',
    },
    headerSafeArea: {
        backgroundColor: '#FFFFFF',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    backButton: {
        padding: 8,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: '#212121',
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 24,
    },
    sectionContainer: {
        paddingHorizontal: 16,
        marginTop: 24,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 16,
    },
    fieldCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        padding: 16,
        marginBottom: 12,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    iconContainer: {
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: '#E8F5E9',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    fieldContent: {
        flex: 1,
    },
    fieldLabel: {
        fontSize: 16,
        fontWeight: '500',
        color: '#212121',
    },
    fieldValue: {
        fontSize: 14,
        color: '#757575',
        marginTop: 4,
    },
});
