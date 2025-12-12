// ============================================
// Add Loan Form - For Personal Finance Mode
// Requires selecting a friend before recording a loan
// ============================================

import React, { useState, useMemo } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    Modal,
    StyleSheet,
    Alert,
    ScrollView,
    ActivityIndicator,
    KeyboardAvoidingView,
    Platform,
} from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useData } from '@/contexts/DataContext';
import { Customer } from '@/types';
import { useTranslation } from 'react-i18next';

interface AddLoanFormProps {
    visible: boolean;
    onClose: () => void;
    onAddPerson?: () => void; // Callback to open AddCustomerForm
}

type LoanType = 'give' | 'receive';

export function AddLoanForm({ visible, onClose, onAddPerson }: AddLoanFormProps) {
    const { t } = useTranslation();
    const { customers, updateCustomer, createSale } = useData();
    const [isLoading, setIsLoading] = useState(false);

    // Form state
    const [selectedFriend, setSelectedFriend] = useState<Customer | null>(null);
    const [loanType, setLoanType] = useState<LoanType>('give');
    const [amount, setAmount] = useState('');
    const [description, setDescription] = useState('');
    const [showFriendPicker, setShowFriendPicker] = useState(false);

    const hasFriends = customers.length > 0;

    const resetForm = () => {
        setSelectedFriend(null);
        setLoanType('give');
        setAmount('');
        setDescription('');
    };

    const handleSubmit = async () => {
        // Validation
        if (!selectedFriend) {
            Alert.alert(t('common.error'), t('personal.loan.selectFriend'));
            return;
        }

        if (!amount || isNaN(Number(amount)) || Number(amount) <= 0) {
            Alert.alert(t('common.error'), t('forms.amountRequired'));
            return;
        }

        setIsLoading(true);
        try {
            const today = new Date().toISOString().split('T')[0];
            const loanAmount = Number(amount);

            // Update friend's outstanding balance
            // Give = add to outstanding (they owe us)
            // Receive = subtract from outstanding (we paid back or they gave us)
            const outstandingChange = loanType === 'give' ? loanAmount : -loanAmount;
            const newOutstanding = Math.max(0, selectedFriend.outstanding + outstandingChange);

            await updateCustomer(selectedFriend.id, { outstanding: newOutstanding });

            // Record as a sale transaction for history tracking
            await createSale({
                saleType: 'BAKAYA_PAYMENT', // Reuse bakaya payment type for loan tracking
                customerId: selectedFriend.id,
                items: [],
                subtotal: loanAmount,
                discount: 0,
                total: loanAmount,
                amountPaid: loanType === 'receive' ? loanAmount : 0,
                paymentStatus: loanType === 'receive' ? 'paid' : 'pending',
                paymentMethod: 'cash',
                date: today,
            });

            Alert.alert(t('common.success'), t('personal.loan.loanRecorded'));
            resetForm();
            onClose();
        } catch (error) {
            console.error('Save loan error:', error);
            Alert.alert(t('common.error'), t('personal.loan.saveError'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        resetForm();
        onClose();
    };

    const handleAddPerson = () => {
        onClose();
        onAddPerson?.();
    };

    // No friends - show prompt to add friend first
    if (!hasFriends && visible) {
        return (
            <Modal
                visible={visible}
                animationType="slide"
                presentationStyle="pageSheet"
                onRequestClose={handleClose}
            >
                <View style={styles.container}>
                    {/* Header */}
                    <View style={styles.header}>
                        <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
                            <FontAwesome name="times" size={24} color="#333" />
                        </TouchableOpacity>
                        <Text style={styles.title}>{t('personal.loan.title')}</Text>
                        <View style={styles.placeholder} />
                    </View>

                    {/* Empty State */}
                    <View style={styles.emptyState}>
                        <FontAwesome name="user-plus" size={64} color="#BDBDBD" />
                        <Text style={styles.emptyTitle}>{t('personal.loan.noFriends')}</Text>
                        <TouchableOpacity
                            style={styles.addFriendButton}
                            onPress={handleAddPerson}
                        >
                            <FontAwesome name="plus" size={16} color="#FFFFFF" />
                            <Text style={styles.addFriendButtonText}>{t('personal.loan.addFriend')}</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
        );
    }

    return (
        <Modal
            visible={visible}
            animationType="slide"
            presentationStyle="pageSheet"
            onRequestClose={handleClose}
        >
            <KeyboardAvoidingView
                style={styles.container}
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            >
                {/* Header */}
                <View style={styles.header}>
                    <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
                        <FontAwesome name="times" size={24} color="#333" />
                    </TouchableOpacity>
                    <Text style={styles.title}>{t('personal.loan.title')}</Text>
                    <View style={styles.placeholder} />
                </View>

                <ScrollView style={styles.form} showsVerticalScrollIndicator={false}>
                    {/* Select Friend */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('personal.loan.selectFriend')} *</Text>
                        <TouchableOpacity
                            style={styles.selectButton}
                            onPress={() => setShowFriendPicker(true)}
                        >
                            <Text style={[
                                styles.selectButtonText,
                                !selectedFriend && styles.selectButtonPlaceholder
                            ]}>
                                {selectedFriend?.name || t('personal.loan.selectFriend')}
                            </Text>
                            <FontAwesome name="chevron-down" size={16} color="#666" />
                        </TouchableOpacity>
                    </View>

                    {/* Loan Type */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.category')}</Text>
                        <View style={styles.loanTypeContainer}>
                            <TouchableOpacity
                                style={[
                                    styles.loanTypeButton,
                                    loanType === 'give' && styles.loanTypeButtonActive,
                                ]}
                                onPress={() => setLoanType('give')}
                            >
                                <FontAwesome
                                    name="arrow-up"
                                    size={18}
                                    color={loanType === 'give' ? '#FFFFFF' : '#D32F2F'}
                                />
                                <Text style={[
                                    styles.loanTypeText,
                                    loanType === 'give' && styles.loanTypeTextActive,
                                ]}>
                                    {t('personal.loan.giveToFriend')}
                                </Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[
                                    styles.loanTypeButton,
                                    loanType === 'receive' && styles.loanTypeButtonActiveGreen,
                                ]}
                                onPress={() => setLoanType('receive')}
                            >
                                <FontAwesome
                                    name="arrow-down"
                                    size={18}
                                    color={loanType === 'receive' ? '#FFFFFF' : '#2E7D32'}
                                />
                                <Text style={[
                                    styles.loanTypeText,
                                    loanType === 'receive' && styles.loanTypeTextActive,
                                ]}>
                                    {t('personal.loan.receiveFromFriend')}
                                </Text>
                            </TouchableOpacity>
                        </View>
                    </View>

                    {/* Amount */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('personal.loan.amount')} *</Text>
                        <View style={styles.amountInputContainer}>
                            <Text style={styles.currencySymbol}>₹</Text>
                            <TextInput
                                style={styles.amountInput}
                                value={amount}
                                onChangeText={setAmount}
                                placeholder="0"
                                placeholderTextColor="#9E9E9E"
                                keyboardType="numeric"
                            />
                        </View>
                    </View>

                    {/* Description */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('personal.loan.description')}</Text>
                        <TextInput
                            style={styles.textInput}
                            value={description}
                            onChangeText={setDescription}
                            placeholder={t('forms.descriptionPlaceholder')}
                            placeholderTextColor="#9E9E9E"
                            multiline
                            numberOfLines={2}
                        />
                    </View>
                </ScrollView>

                {/* Submit Button */}
                <View style={styles.footer}>
                    <TouchableOpacity
                        style={[styles.submitButton, isLoading && styles.submitButtonDisabled]}
                        onPress={handleSubmit}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <Text style={styles.submitButtonText}>{t('common.save')}</Text>
                        )}
                    </TouchableOpacity>
                </View>

                {/* Friend Picker Modal */}
                <Modal
                    visible={showFriendPicker}
                    transparent
                    animationType="fade"
                    onRequestClose={() => setShowFriendPicker(false)}
                >
                    <TouchableOpacity
                        style={styles.pickerOverlay}
                        activeOpacity={1}
                        onPress={() => setShowFriendPicker(false)}
                    >
                        <View style={styles.pickerContainer}>
                            <Text style={styles.pickerTitle}>{t('personal.loan.selectFriend')}</Text>
                            <ScrollView>
                                {customers.map((friend) => (
                                    <TouchableOpacity
                                        key={friend.id}
                                        style={[
                                            styles.pickerItem,
                                            selectedFriend?.id === friend.id && styles.pickerItemSelected,
                                        ]}
                                        onPress={() => {
                                            setSelectedFriend(friend);
                                            setShowFriendPicker(false);
                                        }}
                                    >
                                        <View style={styles.friendInfo}>
                                            <Text style={[
                                                styles.pickerItemText,
                                                selectedFriend?.id === friend.id && styles.pickerItemTextSelected,
                                            ]}>
                                                {friend.name}
                                            </Text>
                                            {friend.outstanding > 0 && (
                                                <Text style={styles.outstandingText}>
                                                    ₹{friend.outstanding.toLocaleString('en-IN')} {t('customers.outstanding')}
                                                </Text>
                                            )}
                                        </View>
                                        {selectedFriend?.id === friend.id && (
                                            <FontAwesome name="check" size={16} color="#2E7D32" />
                                        )}
                                    </TouchableOpacity>
                                ))}
                            </ScrollView>
                        </View>
                    </TouchableOpacity>
                </Modal>
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
        padding: 4,
    },
    title: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
    },
    placeholder: {
        width: 32,
    },
    form: {
        flex: 1,
        padding: 16,
    },
    inputGroup: {
        marginBottom: 20,
    },
    label: {
        fontSize: 14,
        fontWeight: '600',
        color: '#424242',
        marginBottom: 8,
    },
    selectButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: '#F5F5F5',
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 16,
    },
    selectButtonText: {
        fontSize: 16,
        color: '#212121',
    },
    selectButtonPlaceholder: {
        color: '#9E9E9E',
    },
    loanTypeContainer: {
        flexDirection: 'row',
        gap: 12,
    },
    loanTypeButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        backgroundColor: '#F5F5F5',
        borderRadius: 12,
        paddingVertical: 14,
        borderWidth: 2,
        borderColor: 'transparent',
    },
    loanTypeButtonActive: {
        backgroundColor: '#D32F2F',
        borderColor: '#D32F2F',
    },
    loanTypeButtonActiveGreen: {
        backgroundColor: '#2E7D32',
        borderColor: '#2E7D32',
    },
    loanTypeText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#424242',
    },
    loanTypeTextActive: {
        color: '#FFFFFF',
    },
    amountInputContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#F5F5F5',
        borderRadius: 12,
        paddingHorizontal: 16,
    },
    currencySymbol: {
        fontSize: 24,
        fontWeight: '700',
        color: '#2E7D32',
        marginRight: 8,
    },
    amountInput: {
        flex: 1,
        fontSize: 32,
        fontWeight: '700',
        color: '#212121',
        paddingVertical: 16,
    },
    textInput: {
        backgroundColor: '#F5F5F5',
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 12,
        fontSize: 16,
        color: '#212121',
        minHeight: 60,
        textAlignVertical: 'top',
    },
    footer: {
        padding: 16,
        borderTopWidth: 1,
        borderTopColor: '#E0E0E0',
    },
    submitButton: {
        backgroundColor: '#2E7D32',
        borderRadius: 12,
        paddingVertical: 16,
        alignItems: 'center',
    },
    submitButtonDisabled: {
        opacity: 0.6,
    },
    submitButtonText: {
        fontSize: 18,
        fontWeight: '700',
        color: '#FFFFFF',
    },
    emptyState: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        padding: 32,
    },
    emptyTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#757575',
        marginTop: 16,
        marginBottom: 24,
        textAlign: 'center',
    },
    addFriendButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        backgroundColor: '#2E7D32',
        borderRadius: 12,
        paddingHorizontal: 24,
        paddingVertical: 14,
    },
    addFriendButtonText: {
        fontSize: 16,
        fontWeight: '700',
        color: '#FFFFFF',
    },
    pickerOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    pickerContainer: {
        backgroundColor: '#FFFFFF',
        borderRadius: 16,
        padding: 16,
        width: '85%',
        maxHeight: '60%',
    },
    pickerTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 16,
        textAlign: 'center',
    },
    pickerItem: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: 14,
        paddingHorizontal: 12,
        borderRadius: 8,
    },
    pickerItemSelected: {
        backgroundColor: '#E8F5E9',
    },
    friendInfo: {
        flex: 1,
    },
    pickerItemText: {
        fontSize: 16,
        color: '#424242',
    },
    pickerItemTextSelected: {
        color: '#2E7D32',
        fontWeight: '600',
    },
    outstandingText: {
        fontSize: 12,
        color: '#D32F2F',
        marginTop: 2,
    },
});
