import React, { useState } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, Modal, Alert, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { router } from 'expo-router';
import { removeData } from '@/services/storage';
import { useData } from '@/contexts/DataContext';
import { STORAGE_KEYS, Customer } from '@/types';
import { getData, setData } from '@/services/storage';

// Data types that can be cleared
interface ClearOptions {
    products: boolean;
    customers: boolean;
    sales: boolean;
    expenses: boolean;
    bakaya: boolean; // Outstanding balances
}

export default function StoreSettingsScreen() {
    const { refreshAll } = useData();
    const [showClearDataModal, setShowClearDataModal] = useState(false);
    const [isClearing, setIsClearing] = useState(false);
    const [clearOptions, setClearOptions] = useState<ClearOptions>({
        products: true,
        customers: true,
        sales: true,
        expenses: true,
        bakaya: true,
    });

    const toggleOption = (key: keyof ClearOptions) => {
        setClearOptions(prev => ({ ...prev, [key]: !prev[key] }));
    };

    const toggleAll = () => {
        const allSelected = Object.values(clearOptions).every(v => v);
        setClearOptions({
            products: !allSelected,
            customers: !allSelected,
            sales: !allSelected,
            expenses: !allSelected,
            bakaya: !allSelected,
        });
    };

    const anySelected = Object.values(clearOptions).some(v => v);
    const allSelected = Object.values(clearOptions).every(v => v);

    const handleClearData = async () => {
        if (!anySelected) {
            Alert.alert('चेतावनी', 'कृपया कम से कम एक विकल्प चुनें।');
            return;
        }

        setIsClearing(true);
        try {
            const promises: Promise<boolean>[] = [];

            if (clearOptions.products) {
                promises.push(removeData(STORAGE_KEYS.PRODUCTS));
                promises.push(removeData(STORAGE_KEYS.CATEGORIES));
            }
            if (clearOptions.customers) {
                promises.push(removeData(STORAGE_KEYS.CUSTOMERS));
            }
            if (clearOptions.sales) {
                promises.push(removeData(STORAGE_KEYS.SALES));
            }
            if (clearOptions.expenses) {
                promises.push(removeData(STORAGE_KEYS.EXPENSES));
            }

            // Clear bakaya (outstanding balances) - reset all customer balances to 0
            if (clearOptions.bakaya && !clearOptions.customers) {
                // Only clear bakaya if not already deleting all customers
                const customers = await getData<Customer[]>(STORAGE_KEYS.CUSTOMERS);
                if (customers && customers.length > 0) {
                    const updatedCustomers = customers.map(c => ({
                        ...c,
                        outstandingBalance: 0
                    }));
                    await setData(STORAGE_KEYS.CUSTOMERS, updatedCustomers);
                }
            }

            await Promise.all(promises);
            await refreshAll();
            setShowClearDataModal(false);

            // Build success message
            const cleared: string[] = [];
            if (clearOptions.products) cleared.push('प्रोडक्ट');
            if (clearOptions.customers) cleared.push('ग्राहक');
            if (clearOptions.sales) cleared.push('बिक्री');
            if (clearOptions.expenses) cleared.push('खर्च');
            if (clearOptions.bakaya && !clearOptions.customers) cleared.push('बकाया');

            Alert.alert('सफलता', `${cleared.join(', ')} का डेटा सफलतापूर्वक साफ कर दिया गया है।`);
        } catch (error) {
            console.error('Clear data error:', error);
            Alert.alert('त्रुटि', 'कुछ गलत हो गया।');
        } finally {
            setIsClearing(false);
        }
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
                    <Text style={styles.headerTitle}>स्टोर की सेटिंग</Text>
                    <TouchableOpacity style={styles.notificationButton}>
                        <FontAwesome name="bell" size={20} color="#212121" />
                    </TouchableOpacity>
                </View>
            </SafeAreaView>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Editable Fields */}
                <View style={styles.fieldsContainer}>
                    {/* Store Name */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>दुकान का नाम</Text>
                            <Text style={styles.fieldValue}>अपना स्टोर</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Store Address */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>दुकान का पता</Text>
                            <Text style={styles.fieldValue}>शॉप नं. 12, जैन मार्केट, मुंबई</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Business Type */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>व्यवसाय का प्रकार</Text>
                            <Text style={styles.fieldValue}>खुदरा व्यापार</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* GST Details */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>जीएसटी विवरण</Text>
                            <Text style={styles.fieldValue}>27AABCU9603R1ZM</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Canvasing Settings */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>कानवासिंग सेट</Text>
                            <Text style={styles.fieldValue}>डिफ़ॉल्ट सेटिंग</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Store Rate */}
                    <TouchableOpacity style={styles.navigationCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>स्टोर की दुला</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={18} color="#757575" />
                    </TouchableOpacity>

                    {/* Language */}
                    <TouchableOpacity style={styles.navigationCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>भाषा</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={18} color="#757575" />
                    </TouchableOpacity>

                    {/* Clear Data - Danger Zone */}
                    <View style={styles.dangerSection}>
                        <Text style={styles.dangerSectionTitle}>डेटा प्रबंधन</Text>
                        <TouchableOpacity
                            style={styles.dangerCard}
                            onPress={() => setShowClearDataModal(true)}
                        >
                            <View style={styles.dangerIconContainer}>
                                <FontAwesome name="trash" size={20} color="#D32F2F" />
                            </View>
                            <View style={styles.fieldContent}>
                                <Text style={styles.dangerLabel}>सभी डेटा साफ करें</Text>
                                <Text style={styles.dangerDescription}>
                                    प्रोडक्ट, ग्राहक, बिक्री और खर्च का पूरा डेटा मिटाएं
                                </Text>
                            </View>
                            <FontAwesome name="chevron-right" size={18} color="#D32F2F" />
                        </TouchableOpacity>
                    </View>
                </View>
            </ScrollView>

            {/* Clear Data Confirmation Modal */}
            <Modal
                visible={showClearDataModal}
                transparent={true}
                animationType="fade"
                onRequestClose={() => setShowClearDataModal(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.modalIconContainer}>
                            <FontAwesome name="trash" size={40} color="#D32F2F" />
                        </View>
                        <Text style={styles.modalTitle}>डेटा साफ करें</Text>
                        <Text style={styles.modalSubtitle}>
                            जो डेटा आप मिटाना चाहते हैं उसे चुनें:
                        </Text>

                        {/* Checkbox Options */}
                        <View style={styles.checkboxContainer}>
                            {/* Select All */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={toggleAll}
                            >
                                <View style={[styles.checkbox, allSelected && styles.checkboxChecked]}>
                                    {allSelected && <FontAwesome name="check" size={12} color="#FFFFFF" />}
                                </View>
                                <Text style={styles.checkboxLabelBold}>सभी चुनें</Text>
                            </TouchableOpacity>

                            <View style={styles.checkboxDivider} />

                            {/* Products */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={() => toggleOption('products')}
                            >
                                <View style={[styles.checkbox, clearOptions.products && styles.checkboxChecked]}>
                                    {clearOptions.products && <FontAwesome name="check" size={12} color="#FFFFFF" />}
                                </View>
                                <View style={styles.checkboxContent}>
                                    <Text style={styles.checkboxLabel}>प्रोडक्ट</Text>
                                    <Text style={styles.checkboxDesc}>सभी उत्पाद और श्रेणियां</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Customers */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={() => toggleOption('customers')}
                            >
                                <View style={[styles.checkbox, clearOptions.customers && styles.checkboxChecked]}>
                                    {clearOptions.customers && <FontAwesome name="check" size={12} color="#FFFFFF" />}
                                </View>
                                <View style={styles.checkboxContent}>
                                    <Text style={styles.checkboxLabel}>ग्राहक</Text>
                                    <Text style={styles.checkboxDesc}>सभी ग्राहक जानकारी</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Sales */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={() => toggleOption('sales')}
                            >
                                <View style={[styles.checkbox, clearOptions.sales && styles.checkboxChecked]}>
                                    {clearOptions.sales && <FontAwesome name="check" size={12} color="#FFFFFF" />}
                                </View>
                                <View style={styles.checkboxContent}>
                                    <Text style={styles.checkboxLabel}>बिक्री</Text>
                                    <Text style={styles.checkboxDesc}>सभी बिक्री रिकॉर्ड</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Expenses */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={() => toggleOption('expenses')}
                            >
                                <View style={[styles.checkbox, clearOptions.expenses && styles.checkboxChecked]}>
                                    {clearOptions.expenses && <FontAwesome name="check" size={12} color="#FFFFFF" />}
                                </View>
                                <View style={styles.checkboxContent}>
                                    <Text style={styles.checkboxLabel}>खर्च</Text>
                                    <Text style={styles.checkboxDesc}>सभी खर्च रिकॉर्ड</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Bakaya (Outstanding Balance) */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={() => toggleOption('bakaya')}
                            >
                                <View style={[styles.checkbox, clearOptions.bakaya && styles.checkboxChecked]}>
                                    {clearOptions.bakaya && <FontAwesome name="check" size={12} color="#FFFFFF" />}
                                </View>
                                <View style={styles.checkboxContent}>
                                    <Text style={styles.checkboxLabel}>बकाया</Text>
                                    <Text style={styles.checkboxDesc}>सभी ग्राहकों की बकाया राशि शून्य करें</Text>
                                </View>
                            </TouchableOpacity>
                        </View>

                        <Text style={styles.warningText}>
                            ⚠️ यह क्रिया पूर्ववत नहीं की जा सकती!
                        </Text>

                        <View style={styles.modalButtons}>
                            <TouchableOpacity
                                style={styles.modalCancelButton}
                                onPress={() => setShowClearDataModal(false)}
                                disabled={isClearing}
                            >
                                <Text style={styles.modalCancelButtonText}>रद्द करें</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[
                                    styles.modalConfirmButton,
                                    isClearing && styles.modalButtonDisabled,
                                    !anySelected && styles.modalButtonDisabled
                                ]}
                                onPress={handleClearData}
                                disabled={isClearing || !anySelected}
                            >
                                {isClearing ? (
                                    <ActivityIndicator size="small" color="#FFFFFF" />
                                ) : (
                                    <Text style={styles.modalConfirmButtonText}>साफ करें</Text>
                                )}
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
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
    notificationButton: {
        padding: 8,
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 24,
    },
    fieldsContainer: {
        paddingHorizontal: 16,
        marginTop: 16,
    },
    fieldCard: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
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
    navigationCard: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
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
    fieldContent: {
        flex: 1,
    },
    fieldLabel: {
        fontSize: 16,
        color: '#212121',
        fontWeight: '500',
    },
    fieldValue: {
        fontSize: 14,
        color: '#757575',
        marginTop: 4,
    },
    editIcon: {
        padding: 8,
    },
    // Danger Section Styles
    dangerSection: {
        marginTop: 24,
        paddingTop: 16,
        borderTopWidth: 1,
        borderTopColor: '#E0E0E0',
    },
    dangerSectionTitle: {
        fontSize: 14,
        fontWeight: '600',
        color: '#757575',
        marginBottom: 12,
        textTransform: 'uppercase',
    },
    dangerCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFEBEE',
        padding: 16,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#FFCDD2',
    },
    dangerIconContainer: {
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    dangerLabel: {
        fontSize: 16,
        fontWeight: '600',
        color: '#D32F2F',
    },
    dangerDescription: {
        fontSize: 12,
        color: '#757575',
        marginTop: 2,
    },
    // Modal Styles
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: 24,
    },
    modalContent: {
        backgroundColor: '#FFFFFF',
        borderRadius: 16,
        padding: 24,
        width: '100%',
        maxWidth: 340,
        alignItems: 'center',
    },
    modalIconContainer: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: '#FFEBEE',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 16,
    },
    modalTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 12,
        textAlign: 'center',
    },
    modalMessage: {
        fontSize: 14,
        color: '#616161',
        textAlign: 'left',
        lineHeight: 22,
        marginBottom: 24,
    },
    modalButtons: {
        flexDirection: 'row',
        gap: 12,
        width: '100%',
    },
    modalCancelButton: {
        flex: 1,
        paddingVertical: 14,
        borderRadius: 10,
        backgroundColor: '#F5F5F5',
        alignItems: 'center',
    },
    modalCancelButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#616161',
    },
    modalConfirmButton: {
        flex: 1,
        paddingVertical: 14,
        borderRadius: 10,
        backgroundColor: '#D32F2F',
        alignItems: 'center',
    },
    modalButtonDisabled: {
        opacity: 0.7,
    },
    modalConfirmButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    // Checkbox Styles
    modalSubtitle: {
        fontSize: 14,
        color: '#616161',
        marginBottom: 16,
        textAlign: 'center',
    },
    checkboxContainer: {
        width: '100%',
        marginBottom: 16,
    },
    checkboxRow: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 12,
    },
    checkbox: {
        width: 22,
        height: 22,
        borderRadius: 4,
        borderWidth: 2,
        borderColor: '#BDBDBD',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    checkboxChecked: {
        backgroundColor: '#D32F2F',
        borderColor: '#D32F2F',
    },
    checkboxContent: {
        flex: 1,
    },
    checkboxLabel: {
        fontSize: 15,
        color: '#212121',
        fontWeight: '500',
    },
    checkboxLabelBold: {
        fontSize: 15,
        color: '#212121',
        fontWeight: '700',
    },
    checkboxDesc: {
        fontSize: 12,
        color: '#757575',
        marginTop: 2,
    },
    checkboxDivider: {
        height: 1,
        backgroundColor: '#E0E0E0',
        marginVertical: 8,
    },
    warningText: {
        fontSize: 13,
        color: '#D32F2F',
        fontWeight: '600',
        marginBottom: 16,
        textAlign: 'center',
    },
});
