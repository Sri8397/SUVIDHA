// ============================================
// Add Expense Form - Modal Form Component
// ============================================

import React, { useState } from 'react';
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
import { ExpenseCategory } from '@/types';
import { useTranslation } from 'react-i18next';
import { Colors } from '@/constants/theme';

interface AddExpenseFormProps {
    visible: boolean;
    onClose: () => void;
}

const EXPENSE_CATEGORIES: { id: ExpenseCategory; icon: string }[] = [
    { id: 'rent', icon: 'home' },
    { id: 'electricity', icon: 'bolt' },
    { id: 'salary', icon: 'users' },
    { id: 'inventory', icon: 'cubes' },
    { id: 'transport', icon: 'truck' },
    { id: 'maintenance', icon: 'wrench' },
    { id: 'other', icon: 'ellipsis-h' },
];

export function AddExpenseForm({ visible, onClose }: AddExpenseFormProps) {
    const { t } = useTranslation();
    const { addExpense } = useData();
    const [isLoading, setIsLoading] = useState(false);

    // Form state
    const [category, setCategory] = useState<ExpenseCategory>('other');
    const [amount, setAmount] = useState('');
    const [description, setDescription] = useState('');

    const resetForm = () => {
        setCategory('other');
        setAmount('');
        setDescription('');
    };

    const handleSubmit = async () => {
        // Validation
        if (!amount || isNaN(Number(amount)) || Number(amount) <= 0) {
            Alert.alert(t('common.error'), t('forms.amountRequired'));
            return;
        }

        setIsLoading(true);
        try {
            const today = new Date().toISOString().split('T')[0];

            await addExpense({
                category,
                amount: Number(amount),
                description: description.trim() || t(`expenseCategories.${category}`),
                date: today,
            });

            Alert.alert(t('common.success'), t('forms.expense.added'));
            resetForm();
            onClose();
        } catch (error) {
            console.error('Save expense error:', error);
            Alert.alert(t('common.error'), t('forms.expense.saveError'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        resetForm();
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
                    <Text style={styles.title}>{t('forms.expense.addTitle')}</Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView
                    style={styles.form}
                    showsVerticalScrollIndicator={false}
                    keyboardShouldPersistTaps="handled"
                >
                    {/* Category Selection */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.expense.category')}</Text>
                        <View style={styles.categoryGrid}>
                            {EXPENSE_CATEGORIES.map((cat) => (
                                <TouchableOpacity
                                    key={cat.id}
                                    style={[
                                        styles.categoryChip,
                                        category === cat.id && styles.categoryChipActive,
                                    ]}
                                    onPress={() => setCategory(cat.id)}
                                >
                                    <FontAwesome
                                        name={cat.icon as any}
                                        size={14}
                                        color={category === cat.id ? Colors.light.white : Colors.light.textSecondary}
                                    />
                                    <Text
                                        style={[
                                            styles.categoryChipText,
                                            category === cat.id && styles.categoryChipTextActive,
                                        ]}
                                    >
                                        {t(`expenseCategories.${cat.id}`)}
                                    </Text>
                                </TouchableOpacity>
                            ))}
                        </View>
                    </View>

                    {/* Amount */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.expense.amount')} *</Text>
                        <View style={styles.amountInputContainer}>
                            <Text style={styles.currencySymbol}>₹</Text>
                            <TextInput
                                style={styles.amountInput}
                                value={amount}
                                onChangeText={setAmount}
                                placeholder="0"
                                placeholderTextColor={Colors.light.textTertiary}
                                keyboardType="numeric"
                            />
                        </View>
                    </View>

                    {/* Description */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.expense.description')}</Text>
                        <TransliterationInput
                            style={[styles.input, styles.textArea]}
                            value={description}
                            onChangeText={setDescription}
                            placeholder={t('forms.expense.descriptionPlaceholder')}
                            placeholderTextColor={Colors.light.textTertiary}
                            multiline
                            numberOfLines={3}
                            textAlignVertical="top"
                        />
                    </View>

                </ScrollView>

                {/* Submit Button - Fixed at Bottom */}
                <View style={styles.bottomButtonContainer}>
                    <TouchableOpacity
                        style={[styles.submitButton, isLoading && styles.submitButtonDisabled]}
                        onPress={handleSubmit}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <ActivityIndicator color={Colors.light.white} />
                        ) : (
                            <Text style={styles.submitButtonText}>{t('forms.addExpense')}</Text>
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
    inputGroup: {
        marginBottom: 20,
    },
    label: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.text,
        marginBottom: 12,
    },
    categoryGrid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8,
    },
    categoryChip: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: Colors.light.background,
        borderRadius: 20,
        paddingHorizontal: 12,
        paddingVertical: 8,
        gap: 6,
        borderWidth: 1,
        borderColor: Colors.light.border,
    },
    categoryChipActive: {
        backgroundColor: Colors.light.primary,
        borderColor: Colors.light.primary,
    },
    categoryChipText: {
        fontSize: 13,
        color: Colors.light.textSecondary,
    },
    categoryChipTextActive: {
        color: Colors.light.white,
        fontWeight: '600',
    },
    amountInputContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        borderWidth: 1,
        borderColor: Colors.light.border,
        borderRadius: 10,
        backgroundColor: Colors.light.background,
        paddingHorizontal: 12,
    },
    currencySymbol: {
        fontSize: 18,
        fontWeight: '600',
        color: Colors.light.primary,
        marginRight: 6,
    },
    amountInput: {
        flex: 1,
        fontSize: 20,
        fontWeight: '600',
        color: Colors.light.text,
        paddingVertical: 12,
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
        height: 80,
        paddingTop: 12,
    },
    bottomButtonContainer: {
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
