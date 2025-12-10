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

interface ReceivePaymentFormProps {
    visible: boolean;
    onClose: () => void;
    customer?: Customer;
}

export function ReceivePaymentForm({ visible, onClose, customer }: ReceivePaymentFormProps) {
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
            Alert.alert('त्रुटि', 'सही राशि दर्ज करें');
            return;
        }

        if (paymentAmount > customer.outstanding) {
            Alert.alert('त्रुटि', 'राशि बकाया से अधिक नहीं हो सकती');
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

            Alert.alert('सफलता', `₹${paymentAmount} प्राप्त हुए। शेष बकाया: ₹${newOutstanding}`);
            onClose();
        } catch (error) {
            console.error('Payment error:', error);
            Alert.alert('त्रुटि', 'पेमेंट सेव करने में समस्या हुई');
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
                        <FontAwesome name="times" size={20} color="#757575" />
                    </TouchableOpacity>
                    <Text style={styles.title}>बकाया भुगतान लें</Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView style={styles.form} showsVerticalScrollIndicator={false}>
                    {/* Customer Info Card - Styled like Selector */}
                    <View style={styles.customerCard}>
                        <View style={styles.customerIcon}>
                            <FontAwesome name="user" size={20} color="#2E7D32" />
                        </View>
                        <View style={styles.customerInfo}>
                            <Text style={styles.customerLabel}>ग्राहक</Text>
                            <Text style={styles.customerName}>{customer?.name}</Text>
                        </View>
                        <View style={styles.outstandingBadge}>
                            <Text style={styles.outstandingLabel}>बकाया</Text>
                            <Text style={styles.outstandingValue}>₹{customer?.outstanding.toLocaleString('en-IN')}</Text>
                        </View>
                    </View>

                    {/* Received Amount Input */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>प्राप्त राशि (₹)</Text>
                        <TextInput
                            style={styles.input}
                            value={amount}
                            onChangeText={setAmount}
                            placeholder="राशि दर्ज करें"
                            placeholderTextColor="#9E9E9E"
                            keyboardType="numeric"
                            autoFocus
                        />
                    </View>

                    {/* Payment Method */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>भुगतान का तरीका</Text>
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
                                        {method === 'cash' ? 'Cash' :
                                            method === 'upi' ? 'UPI' : 'Card'}
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
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <Text style={styles.submitButtonText}>भुगतान प्राप्त करें</Text>
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
        backgroundColor: '#F5F5F5',
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
    customerCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        padding: 16,
        borderRadius: 12,
        marginBottom: 24,
    },
    customerIcon: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: '#E8F5E9',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    customerInfo: {
        flex: 1,
    },
    customerLabel: {
        fontSize: 12,
        color: '#757575',
    },
    customerName: {
        fontSize: 16,
        fontWeight: '600',
        color: '#212121',
    },
    outstandingBadge: {
        alignItems: 'flex-end',
    },
    outstandingLabel: {
        fontSize: 12,
        color: '#757575',
    },
    outstandingValue: {
        fontSize: 16,
        fontWeight: '700',
        color: '#D32F2F',
    },
    inputGroup: {
        marginBottom: 24,
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
        backgroundColor: '#FFFFFF',
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
        borderColor: '#E0E0E0',
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
    },
    paymentChipActive: {
        backgroundColor: '#2E7D32',
        borderColor: '#2E7D32',
    },
    paymentChipText: {
        fontSize: 14,
        fontWeight: '500',
        color: '#616161',
    },
    paymentChipTextActive: {
        color: '#FFFFFF',
    },
    submitButton: {
        backgroundColor: '#2E7D32',
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
        color: '#FFFFFF',
    },
});
