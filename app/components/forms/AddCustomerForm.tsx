// ============================================
// Add Customer Form - Modal Form Component
// ============================================

import React, { useState, useEffect } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    Modal,
    ScrollView,
    StyleSheet,
    Alert,
    ActivityIndicator,
    KeyboardAvoidingView,
    Platform,
} from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { TransliterationInput } from '../ui/TransliterationInput';
import { useData } from '@/contexts/DataContext';
import { useAppMode } from '@/contexts/ModeContext';
import { useTranslation } from 'react-i18next';
import { Colors } from '@/constants/theme';

interface AddCustomerFormProps {
    visible: boolean;
    onClose: () => void;
    editCustomer?: {
        id: string;
        name: string;
        phone: string;
        address?: string;
    };
}

export function AddCustomerForm({ visible, onClose, editCustomer }: AddCustomerFormProps) {
    const { t } = useTranslation();
    const { addCustomer, updateCustomer } = useData();
    const { appMode } = useAppMode();
    const isPersonalMode = appMode === 'personal';
    const [isLoading, setIsLoading] = useState(false);

    // Form state
    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [address, setAddress] = useState('');

    // Sync form state when editCustomer changes or modal opens
    useEffect(() => {
        if (visible) {
            if (editCustomer) {
                setName(editCustomer.name || '');
                setPhone(editCustomer.phone || '');
                setAddress(editCustomer.address || '');
            } else {
                // Reset form for new customer
                setName('');
                setPhone('');
                setAddress('');
            }
        }
    }, [visible, editCustomer]);

    const resetForm = () => {
        setName('');
        setPhone('');
        setAddress('');
    };

    const handleSubmit = async () => {
        // Validation
        if (!name.trim()) {
            Alert.alert(t('common.error'), t(isPersonalMode ? 'personal.friend.name' : 'forms.customer.enterName'));
            return;
        }
        if (!phone.trim() || phone.length < 10) {
            Alert.alert(t('common.error'), t('forms.phoneValidation', 'Enter valid phone number (10 digits)'));
            return;
        }

        setIsLoading(true);
        try {
            const customerData = {
                name: name.trim(),
                phone: phone.trim(),
                address: address.trim() || undefined,
            };

            if (editCustomer) {
                await updateCustomer(editCustomer.id, customerData);
                Alert.alert(t('common.success'), t(isPersonalMode ? 'personal.friend.added' : 'forms.customer.updated'));
            } else {
                await addCustomer(customerData);
                Alert.alert(t('common.success'), t(isPersonalMode ? 'personal.friend.added' : 'forms.customer.added'));
            }

            resetForm();
            onClose();
        } catch (error) {
            console.error('Save customer error:', error);
            Alert.alert(t('common.error'), t(isPersonalMode ? 'personal.friend.saveError' : 'forms.customer.saveError'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        if (!editCustomer) resetForm();
        onClose();
    };

    return (
        <Modal
            visible={visible}
            animationType="slide"
            presentationStyle="pageSheet"
            onRequestClose={handleClose}
        >
            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                style={styles.container}
            >
                {/* Header */}
                <View style={styles.header}>
                    <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
                        <FontAwesome name="times" size={20} color={Colors.light.textSecondary} />
                    </TouchableOpacity>
                    <Text style={styles.title}>
                        {editCustomer
                            ? t(isPersonalMode ? 'personal.friend.title' : 'forms.customer.editTitle')
                            : t(isPersonalMode ? 'personal.friend.title' : 'forms.customer.addTitle')}
                    </Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView
                    style={styles.form}
                    showsVerticalScrollIndicator={false}
                    keyboardShouldPersistTaps="handled"
                >
                    {/* Customer Icon */}
                    <View style={styles.iconSection}>
                        <View style={styles.customerIcon}>
                            <FontAwesome name="user" size={48} color={Colors.light.primary} />
                        </View>
                    </View>

                    {/* Customer/Friend Name */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t(isPersonalMode ? 'personal.friend.name' : 'forms.customer.name')} *</Text>
                        <TransliterationInput
                            style={styles.input}
                            value={name}
                            onChangeText={setName}
                            placeholder={t(isPersonalMode ? 'personal.friend.namePlaceholder' : 'forms.customer.namePlaceholder')}
                            placeholderTextColor={Colors.light.textTertiary}
                        />
                    </View>

                    {/* Phone */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t(isPersonalMode ? 'personal.friend.phone' : 'forms.customer.phone')} *</Text>
                        <TextInput
                            style={styles.input}
                            value={phone}
                            onChangeText={setPhone}
                            placeholder={t(isPersonalMode ? 'personal.friend.phonePlaceholder' : 'forms.customer.phonePlaceholder')}
                            placeholderTextColor={Colors.light.textTertiary}
                            keyboardType="phone-pad"
                            maxLength={10}
                        />
                    </View>

                    {/* Address */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.customer.address')}</Text>
                        <TransliterationInput
                            style={[styles.input, styles.textArea]}
                            value={address}
                            onChangeText={setAddress}
                            placeholder={t('forms.customer.addressPlaceholder')}
                            placeholderTextColor={Colors.light.textTertiary}
                            multiline

                            numberOfLines={3}
                            textAlignVertical="top"
                        />
                    </View>
                </ScrollView>

                {/* Submit Button - Fixed at Bottom */}
                <View style={styles.footer}>
                    <TouchableOpacity
                        style={[styles.submitButton, isLoading && styles.submitButtonDisabled]}
                        onPress={handleSubmit}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <ActivityIndicator color={Colors.light.white} />
                        ) : (
                            <Text style={styles.submitButtonText}>
                                {editCustomer ? t('common.save') : t(isPersonalMode ? 'personal.friend.title' : 'customers.addCustomer')}
                            </Text>
                        )}
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
        </Modal>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.light.white,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.border,
    },
    closeButton: {
        width: 40,
        height: 40,
        alignItems: 'center',
        justifyContent: 'center',
    },
    title: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
    },
    form: {
        flex: 1,
        padding: 16,
    },
    iconSection: {
        alignItems: 'center',
        marginBottom: 24,
    },
    customerIcon: {
        width: 100,
        height: 100,
        borderRadius: 50,
        backgroundColor: Colors.light.primaryLight,
        alignItems: 'center',
        justifyContent: 'center',
    },
    inputGroup: {
        marginBottom: 16,
    },
    label: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.text,
        marginBottom: 8,
    },
    input: {
        borderWidth: 1,
        borderColor: Colors.light.border,
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 14,
        fontSize: 16,
        color: Colors.light.text,
        backgroundColor: Colors.light.background,
    },
    textArea: {
        height: 100,
        paddingTop: 14,
    },
    footer: {
        padding: 16,
        paddingBottom: 32,
        backgroundColor: Colors.light.white,
        borderTopWidth: 1,
        borderTopColor: Colors.light.border,
    },
    submitButton: {
        backgroundColor: Colors.light.primary,
        borderRadius: 12,
        paddingVertical: 16,
        alignItems: 'center',
    },
    submitButtonDisabled: {
        opacity: 0.7,
    },
    submitButtonText: {
        fontSize: 16,
        fontWeight: '700',
        color: Colors.light.white,
    },
});
