import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, Modal, Alert, ActivityIndicator, TextInput, KeyboardAvoidingView, Platform } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { router } from 'expo-router';
import { removeData, getData, setData } from '@/services/storage';
import { useData } from '@/contexts/DataContext';
import { useSettings } from '@/contexts/settings-context';
import { useAppMode } from '@/contexts/ModeContext';
import { STORAGE_KEYS, Customer } from '@/types';

import { TransliterationInput } from '../components/ui/TransliterationInput';
import { LanguageModal } from '../components/ui/language-modal';
import { Colors } from '@/constants/theme';

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
    keyboardType?: 'default' | 'phone-pad' | 'email-address' | 'numeric';
    placeholder?: string;
}

function EditModal({ visible, title, value, onSave, onClose, keyboardType = 'default', placeholder }: EditModalProps) {
    const { t } = useTranslation();
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
            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                style={styles.editModalOverlay}
            >
                <View style={styles.editModalContent}>
                    <Text style={styles.editModalTitle}>{title}</Text>
                    {keyboardType === 'default' ? (
                        <TransliterationInput
                            style={styles.editModalInput}
                            value={inputValue}
                            onChangeText={setInputValue}
                            placeholder={placeholder}
                            placeholderTextColor={Colors.light.textTertiary}
                            autoFocus
                        />
                    ) : (
                        <TextInput
                            style={styles.editModalInput}
                            value={inputValue}
                            onChangeText={setInputValue}
                            keyboardType={keyboardType}
                            placeholder={placeholder}
                            placeholderTextColor={Colors.light.textTertiary}
                            autoFocus
                        />
                    )}
                    <View style={styles.editModalButtons}>
                        <TouchableOpacity
                            style={styles.editCancelButton}
                            onPress={onClose}
                            disabled={isSaving}
                        >
                            <Text style={styles.editCancelButtonText}>{t('common.cancel')}</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={styles.editSaveButton}
                            onPress={handleSave}
                            disabled={isSaving}
                        >
                            {isSaving ? (
                                <ActivityIndicator size="small" color={Colors.light.white} />
                            ) : (
                                <Text style={styles.editSaveButtonText}>{t('common.save')}</Text>
                            )}
                        </TouchableOpacity>
                    </View>
                </View>
            </KeyboardAvoidingView>
        </Modal>
    );
}

export default function StoreSettingsScreen() {
    const { t, i18n } = useTranslation();
    const { refreshAll } = useData();
    const { storeSettings, updateStoreSettings, isLoading: settingsLoading } = useSettings();
    const { appMode } = useAppMode();
    const isPersonalMode = appMode === 'personal';
    const [showClearDataModal, setShowClearDataModal] = useState(false);
    const [isClearing, setIsClearing] = useState(false);
    const [editField, setEditField] = useState<'name' | 'address' | 'businessType' | 'gstin' | 'canvasingRate' | 'storeRate' | null>(null);
    const [clearOptions, setClearOptions] = useState<ClearOptions>({
        products: !isPersonalMode, // Products not relevant for personal mode
        customers: true,
        sales: true,
        expenses: true,
        bakaya: true,
    });
    const [showLanguageModal, setShowLanguageModal] = useState(false);

    const toggleOption = (key: keyof ClearOptions) => {
        setClearOptions(prev => ({ ...prev, [key]: !prev[key] }));
    };

    const toggleAll = () => {
        const allSelected = Object.values(clearOptions).every(v => v);
        setClearOptions({
            products: isPersonalMode ? false : !allSelected, // Products not relevant for personal mode
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
            Alert.alert(t('common.warning'), t('storeSettings.selectAtLeastOne'));
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
            if (clearOptions.products) cleared.push(t('inventory.product'));
            if (clearOptions.customers) cleared.push(t('customers.customer'));
            if (clearOptions.sales) cleared.push(t('reports.sales'));
            if (clearOptions.expenses) cleared.push(t('reports.expenses'));
            if (clearOptions.bakaya && !clearOptions.customers) cleared.push(t('customers.outstanding'));

            Alert.alert(t('common.success'), t('storeSettings.clearSuccess', { data: cleared.join(', ') }));
        } catch (error) {
            console.error('Clear data error:', error);
            Alert.alert(t('common.error'), t('profile.updateError'));
        } finally {
            setIsClearing(false);
        }
    };

    const handleFieldSave = async (field: keyof typeof storeSettings, value: string) => {
        const success = await updateStoreSettings({ [field]: value });
        if (success) {
            Alert.alert(t('common.success'), t('profile.updateSuccess'));
        } else {
            Alert.alert(t('common.error'), t('profile.updateError'));
        }
    };

    // Helper functions for display values
    const getStoreName = () => storeSettings.name || t('profile.notSet');
    const getStoreAddress = () => storeSettings.address || t('profile.notSet');
    const getBusinessType = () => storeSettings.businessType || t('profile.notSet');
    const getGstin = () => storeSettings.gstin || t('profile.notSet');
    const getCanvasingRate = () => storeSettings.canvasingRate || t('profile.notSet');
    const getStoreRate = () => storeSettings.storeRate || t('profile.notSet');

    if (settingsLoading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color={Colors.light.primary} />
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />

            {/* Header */}
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color={Colors.light.text} />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>{t(isPersonalMode ? 'personal.appSettings' : 'storeSettings.title')}</Text>
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
                    {/* Store-specific settings - hidden in personal mode */}
                    {!isPersonalMode && (
                        <>
                            {/* Store Name */}
                            <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('name')} activeOpacity={0.7}>
                                <View style={styles.fieldContent}>
                                    <Text style={styles.fieldLabel}>{t('storeSettings.storeName')}</Text>
                                    <Text style={styles.fieldValue}>{getStoreName()}</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Store Address */}
                            <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('address')} activeOpacity={0.7}>
                                <View style={styles.fieldContent}>
                                    <Text style={styles.fieldLabel}>{t('storeSettings.storeAddress')}</Text>
                                    <Text style={styles.fieldValue}>{getStoreAddress()}</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Business Type */}
                            <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('businessType')} activeOpacity={0.7}>
                                <View style={styles.fieldContent}>
                                    <Text style={styles.fieldLabel}>{t('storeSettings.businessType')}</Text>
                                    <Text style={styles.fieldValue}>{getBusinessType()}</Text>
                                </View>
                            </TouchableOpacity>

                            {/* GST Details */}
                            <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('gstin')} activeOpacity={0.7}>
                                <View style={styles.fieldContent}>
                                    <Text style={styles.fieldLabel}>{t('storeSettings.gstDetails')}</Text>
                                    <Text style={styles.fieldValue}>{getGstin()}</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Canvasing Settings */}
                            {/* Canvasing Settings */}
                            <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('canvasingRate')} activeOpacity={0.7}>
                                <View style={styles.fieldContent}>
                                    <Text style={styles.fieldLabel}>{t('storeSettings.canvasingRate')}</Text>
                                    <Text style={styles.fieldValue}>{getCanvasingRate()}</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Store Rate */}
                            <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('storeRate')} activeOpacity={0.7}>
                                <View style={styles.fieldContent}>
                                    <Text style={styles.fieldLabel}>{t('storeSettings.storeRate')}</Text>
                                    <Text style={styles.fieldValue}>{getStoreRate()}</Text>
                                </View>
                            </TouchableOpacity>
                        </>
                    )}

                    {/* Language - shown in both modes */}
                    <TouchableOpacity style={styles.navigationCard} onPress={() => setShowLanguageModal(true)}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('storeSettings.language')}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={18} color={Colors.light.textSecondary} />
                    </TouchableOpacity>

                    {/* Profit Calculation Method - only for business mode */}
                    {!isPersonalMode && (
                        <View style={styles.settingSection}>
                            <Text style={styles.settingSectionTitle}>{t('storeSettings.profitCalculation')}</Text>
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
                                        <Text style={styles.radioLabel}>{t('storeSettings.basedOnSales')}</Text>
                                        <Text style={styles.radioDesc}>{t('storeSettings.basedOnSalesDesc')}</Text>
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
                                        <Text style={styles.radioLabel}>{t('storeSettings.basedOnReceived')}</Text>
                                        <Text style={styles.radioDesc}>{t('storeSettings.basedOnReceivedDesc')}</Text>
                                    </View>
                                </TouchableOpacity>
                            </View>
                        </View>
                    )}

                    {/* Clear Data - Danger Zone */}
                    <View style={styles.dangerSection}>
                        <Text style={styles.dangerSectionTitle}>{t('storeSettings.dataManagement')}</Text>
                        <TouchableOpacity
                            style={styles.dangerCard}
                            onPress={() => setShowClearDataModal(true)}
                        >
                            <View style={styles.dangerIconContainer}>
                                <FontAwesome name="trash" size={20} color={Colors.light.error} />
                            </View>
                            <View style={styles.fieldContent}>
                                <Text style={styles.dangerLabel}>{t('storeSettings.clearAllData')}</Text>
                                <Text style={styles.dangerDescription}>
                                    {t('storeSettings.clearDataDesc')}
                                </Text>
                            </View>
                            <FontAwesome name="chevron-right" size={18} color={Colors.light.error} />
                        </TouchableOpacity>
                    </View>
                </View>
            </ScrollView>

            {/* Edit Modals */}
            <EditModal
                visible={editField === 'name'}
                title={t('storeSettings.changeStoreName')}
                value={storeSettings.name || ''}
                onSave={(value) => handleFieldSave('name', value)}
                onClose={() => setEditField(null)}
                placeholder={t('storeSettings.enterStoreName')}
            />
            <EditModal
                visible={editField === 'address'}
                title={t('storeSettings.changeStoreAddress')}
                value={storeSettings.address || ''}
                onSave={(value) => handleFieldSave('address', value)}
                onClose={() => setEditField(null)}
                placeholder={t('storeSettings.enterAddress', { defaultValue: 'Enter address' })}
            />
            <EditModal
                visible={editField === 'businessType'}
                title={t('storeSettings.changeBusinessType')}
                value={storeSettings.businessType || ''}
                onSave={(value) => handleFieldSave('businessType', value)}
                onClose={() => setEditField(null)}
                placeholder={t('storeSettings.enterBusinessType')}
            />
            <EditModal
                visible={editField === 'gstin'}
                title={t('storeSettings.changeGst')}
                value={storeSettings.gstin || ''}
                onSave={(value) => handleFieldSave('gstin', value)}
                onClose={() => setEditField(null)}
                placeholder={t('storeSettings.enterGst')}
            />
            <EditModal
                visible={editField === 'canvasingRate'}
                title={t('storeSettings.changeCanvasingRate')}
                value={storeSettings.canvasingRate || ''}
                onSave={(value) => handleFieldSave('canvasingRate', value)}
                onClose={() => setEditField(null)}
                placeholder={t('storeSettings.enterCanvasingRate')}
                keyboardType="numeric"
            />
            <EditModal
                visible={editField === 'storeRate'}
                title={t('storeSettings.changeStoreRate')}
                value={storeSettings.storeRate || ''}
                onSave={(value) => handleFieldSave('storeRate', value)}
                onClose={() => setEditField(null)}
                placeholder={t('storeSettings.enterStoreRate')}
                keyboardType="numeric"
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
                            <FontAwesome name="trash" size={40} color={Colors.light.error} />
                        </View>
                        <Text style={styles.modalTitle}>{t('storeSettings.clearDataTitle')}</Text>
                        <Text style={styles.modalSubtitle}>
                            {t('storeSettings.clearDataSubtitle')}
                        </Text>

                        {/* Checkbox Options */}
                        <View style={styles.checkboxContainer}>
                            {/* Select All */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={toggleAll}
                            >
                                <View style={[styles.checkbox, allSelected && styles.checkboxChecked]}>
                                    {allSelected && <FontAwesome name="check" size={12} color={Colors.light.white} />}
                                </View>
                                <Text style={styles.checkboxLabelBold}>{t('storeSettings.selectAll')}</Text>
                            </TouchableOpacity>

                            <View style={styles.checkboxDivider} />

                            {/* Products - only for business mode */}
                            {!isPersonalMode && (
                                <TouchableOpacity
                                    style={styles.checkboxRow}
                                    onPress={() => toggleOption('products')}
                                >
                                    <View style={[styles.checkbox, clearOptions.products && styles.checkboxChecked]}>
                                        {clearOptions.products && <FontAwesome name="check" size={12} color={Colors.light.white} />}
                                    </View>
                                    <View style={styles.checkboxContent}>
                                        <Text style={styles.checkboxLabel}>{t('inventory.product')}</Text>
                                        <Text style={styles.checkboxDesc}>{t('storeSettings.clearDataDesc').split(',')[0]}...</Text>
                                    </View>
                                </TouchableOpacity>
                            )}

                            {/* Customers/Friends */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={() => toggleOption('customers')}
                            >
                                <View style={[styles.checkbox, clearOptions.customers && styles.checkboxChecked]}>
                                    {clearOptions.customers && <FontAwesome name="check" size={12} color={Colors.light.white} />}
                                </View>
                                <View style={styles.checkboxContent}>
                                    <Text style={styles.checkboxLabel}>{t(isPersonalMode ? 'personal.people' : 'customers.customer')}</Text>
                                    <Text style={styles.checkboxDesc}>{t('storeSettings.clearDataDesc').split(',')[1]}...</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Sales/Income */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={() => toggleOption('sales')}
                            >
                                <View style={[styles.checkbox, clearOptions.sales && styles.checkboxChecked]}>
                                    {clearOptions.sales && <FontAwesome name="check" size={12} color={Colors.light.white} />}
                                </View>
                                <View style={styles.checkboxContent}>
                                    <Text style={styles.checkboxLabel}>{t(isPersonalMode ? 'reports.income' : 'reports.sales')}</Text>
                                    <Text style={styles.checkboxDesc}>{t('storeSettings.clearDataDesc').split(',')[2]}...</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Expenses */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={() => toggleOption('expenses')}
                            >
                                <View style={[styles.checkbox, clearOptions.expenses && styles.checkboxChecked]}>
                                    {clearOptions.expenses && <FontAwesome name="check" size={12} color={Colors.light.white} />}
                                </View>
                                <View style={styles.checkboxContent}>
                                    <Text style={styles.checkboxLabel}>{t('reports.expenses')}</Text>
                                    <Text style={styles.checkboxDesc}>{t('storeSettings.clearDataDesc').split(',')[3]}...</Text>
                                </View>
                            </TouchableOpacity>

                            {/* Bakaya (Outstanding Balance/Loans) */}
                            <TouchableOpacity
                                style={styles.checkboxRow}
                                onPress={() => toggleOption('bakaya')}
                            >
                                <View style={[styles.checkbox, clearOptions.bakaya && styles.checkboxChecked]}>
                                    {clearOptions.bakaya && <FontAwesome name="check" size={12} color={Colors.light.white} />}
                                </View>
                                <View style={styles.checkboxContent}>
                                    <Text style={styles.checkboxLabel}>{t(isPersonalMode ? 'personal.loanGiven' : 'customers.outstanding')}</Text>
                                    <Text style={styles.checkboxDesc}>{t('storeSettings.clearAllData')}</Text>
                                </View>
                            </TouchableOpacity>
                        </View>

                        <Text style={styles.warningText}>
                            {t('storeSettings.warningUndo')}
                        </Text>

                        <View style={styles.modalButtons}>
                            <TouchableOpacity
                                style={styles.modalCancelButton}
                                onPress={() => setShowClearDataModal(false)}
                                disabled={isClearing}
                            >
                                <Text style={styles.modalCancelButtonText}>{t('common.cancel')}</Text>
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
                                    <ActivityIndicator size="small" color={Colors.light.white} />
                                ) : (
                                    <Text style={styles.modalConfirmButtonText}>{t('storeSettings.clear')}</Text>
                                )}
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
            <LanguageModal
                visible={showLanguageModal}
                onClose={() => setShowLanguageModal(false)}
                selectedLanguage={i18n.language}
                onSelectLanguage={() => setShowLanguageModal(false)}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.light.background,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: Colors.light.background,
    },
    headerSafeArea: {
        backgroundColor: Colors.light.white,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 16,
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
    fieldsContainer: {
        paddingHorizontal: 16,
        marginTop: 16,
    },
    fieldCard: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
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
    navigationCard: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
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
    fieldContent: {
        flex: 1,
    },
    fieldLabel: {
        fontSize: 16,
        color: Colors.light.text,
        fontWeight: '500',
    },
    fieldValue: {
        fontSize: 14,
        color: Colors.light.textSecondary,
        marginTop: 4,
    },
    // Danger Section Styles
    dangerSection: {
        marginTop: 24,
        paddingTop: 16,
        borderTopWidth: 1,
        borderTopColor: Colors.light.border,
    },
    dangerSectionTitle: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.textSecondary,
        marginBottom: 12,
        textTransform: 'uppercase',
    },
    dangerCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: Colors.light.errorLight,
        padding: 16,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: Colors.light.border, // Keeping this as it's a specific shade not in MAIN list but is fine or I can add to theme
    },
    dangerIconContainer: {
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: Colors.light.white,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    dangerLabel: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.error,
    },
    dangerDescription: {
        fontSize: 12,
        color: Colors.light.textSecondary,
        marginTop: 2,
    },
    // Edit Modal Styles
    editModalOverlay: {
        flex: 1,
        backgroundColor: Colors.light.overlay,
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 24,
    },
    editModalContent: {
        backgroundColor: Colors.light.white,
        borderRadius: 16,
        padding: 24,
        width: '100%',
        maxWidth: 340,
    },
    editModalTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
        marginBottom: 16,
        textAlign: 'center',
    },
    editModalInput: {
        borderWidth: 1,
        borderColor: Colors.light.border,
        borderRadius: 8,
        padding: 12,
        fontSize: 16,
        color: Colors.light.text,
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
        borderColor: Colors.light.border,
        alignItems: 'center',
    },
    editCancelButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.textSecondary,
    },
    editSaveButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        backgroundColor: Colors.light.primary,
        alignItems: 'center',
    },
    editSaveButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.white,
    },
    // Clear Data Modal Styles
    modalOverlay: {
        flex: 1,
        backgroundColor: Colors.light.overlay,
        justifyContent: 'center',
        alignItems: 'center',
        padding: 24,
    },
    modalContent: {
        backgroundColor: Colors.light.white,
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
        backgroundColor: Colors.light.errorLight,
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 16,
    },
    modalTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: Colors.light.text,
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
        backgroundColor: Colors.light.background,
        alignItems: 'center',
    },
    modalCancelButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.textSecondary,
    },
    modalConfirmButton: {
        flex: 1,
        paddingVertical: 14,
        borderRadius: 10,
        backgroundColor: Colors.light.error,
        alignItems: 'center',
    },
    modalButtonDisabled: {
        opacity: 0.7,
    },
    modalConfirmButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.white,
    },
    // Checkbox Styles
    modalSubtitle: {
        fontSize: 14,
        color: Colors.light.textSecondary,
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
        borderColor: Colors.light.border,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    checkboxChecked: {
        backgroundColor: Colors.light.error,
        borderColor: Colors.light.error,
    },
    checkboxContent: {
        flex: 1,
    },
    checkboxLabel: {
        fontSize: 15,
        color: Colors.light.text,
        fontWeight: '500',
    },
    checkboxLabelBold: {
        fontSize: 15,
        color: Colors.light.text,
        fontWeight: '700',
    },
    checkboxDesc: {
        fontSize: 12,
        color: Colors.light.textSecondary,
        marginTop: 2,
    },
    checkboxDivider: {
        height: 1,
        backgroundColor: Colors.light.border,
        marginVertical: 8,
    },
    warningText: {
        fontSize: 13,
        color: Colors.light.error,
        fontWeight: '600',
        marginBottom: 16,
        textAlign: 'center',
    },
    // Profit Calculation Method Styles
    settingSection: {
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
    settingSectionTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.text,
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
        backgroundColor: Colors.light.background,
    },
    radioCircle: {
        width: 22,
        height: 22,
        borderRadius: 11,
        borderWidth: 2,
        borderColor: Colors.light.border,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
        marginTop: 2,
    },
    radioCircleSelected: {
        borderColor: Colors.light.primary,
    },
    radioInner: {
        width: 12,
        height: 12,
        borderRadius: 6,
        backgroundColor: Colors.light.primary,
    },
    radioContent: {
        flex: 1,
    },
    radioLabel: {
        fontSize: 15,
        fontWeight: '500',
        color: Colors.light.text,
    },
    radioDesc: {
        fontSize: 12,
        color: Colors.light.textSecondary,
        marginTop: 2,
    },
});
