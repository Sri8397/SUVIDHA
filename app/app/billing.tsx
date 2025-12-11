import React from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router } from 'expo-router';

export default function BillingScreen() {
    const { t } = useTranslation();
    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            {/* Header */}
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color="#212121" />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>{t('billing.title')}</Text>
                    <View style={styles.backButton} />
                </View>
            </SafeAreaView>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Payment Methods Section */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>{t('billing.paymentMethods')}</Text>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="account-balance" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('billing.bankAccount')}</Text>
                            <Text style={styles.fieldValue}>HDFC Bank - ****1234</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="phone-android" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>UPI</Text>
                            <Text style={styles.fieldValue}>store@upi</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="qr-code" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('billing.qrCode')}</Text>
                            <Text style={styles.fieldValue}>{t('billing.active')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>
                </View>

                {/* Invoice Settings Section */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>{t('billing.invoiceSettings')}</Text>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="receipt" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('billing.invoiceTemplate')}</Text>
                            <Text style={styles.fieldValue}>{t('billing.defaultTemplate')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="format-list-numbered" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('billing.invoiceNumberSeries')}</Text>
                            <Text style={styles.fieldValue}>INV-2024-</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>
                </View>

                {/* Billing History Section */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>{t('billing.billingHistory')}</Text>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="history" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('billing.viewPastPayments')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#757575" />
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.fieldCard}>
                        <View style={styles.iconContainer}>
                            <MaterialIcons name="download" size={24} color="#2E7D32" />
                        </View>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('billing.downloadReports')}</Text>
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
