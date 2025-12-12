import React from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, Linking } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router } from 'expo-router';
import { Colors } from '@/constants/theme';

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
            <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />

            {/* Header */}
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color={Colors.light.text} />
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
                            <MaterialIcons name="phone" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.phoneSupport')}</Text>
                            <Text style={styles.fieldValue}>1800-123-456 {t('support.tollFree')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textSecondary} />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard} onPress={handleEmail}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="email" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.emailSupport')}</Text>
                            <Text style={styles.fieldValue}>support@suvidha.app</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textSecondary} />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard} onPress={handleWhatsApp}>
                        <View style={styles.iconContainer}>
                            <FontAwesome name="whatsapp" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.whatsappSupport')}</Text>
                            <Text style={styles.fieldValue}>{t('support.startChat')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textSecondary} />
                    </TouchableOpacity>
                </View>

                {/* FAQ Section */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>{t('support.faq')}</Text>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="help-outline" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.faq1')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textSecondary} />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="help-outline" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.faq2')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textSecondary} />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="help-outline" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.faq3')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textSecondary} />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="help-outline" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.faq4')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textSecondary} />
                    </TouchableOpacity>
                </View>

                {/* App Info Section */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>{t('support.appInfo')}</Text>

                    <View style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="info-outline" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.appVersion')}</Text>
                            <Text style={styles.fieldValue}>1.0.0</Text>
                        </View>
                    </View>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="description" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.termsAndConditions')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textSecondary} />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="privacy-tip" size={24} color={Colors.light.primary} />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('support.privacyPolicy')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textSecondary} />
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.light.background,
    },
    headerSafeArea: {
        backgroundColor: Colors.light.white,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        backgroundColor: Colors.light.white,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.border,
    },
    backButton: {
        padding: 8,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: Colors.light.text,
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
        color: Colors.light.text,
        marginBottom: 16,
    },
    fieldCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: Colors.light.white,
        padding: 16,
        marginBottom: 12,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    iconContainer: {
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: Colors.light.primaryLight,
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
        color: Colors.light.text,
    },
    fieldValue: {
        fontSize: 14,
        color: Colors.light.textSecondary,
        marginTop: 4,
    },
});
