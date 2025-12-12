// ============================================
// Receive Payment Form - Modal Form Component
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
} from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useData } from '@/contexts/DataContext';
import { Customer } from '@/types';
import { useTranslation } from 'react-i18next';
import { Colors } from '@/constants/theme';

interface ReceivePaymentFormProps {
    visible: boolean;
    onClose: () => void;
    customer?: Customer;
}

export function ReceivePaymentForm({ visible, onClose, customer }: ReceivePaymentFormProps) {
    const { t } = useTranslation();
    const { updateCustomer, createSale } = useData();
    const [isLoading, setIsLoading] = useState(false);
    const [amount, setAmount] = useState('');
    const [paymentMethod, setPaymentMethod] = useState<'cash' | 'upi' | 'card'>('cash');

    useEffect(() => {
        if (visible && customer) {
            setAmount(customer.outstanding.toString());
        } else {
            setAmount('');
            setPaymentMethod('cash');
        }
    }, [visible, customer]);

    const handleSubmit = async () => {
        if (!customer) return;

        const paymentAmount = parseFloat(amount);
        if (isNaN(paymentAmount) || paymentAmount <= 0) {
            Alert.alert(t('common.error'), t('forms.payment.enterValidAmount'));
            return;
        }

        if (paymentAmount > customer.outstanding) {
            Alert.alert(t('common.error'), t('forms.payment.exceedsOutstanding'));
            return;
        }

        setIsLoading(true);
        try {
            const today = new Date().toISOString().split('T')[0];
            const newOutstanding = customer.outstanding - paymentAmount;

            // Create a BAKAYA_PAYMENT sale record for transaction history
            await createSale({
                saleType: 'BAKAYA_PAYMENT',
                customerId: customer.id,
                items: [], // No items for bakaya payment
                subtotal: paymentAmount,
                discount: 0,
                total: paymentAmount,
                amountPaid: paymentAmount,
                paymentStatus: 'paid',
                paymentMethod,
                date: today,
            });

            // Update customer's outstanding balance
            await updateCustomer(customer.id, { outstanding: newOutstanding });

            Alert.alert(t('common.success'), t('forms.payment.receivedSuccess', { amount: paymentAmount, remaining: newOutstanding }));
            onClose();
        } catch (error) {
            console.error('Payment error:', error);
            Alert.alert(t('common.error'), t('forms.payment.saveError'));
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Modal
            visible={visible}
            animationType="slide"
            presentationStyle="pageSheet"
            onRequestClose={onClose}
        >
            <View style={styles.container}>
                {/* Header */}
                <View style={styles.header}>
                    <TouchableOpacity onPress={onClose} style={styles.closeButton}>
                        <FontAwesome name="times" size={20} color={Colors.light.textSecondary} />
                    </TouchableOpacity>
                    <Text style={styles.title}>{t('forms.payment.title')}</Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView style={styles.form} showsVerticalScrollIndicator={false}>
                    {/* Customer Info Card - Styled like Selector */}
                    <View style={styles.customerCard}>
                        <View style={styles.customerIcon}>
                            <FontAwesome name="user" size={20} color={Colors.light.primary} />
                        </View>
                        <View style={styles.customerInfo}>
                            <Text style={styles.customerLabel}>{t('forms.payment.customer')}</Text>
                            <Text style={styles.customerName}>{customer?.name}</Text>
                        </View>
                        <View style={styles.outstandingBadge}>
                            <Text style={styles.outstandingLabel}>{t('forms.payment.outstanding')}</Text>
                            <Text style={styles.outstandingValue}>₹{customer?.outstanding.toLocaleString('en-IN')}</Text>
                        </View>
                    </View>

                    {/* Received Amount Input */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.payment.receivedAmount')} (₹)</Text>
                        <TextInput
                            style={styles.input}
                            value={amount}
                            onChangeText={setAmount}
                            placeholder={t('forms.payment.enterAmount')}
                            placeholderTextColor={Colors.light.textTertiary}
                            keyboardType="numeric"
                            autoFocus
                        />
                    </View>

                    {/* Payment Method */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.payment.paymentMethod')}</Text>
                        <View style={styles.paymentMethods}>
                            {(['cash', 'upi', 'card'] as const).map((method) => (
                                <TouchableOpacity
                                    key={method}
                                    style={[
                                        styles.paymentChip,
                                        paymentMethod === method && styles.paymentChipActive,
                                    ]}
                                    onPress={() => setPaymentMethod(method)}
                                >
                                    <Text style={[
                                        styles.paymentChipText,
                                        paymentMethod === method && styles.paymentChipTextActive,
                                    ]}>
                                        {method === 'cash' ? t('forms.payment.methods.cash') :
                                            method === 'upi' ? t('forms.payment.methods.upi') : t('forms.payment.methods.card')}
                                    </Text>
                                </TouchableOpacity>
                            ))}
                        </View>
                    </View>

                    {/* Submit Button */}
                    <TouchableOpacity
                        style={[styles.submitButton, isLoading && styles.submitButtonDisabled]}
                        onPress={handleSubmit}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <ActivityIndicator color={Colors.light.white} />
                        ) : (
                            <Text style={styles.submitButtonText}>{t('forms.payment.receivePayment')}</Text>
                        )}
                    </TouchableOpacity>
                </ScrollView>
            </View>
        </Modal>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.light.background,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        backgroundColor: Colors.light.card,
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
    customerCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: Colors.light.card,
        padding: 16,
        borderRadius: 12,
        marginBottom: 24,
    },
    customerIcon: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: Colors.light.primaryLight,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    customerInfo: {
        flex: 1,
    },
    customerLabel: {
        fontSize: 12,
        color: Colors.light.textSecondary,
    },
    customerName: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.text,
    },
    outstandingBadge: {
        alignItems: 'flex-end',
    },
    outstandingLabel: {
        fontSize: 12,
        color: Colors.light.textSecondary,
    },
    outstandingValue: {
        fontSize: 16,
        fontWeight: '700',
        color: Colors.light.error,
    },
    inputGroup: {
        marginBottom: 24,
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
        backgroundColor: Colors.light.card,
    },
    paymentMethods: {
        flexDirection: 'row',
        gap: 8,
    },
    paymentChip: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: Colors.light.border,
        backgroundColor: Colors.light.card,
        alignItems: 'center',
    },
    paymentChipActive: {
        backgroundColor: Colors.light.primary,
        borderColor: Colors.light.primary,
    },
    paymentChipText: {
        fontSize: 14,
        fontWeight: '500',
        color: Colors.light.textSecondary,
    },
    paymentChipTextActive: {
        color: Colors.light.white,
    },
    submitButton: {
        backgroundColor: Colors.light.primary,
        borderRadius: 12,
        paddingVertical: 16,
        alignItems: 'center',
        marginTop: 24,
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
