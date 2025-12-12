// ============================================
// Stock Adjustment Form - Modal Form Component
// ============================================
// Used for adjusting stock with proper audit trail
// Creates expense records for all adjustments

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
import { TransliterationInput } from '@/components/ui/TransliterationInput';
import { Product, ExpenseCategory } from '@/types';
import { useTranslation } from 'react-i18next';

type AdjustmentType = 'ADD' | 'REMOVE';

interface StockAdjustmentFormProps {
    visible: boolean;
    onClose: () => void;
    preSelectedProduct?: Product;
}

export function StockAdjustmentForm({ visible, onClose, preSelectedProduct }: StockAdjustmentFormProps) {
    const { t } = useTranslation();
    const { products, updateProduct, addExpense } = useData();

    const ADJUSTMENT_TYPES: { key: AdjustmentType; label: string; icon: 'plus-circle' | 'minus-circle'; color: string }[] = [
        { key: 'ADD', label: t('stockAdjustment.add'), icon: 'plus-circle', color: '#2E7D32' },
        { key: 'REMOVE', label: t('stockAdjustment.remove'), icon: 'minus-circle', color: '#D32F2F' },
    ];
    const [isLoading, setIsLoading] = useState(false);

    // Form state
    const [selectedProductId, setSelectedProductId] = useState<string | undefined>();
    const [adjustmentType, setAdjustmentType] = useState<AdjustmentType>('ADD');
    const [quantity, setQuantity] = useState('');
    const [reason, setReason] = useState('');
    const [costPerUnit, setCostPerUnit] = useState('');
    const [showProductPicker, setShowProductPicker] = useState(false);

    // Selected product
    const selectedProduct = products.find(p => p.id === selectedProductId);

    // Reset form when visible changes
    useEffect(() => {
        if (visible) {
            if (preSelectedProduct) {
                setSelectedProductId(preSelectedProduct.id);
                setCostPerUnit(preSelectedProduct.costPrice.toString());
            } else {
                setSelectedProductId(undefined);
            }
            setAdjustmentType('ADD');
            setQuantity('');
            setReason('');
        }
    }, [visible, preSelectedProduct]);

    // Update cost when product changes
    useEffect(() => {
        if (selectedProduct) {
            setCostPerUnit(selectedProduct.costPrice.toString());
        }
    }, [selectedProductId]);

    const resetForm = () => {
        setSelectedProductId(undefined);
        setAdjustmentType('ADD');
        setQuantity('');
        setReason('');
        setCostPerUnit('');
    };

    const handleSubmit = async () => {
        if (!selectedProduct) {
            Alert.alert(t('common.error'), t('stockAdjustment.selectProduct'));
            return;
        }

        const qty = parseInt(quantity);
        if (isNaN(qty) || qty <= 0) {
            Alert.alert(t('common.error'), t('forms.stockRequired'));
            return;
        }

        if (adjustmentType === 'REMOVE' && qty > selectedProduct.stock) {
            Alert.alert(t('common.error'), `${t('stockAdjustment.currentStock', { stock: selectedProduct.stock })}`);
            return;
        }



        // Calculate stock delta based on adjustment type
        const stockDelta = adjustmentType === 'ADD' ? qty : -qty;

        setIsLoading(true);
        try {
            const today = new Date().toISOString().split('T')[0];
            const cost = parseFloat(costPerUnit) || selectedProduct.costPrice || 0;
            const totalCost = Math.abs(stockDelta) * cost;

            // Determine expense category based on adjustment type
            let expenseCategory: ExpenseCategory;
            let description: string;

            if (adjustmentType === 'ADD') {
                expenseCategory = 'stock_received';
                description = `${selectedProduct.name}: +${qty} units${reason ? ` (${reason})` : ''}`;
            } else {
                expenseCategory = 'stock_wastage';
                description = `${selectedProduct.name}: -${qty} units${reason ? ` (${reason})` : ''}`;
            }

            // Create expense record for audit trail
            // Only ADD (stock_received) counts as actual cost/expense
            // REMOVE and CORRECTION are just for tracking, not actual spending
            await addExpense({
                category: expenseCategory,
                amount: adjustmentType === 'ADD' ? totalCost : 0, // Only purchases cost money
                description,
                date: today,
            });

            // Update product stock
            const newStock = selectedProduct.stock + stockDelta;
            await updateProduct(selectedProduct.id, { stock: newStock });

            const actionText = adjustmentType === 'ADD' ? t('stockAdjustment.added') : t('stockAdjustment.removed');
            Alert.alert(t('common.success'), t('stockAdjustment.success', { action: actionText, stock: newStock }));

            resetForm();
            onClose();
        } catch (error) {
            console.error('Stock adjustment error:', error);
            Alert.alert(t('common.error'), t('stockAdjustment.updateError'));
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
            <View style={styles.container}>
                {/* Header */}
                <View style={styles.header}>
                    <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
                        <FontAwesome name="times" size={20} color="#757575" />
                    </TouchableOpacity>
                    <Text style={styles.title}>{t('stockAdjustment.title')}</Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView style={styles.form} showsVerticalScrollIndicator={false}>
                    {/* Product Selector */}
                    <TouchableOpacity
                        style={styles.productSelector}
                        onPress={() => setShowProductPicker(true)}
                    >
                        <View style={styles.productIcon}>
                            <FontAwesome name="cube" size={20} color="#2E7D32" />
                        </View>
                        <View style={styles.productInfo}>
                            <Text style={styles.productLabel}>{t('stockAdjustment.product')}</Text>
                            <Text style={styles.productName}>
                                {selectedProduct ? selectedProduct.name : t('stockAdjustment.selectProduct')}
                            </Text>
                            {selectedProduct && (
                                <Text style={styles.currentStock}>
                                    {t('stockAdjustment.currentStock', { stock: selectedProduct.stock })}
                                </Text>
                            )}
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#9E9E9E" />
                    </TouchableOpacity>

                    {/* Adjustment Type */}
                    <View style={styles.section}>
                        <Text style={styles.sectionTitle}>{t('stockAdjustment.adjustmentType')}</Text>
                        <View style={styles.typeGrid}>
                            {ADJUSTMENT_TYPES.map((type) => (
                                <TouchableOpacity
                                    key={type.key}
                                    style={[
                                        styles.typeCard,
                                        adjustmentType === type.key && { borderColor: type.color, backgroundColor: type.color + '10' }
                                    ]}
                                    onPress={() => setAdjustmentType(type.key)}
                                >
                                    <FontAwesome
                                        name={type.icon}
                                        size={24}
                                        color={adjustmentType === type.key ? type.color : '#9E9E9E'}
                                    />
                                    <Text style={[
                                        styles.typeLabel,
                                        adjustmentType === type.key && { color: type.color }
                                    ]}>
                                        {type.label}
                                    </Text>
                                </TouchableOpacity>
                            ))}
                        </View>
                    </View>

                    {/* Quantity Input */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('stockAdjustment.quantity')}</Text>
                        <TextInput
                            style={styles.input}
                            value={quantity}
                            onChangeText={(text) => setQuantity(text.replace(/[^0-9]/g, ''))}
                            placeholder={t('stockAdjustment.howMany')}
                            placeholderTextColor="#9E9E9E"
                            keyboardType="numeric"
                        />
                    </View>

                    {/* Cost Per Unit (for ADD only) */}
                    {adjustmentType === 'ADD' && (
                        <View style={styles.inputGroup}>
                            <Text style={styles.label}>{t('stockAdjustment.costPerUnit')}</Text>
                            <TextInput
                                style={styles.input}
                                value={costPerUnit}
                                onChangeText={(text) => setCostPerUnit(text.replace(/[^0-9.]/g, ''))}
                                placeholder={t('stockAdjustment.cost')}
                                placeholderTextColor="#9E9E9E"
                                keyboardType="numeric"
                            />
                        </View>
                    )}

                    {/* Reason */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('stockAdjustment.reason')}</Text>
                        <TransliterationInput
                            style={[styles.input, styles.textArea]}
                            value={reason}
                            onChangeText={setReason}
                            placeholder={t('stockAdjustment.reasonPlaceholder')}
                            placeholderTextColor="#9E9E9E"
                            multiline
                            numberOfLines={2}
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
                            <Text style={styles.submitButtonText}>{t('stockAdjustment.submit')}</Text>
                        )}
                    </TouchableOpacity>
                </ScrollView>

                {/* Product Picker Modal */}
                <Modal
                    visible={showProductPicker}
                    animationType="slide"
                    presentationStyle="pageSheet"
                    onRequestClose={() => setShowProductPicker(false)}
                >
                    <View style={styles.pickerContainer}>
                        <View style={styles.pickerHeader}>
                            <Text style={styles.pickerTitle}>{t('stockAdjustment.selectProduct')}</Text>
                            <TouchableOpacity onPress={() => setShowProductPicker(false)}>
                                <FontAwesome name="times" size={20} color="#757575" />
                            </TouchableOpacity>
                        </View>
                        <ScrollView>
                            {products.map((product) => (
                                <TouchableOpacity
                                    key={product.id}
                                    style={styles.pickerItem}
                                    onPress={() => {
                                        setSelectedProductId(product.id);
                                        setShowProductPicker(false);
                                    }}
                                >
                                    <View>
                                        <Text style={styles.pickerItemName}>{product.name}</Text>
                                        <Text style={styles.pickerItemStock}>{t('inventory.stock')}: {product.stock}</Text>
                                    </View>
                                    <Text style={styles.pickerItemPrice}>₹{product.price}</Text>
                                </TouchableOpacity>
                            ))}
                            {products.length === 0 && (
                                <Text style={styles.emptyPicker}>{t('stockAdjustment.noProducts')}</Text>
                            )}
                        </ScrollView>
                    </View>
                </Modal>
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
    productSelector: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        padding: 16,
        borderRadius: 12,
        marginBottom: 20,
    },
    productIcon: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: '#E8F5E9',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    productInfo: {
        flex: 1,
    },
    productLabel: {
        fontSize: 12,
        color: '#757575',
    },
    productName: {
        fontSize: 16,
        fontWeight: '600',
        color: '#212121',
    },
    currentStock: {
        fontSize: 12,
        color: '#2E7D32',
        marginTop: 2,
    },
    section: {
        marginBottom: 20,
    },
    sectionTitle: {
        fontSize: 14,
        fontWeight: '600',
        color: '#424242',
        marginBottom: 12,
    },
    typeGrid: {
        flexDirection: 'row',
        gap: 10,
    },
    typeCard: {
        flex: 1,
        alignItems: 'center',
        padding: 16,
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        borderWidth: 2,
        borderColor: '#E0E0E0',
    },
    typeLabel: {
        fontSize: 12,
        fontWeight: '500',
        color: '#757575',
        marginTop: 8,
        textAlign: 'center',
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
    textArea: {
        minHeight: 80,
        textAlignVertical: 'top',
    },

    submitButton: {
        backgroundColor: '#2E7D32',
        borderRadius: 12,
        paddingVertical: 16,
        alignItems: 'center',
        marginTop: 16,
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
    pickerContainer: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    pickerHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    pickerTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
    },
    pickerItem: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#F5F5F5',
    },
    pickerItemName: {
        fontSize: 16,
        color: '#212121',
    },
    pickerItemStock: {
        fontSize: 12,
        color: '#757575',
        marginTop: 2,
    },
    pickerItemPrice: {
        fontSize: 16,
        fontWeight: '600',
        color: '#2E7D32',
    },
    emptyPicker: {
        textAlign: 'center',
        padding: 32,
        color: '#757575',
    },
});
