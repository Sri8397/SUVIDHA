// ============================================
// Add Income Form - For Personal Finance Mode
// ============================================

import React, { useState } from 'react';
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
import { IncomeSource } from '@/types';
import { useTranslation } from 'react-i18next';

interface AddIncomeFormProps {
    visible: boolean;
    onClose: () => void;
}

const INCOME_SOURCES: IncomeSource[] = ['salary', 'freelance', 'business', 'investment', 'gift', 'other'];

export function AddIncomeForm({ visible, onClose }: AddIncomeFormProps) {
    const { t } = useTranslation();
    const { createSale } = useData();
    const [isLoading, setIsLoading] = useState(false);

    // Form state
    const [amount, setAmount] = useState('');
    const [source, setSource] = useState<IncomeSource>('salary');
    const [description, setDescription] = useState('');
    const [showSourcePicker, setShowSourcePicker] = useState(false);

    const resetForm = () => {
        setAmount('');
        setSource('salary');
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
            const incomeAmount = Number(amount);

            await createSale({
                saleType: 'INCOME',
                items: [],
                subtotal: incomeAmount,
                discount: 0,
                total: incomeAmount,
                amountPaid: incomeAmount,
                paymentStatus: 'paid',
                paymentMethod: 'cash',
                date: today,
                incomeSource: source,
            });

            Alert.alert(t('common.success'), t('personal.income.added'));
            resetForm();
            onClose();
        } catch (error) {
            console.error('Save income error:', error);
            Alert.alert(t('common.error'), t('personal.income.saveError'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        resetForm();
        onClose();
    };

    const getSourceLabel = (sourceKey: IncomeSource) => {
        return t(`personal.incomeSources.${sourceKey}`);
    };

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
                    <Text style={styles.title}>{t('personal.income.title')}</Text>
                    <View style={styles.placeholder} />
                </View>

                <ScrollView style={styles.form} showsVerticalScrollIndicator={false}>
                    {/* Amount */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('personal.income.amount')} *</Text>
                        <View style={styles.amountInputContainer}>
                            <Text style={styles.currencySymbol}>₹</Text>
                            <TextInput
                                style={styles.amountInput}
                                value={amount}
                                onChangeText={setAmount}
                                placeholder="0"
                                placeholderTextColor="#9E9E9E"
                                keyboardType="numeric"
                                autoFocus
                            />
                        </View>
                    </View>

                    {/* Income Source */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('personal.income.source')} *</Text>
                        <TouchableOpacity
                            style={styles.selectButton}
                            onPress={() => setShowSourcePicker(true)}
                        >
                            <Text style={styles.selectButtonText}>
                                {getSourceLabel(source)}
                            </Text>
                            <FontAwesome name="chevron-down" size={16} color="#666" />
                        </TouchableOpacity>
                    </View>

                    {/* Description */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('personal.income.description')}</Text>
                        <TextInput
                            style={styles.textInput}
                            value={description}
                            onChangeText={setDescription}
                            placeholder={t('personal.income.descriptionPlaceholder')}
                            placeholderTextColor="#9E9E9E"
                            multiline
                            numberOfLines={3}
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

                {/* Source Picker Modal */}
                <Modal
                    visible={showSourcePicker}
                    transparent
                    animationType="fade"
                    onRequestClose={() => setShowSourcePicker(false)}
                >
                    <TouchableOpacity
                        style={styles.pickerOverlay}
                        activeOpacity={1}
                        onPress={() => setShowSourcePicker(false)}
                    >
                        <View style={styles.pickerContainer}>
                            <Text style={styles.pickerTitle}>{t('personal.incomeSource')}</Text>
                            <ScrollView>
                                {INCOME_SOURCES.map((sourceOption) => (
                                    <TouchableOpacity
                                        key={sourceOption}
                                        style={[
                                            styles.pickerItem,
                                            source === sourceOption && styles.pickerItemSelected,
                                        ]}
                                        onPress={() => {
                                            setSource(sourceOption);
                                            setShowSourcePicker(false);
                                        }}
                                    >
                                        <Text
                                            style={[
                                                styles.pickerItemText,
                                                source === sourceOption && styles.pickerItemTextSelected,
                                            ]}
                                        >
                                            {getSourceLabel(sourceOption)}
                                        </Text>
                                        {source === sourceOption && (
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
    textInput: {
        backgroundColor: '#F5F5F5',
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 12,
        fontSize: 16,
        color: '#212121',
        minHeight: 80,
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
    pickerItemText: {
        fontSize: 16,
        color: '#424242',
    },
    pickerItemTextSelected: {
        color: '#2E7D32',
        fontWeight: '600',
    },
});
