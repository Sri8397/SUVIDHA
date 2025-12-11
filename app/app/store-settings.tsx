import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, Modal, Alert, ActivityIndicator, TextInput } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { router } from 'expo-router';
import { removeData } from '@/services/storage';
import { useData } from '@/contexts/DataContext';
import { useSettings } from '@/contexts/settings-context';
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

// Edit Modal Component
interface EditModalProps {
    visible: boolean;
    title: string;
    value: string;
    onSave: (value: string) => void;
    onClose: () => void;
    keyboardType?: 'default' | 'phone-pad' | 'email-address';
    placeholder?: string;
}

function EditModal({ visible, title, value, onSave, onClose, keyboardType = 'default', placeholder }: EditModalProps) {
    const [inputValue, setInputValue] = useState(value);
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        setInputValue(value);
    }, [value, visible]);

    const handleSave = async () => {
        setIsSaving(true);
        await onSave(inputValue);
        setIsSaving(false);
        onClose();
    };

    return (
        <Modal
            visible={visible}
            transparent
            animationType="fade"
            onRequestClose={onClose}
        >
            <View style={styles.editModalOverlay}>
                <View style={styles.editModalContent}>
                    <Text style={styles.editModalTitle}>{title}</Text>
                    <TextInput
                        style={styles.editModalInput}
                        value={inputValue}
                        onChangeText={setInputValue}
                        keyboardType={keyboardType}
                        placeholder={placeholder}
                        placeholderTextColor="#9E9E9E"
                        autoFocus
                    />
                    <View style={styles.editModalButtons}>
                        <TouchableOpacity
                            style={styles.editCancelButton}
                            onPress={onClose}
                            disabled={isSaving}
                        >
                            <Text style={styles.editCancelButtonText}>रद्द करें</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={styles.editSaveButton}
                            onPress={handleSave}
                            disabled={isSaving}
                        >
                            {isSaving ? (
                                <ActivityIndicator size="small" color="#FFFFFF" />
                            ) : (
                                <Text style={styles.editSaveButtonText}>सेव करें</Text>
                            )}
                        </TouchableOpacity>
                    </View>
                </View>
            </View>
        </Modal>
    );
}

export default function StoreSettingsScreen() {
    const { refreshAll } = useData();
    const { storeSettings, updateStoreSettings, isLoading: settingsLoading } = useSettings();
    const [showClearDataModal, setShowClearDataModal] = useState(false);
    const [isClearing, setIsClearing] = useState(false);
    const [editField, setEditField] = useState<'name' | 'address' | 'businessType' | 'gstin' | null>(null);
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

    const handleFieldSave = async (field: keyof typeof storeSettings, value: string) => {
        const success = await updateStoreSettings({ [field]: value });
        if (success) {
            Alert.alert('सफलता', 'जानकारी सफलतापूर्वक अपडेट हो गई।');
        } else {
            Alert.alert('त्रुटि', 'कुछ गलत हो गया।');
        }
    };

    // Helper functions for display values
    const getStoreName = () => storeSettings.name || 'सेट नहीं है';
    const getStoreAddress = () => storeSettings.address || 'सेट नहीं है';
    const getBusinessType = () => storeSettings.businessType || 'सेट नहीं है';
    const getGstin = () => storeSettings.gstin || 'सेट नहीं है';

    if (settingsLoading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#2E7D32" />
            </View>
        );
    }

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
                    <View style={styles.backButton} />
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
                    <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('name')} activeOpacity={0.7}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>दुकान का नाम</Text>
                            <Text style={styles.fieldValue}>{getStoreName()}</Text>
                        </View>
                    </TouchableOpacity>

                    {/* Store Address */}
                    <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('address')} activeOpacity={0.7}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>दुकान का पता</Text>
                            <Text style={styles.fieldValue}>{getStoreAddress()}</Text>
                        </View>
                    </TouchableOpacity>

                    {/* Business Type */}
                    <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('businessType')} activeOpacity={0.7}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>व्यवसाय का प्रकार</Text>
                            <Text style={styles.fieldValue}>{getBusinessType()}</Text>
                        </View>
                    </TouchableOpacity>

                    {/* GST Details */}
                    <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('gstin')} activeOpacity={0.7}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>जीएसटी विवरण</Text>
                            <Text style={styles.fieldValue}>{getGstin()}</Text>
                        </View>
                    </TouchableOpacity>

                    {/* Canvasing Settings */}
                    <TouchableOpacity style={styles.fieldCard} activeOpacity={0.7}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>कानवासिंग सेट</Text>
                            <Text style={styles.fieldValue}>डिफ़ॉल्ट सेटिंग</Text>
                        </View>
                    </TouchableOpacity>

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

                    {/* Profit Calculation Method */}
                    <View style={styles.settingSection}>
                        <Text style={styles.settingSectionTitle}>लाभ की गणना</Text>
                        <View style={styles.radioGroup}>
                            <TouchableOpacity
                                style={styles.radioOption}
                                onPress={() => updateStoreSettings({ profitCalculationMethod: 'accrual' })}
                            >
                                <View style={[
                                    styles.radioCircle,
                                    storeSettings.profitCalculationMethod === 'accrual' && styles.radioCircleSelected
                                ]}>
                                    {storeSettings.profitCalculationMethod === 'accrual' && (
                                        <View style={styles.radioInner} />
                                    )}
                                </View>
                                <View style={styles.radioContent}>
                                    <Text style={styles.radioLabel}>बिक्री पर आधारित</Text>
                                    <Text style={styles.radioDesc}>पूरी बिक्री राशि गिनें (भले ही अभी पूरा भुगतान न मिला हो)</Text>
                                </View>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.radioOption}
                                onPress={() => updateStoreSettings({ profitCalculationMethod: 'cash' })}
                            >
                                <View style={[
                                    styles.radioCircle,
                                    storeSettings.profitCalculationMethod === 'cash' && styles.radioCircleSelected
                                ]}>
                                    {storeSettings.profitCalculationMethod === 'cash' && (
                                        <View style={styles.radioInner} />
                                    )}
                                </View>
                                <View style={styles.radioContent}>
                                    <Text style={styles.radioLabel}>प्राप्त राशि पर आधारित</Text>
                                    <Text style={styles.radioDesc}>केवल जो पैसा मिला है वही गिनें</Text>
                                </View>
                            </TouchableOpacity>
                        </View>
                    </View>

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

            {/* Edit Modals */}
            <EditModal
                visible={editField === 'name'}
                title="दुकान का नाम बदलें"
                value={storeSettings.name || ''}
                onSave={(value) => handleFieldSave('name', value)}
                onClose={() => setEditField(null)}
                placeholder="दुकान का नाम दर्ज करें"
            />
            <EditModal
                visible={editField === 'address'}
                title="दुकान का पता बदलें"
                value={storeSettings.address || ''}
                onSave={(value) => handleFieldSave('address', value)}
                onClose={() => setEditField(null)}
                placeholder="पता दर्ज करें"
            />
            <EditModal
                visible={editField === 'businessType'}
                title="व्यवसाय का प्रकार बदलें"
                value={storeSettings.businessType || ''}
                onSave={(value) => handleFieldSave('businessType', value)}
                onClose={() => setEditField(null)}
                placeholder="व्यवसाय का प्रकार दर्ज करें"
            />
            <EditModal
                visible={editField === 'gstin'}
                title="जीएसटी विवरण बदलें"
                value={storeSettings.gstin || ''}
                onSave={(value) => handleFieldSave('gstin', value)}
                onClose={() => setEditField(null)}
                placeholder="जीएसटी नंबर दर्ज करें"
            />

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
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#F5F5F5',
    },
    headerSafeArea: {
        backgroundColor: '#FFFFFF',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 16,
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
    // Edit Modal Styles
    editModalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 24,
    },
    editModalContent: {
        backgroundColor: '#FFFFFF',
        borderRadius: 16,
        padding: 24,
        width: '100%',
        maxWidth: 340,
    },
    editModalTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 16,
        textAlign: 'center',
    },
    editModalInput: {
        borderWidth: 1,
        borderColor: '#E0E0E0',
        borderRadius: 8,
        padding: 12,
        fontSize: 16,
        color: '#212121',
        marginBottom: 20,
    },
    editModalButtons: {
        flexDirection: 'row',
        gap: 12,
    },
    editCancelButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: '#E0E0E0',
        alignItems: 'center',
    },
    editCancelButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#757575',
    },
    editSaveButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        backgroundColor: '#2E7D32',
        alignItems: 'center',
    },
    editSaveButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    // Clear Data Modal Styles
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
    // Profit Calculation Method Styles
    settingSection: {
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
    settingSectionTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#212121',
        marginBottom: 12,
    },
    radioGroup: {
        gap: 12,
    },
    radioOption: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        padding: 12,
        borderRadius: 8,
        backgroundColor: '#F9F9F9',
    },
    radioCircle: {
        width: 22,
        height: 22,
        borderRadius: 11,
        borderWidth: 2,
        borderColor: '#BDBDBD',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
        marginTop: 2,
    },
    radioCircleSelected: {
        borderColor: '#2E7D32',
    },
    radioInner: {
        width: 12,
        height: 12,
        borderRadius: 6,
        backgroundColor: '#2E7D32',
    },
    radioContent: {
        flex: 1,
    },
    radioLabel: {
        fontSize: 15,
        fontWeight: '500',
        color: '#212121',
    },
    radioDesc: {
        fontSize: 12,
        color: '#757575',
        marginTop: 2,
    },
});
