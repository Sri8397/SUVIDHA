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
import { useTranslation } from 'react-i18next';

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
            Alert.alert(t('common.error'), t('forms.customer.enterName'));
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
                Alert.alert(t('common.success'), t('forms.customer.updated'));
            } else {
                await addCustomer(customerData);
                Alert.alert(t('common.success'), t('forms.customer.added'));
            }

            resetForm();
            onClose();
        } catch (error) {
            console.error('Save customer error:', error);
            Alert.alert(t('common.error'), t('forms.customer.saveError'));
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
                        <FontAwesome name="times" size={20} color="#757575" />
                    </TouchableOpacity>
                    <Text style={styles.title}>
                        {editCustomer ? t('forms.customer.editTitle') : t('forms.customer.addTitle')}
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
                            <FontAwesome name="user" size={48} color="#2E7D32" />
                        </View>
                    </View>

                    {/* Customer Name */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.customer.name')} *</Text>
                        <TransliterationInput
                            style={styles.input}
                            value={name}
                            onChangeText={setName}
                            placeholder={t('forms.customer.namePlaceholder')}
                            placeholderTextColor="#9E9E9E"
                        />
                    </View>

                    {/* Phone */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.customer.phone')} *</Text>
                        <TextInput
                            style={styles.input}
                            value={phone}
                            onChangeText={setPhone}
                            placeholder={t('forms.customer.phonePlaceholder')}
                            placeholderTextColor="#9E9E9E"
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
                            placeholderTextColor="#9E9E9E"
                            multiline

                            numberOfLines={3}
                            textAlignVertical="top"
                        />
                    </View>

                    {/* Submit Button */}
                    <TouchableOpacity
                        style={[styles.submitButton, isLoading && styles.submitButtonDisabled]}
                        onPress={handleSubmit}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <Text style={styles.submitButtonText}>
                                {editCustomer ? t('common.save') : t('customers.addCustomer')}
                            </Text>
                        )}
                    </TouchableOpacity>
                </ScrollView>
            </KeyboardAvoidingView>
        </Modal>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
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
        color: '#212121',
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
        backgroundColor: '#E8F5E9',
        alignItems: 'center',
        justifyContent: 'center',
    },
    inputGroup: {
        marginBottom: 16,
    },
    label: {
        fontSize: 14,
        fontWeight: '600',
        color: '#424242',
        marginBottom: 8,
    },
    input: {
        borderWidth: 1,
        borderColor: '#E0E0E0',
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 14,
        fontSize: 16,
        color: '#212121',
        backgroundColor: '#FAFAFA',
    },
    textArea: {
        height: 100,
        paddingTop: 14,
    },
    submitButton: {
        backgroundColor: '#2E7D32',
        borderRadius: 12,
        paddingVertical: 16,
        alignItems: 'center',
        marginTop: 24,
        marginBottom: 32,
    },
    submitButtonDisabled: {
        opacity: 0.7,
    },
    submitButtonText: {
        fontSize: 16,
        fontWeight: '700',
        color: '#FFFFFF',
    },
});
