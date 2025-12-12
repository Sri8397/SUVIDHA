// ============================================
// Add Sale Form - Modal Form Component
// ============================================

import React, { useState, useMemo } from 'react';
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
import { useData } from '@/contexts/DataContext';
import { SaleItem, Sale } from '@/types';
import { useTranslation } from 'react-i18next';
import { Colors } from '@/constants/theme';

interface AddSaleFormProps {
    visible: boolean;
    onClose: () => void;
}

export function AddSaleForm({ visible, onClose }: AddSaleFormProps) {
    const { t } = useTranslation();
    const { products, customers, createSale } = useData();
    const [isLoading, setIsLoading] = useState(false);

    // Form state
    const [selectedCustomerId, setSelectedCustomerId] = useState<string | undefined>();
    const [customerName, setCustomerName] = useState(t('forms.sale.walkInCustomer'));
    const [items, setItems] = useState<SaleItem[]>([]);
    const [discount, setDiscount] = useState('0');
    const [amountPaid, setAmountPaid] = useState('');
    const [paymentMethod, setPaymentMethod] = useState<Sale['paymentMethod']>('cash');
    const [showProductPicker, setShowProductPicker] = useState(false);
    const [showCustomerPicker, setShowCustomerPicker] = useState(false);

    // Calculations
    const subtotal = useMemo(() =>
        items.reduce((sum, item) => sum + item.total, 0), [items]);

    const discountAmount = Number(discount) || 0;
    const total = Math.max(0, subtotal - discountAmount);

    // Update amount paid when total changes or customer type changes
    React.useEffect(() => {
        if (!selectedCustomerId) {
            // Walk-in: always pay full amount
            setAmountPaid(total.toString());
        } else {
            // Registered: clamp if current amount exceeds new total
            setAmountPaid(prev => {
                const current = Number(prev);
                if (current > total) return total.toString();
                return prev;
            });
        }
    }, [total, selectedCustomerId]);

    const resetForm = () => {
        setSelectedCustomerId(undefined);
        setCustomerName(t('forms.sale.walkInCustomer'));
        setItems([]);
        setDiscount('0');
        setAmountPaid('');
        setPaymentMethod('cash');
    };

    const addItem = (productId: string) => {
        const product = products.find(p => p.id === productId);
        if (!product) return;

        if (product.stock <= 0) {
            Alert.alert(t('forms.sale.outOfStock'), t('forms.sale.productOutOfStock'));
            return;
        }

        // Check if already in list
        const existingIndex = items.findIndex(i => i.productId === productId);
        if (existingIndex >= 0) {
            // Check stock limit
            if (items[existingIndex].quantity >= product.stock) {
                Alert.alert(t('forms.sale.stockLimit'), t('forms.sale.itemAvailable', { count: product.stock }));
                return;
            }

            // Increase quantity
            const newItems = [...items];
            newItems[existingIndex].quantity += 1;
            newItems[existingIndex].total = newItems[existingIndex].quantity * newItems[existingIndex].unitPrice;
            setItems(newItems);
        } else {
            // Add new item with cost price for profit calculation
            setItems([...items, {
                productId: product.id,
                productName: product.name,
                quantity: 1,
                unitPrice: product.price,
                costPrice: product.costPrice || 0,
                total: product.price,
            }]);
        }
        setShowProductPicker(false);
    };

    const updateItemQuantity = (index: number, qty: number) => {
        if (qty <= 0) {
            // Remove item
            setItems(items.filter((_, i) => i !== index));
        } else {
            const item = items[index];
            const product = products.find(p => p.id === item.productId);

            if (product && qty > product.stock) {
                Alert.alert(t('forms.sale.stockLimit'), t('forms.sale.itemAvailable', { count: product.stock }));
                return;
            }

            const newItems = [...items];
            newItems[index].quantity = qty;
            newItems[index].total = qty * newItems[index].unitPrice;
            setItems(newItems);
        }
    };

    const selectCustomer = (customerId: string) => {
        const customer = customers.find(c => c.id === customerId);
        if (customer) {
            setSelectedCustomerId(customerId);
            setCustomerName(customer.name);
        }
        setShowCustomerPicker(false);
    };

    const handleSubmit = async () => {
        if (items.length === 0) {
            Alert.alert(t('common.error'), t('forms.sale.addAtLeastOne'));
            return;
        }

        const paid = Number(amountPaid) || 0;

        setIsLoading(true);
        try {
            const today = new Date().toISOString().split('T')[0];

            let paymentStatus: Sale['paymentStatus'] = 'pending';
            if (paid >= total) {
                paymentStatus = 'paid';
            } else if (paid > 0) {
                paymentStatus = 'partial';
            }

            await createSale({
                saleType: 'PRODUCT_SALE',
                customerId: selectedCustomerId,
                items,
                subtotal,
                discount: discountAmount,
                total,
                amountPaid: paid,
                paymentStatus,
                paymentMethod,
                date: today,
            });

            Alert.alert(t('common.success'), t('forms.sale.saleRecorded'));
            resetForm();
            onClose();
        } catch (error) {
            console.error('Save sale error:', error);
            Alert.alert(t('common.error'), t('forms.sale.saveError'));
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
                    <Text style={styles.title}>{t('forms.sale.title')}</Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView
                    style={styles.form}
                    showsVerticalScrollIndicator={false}
                    keyboardShouldPersistTaps="handled"
                >
                    {/* Customer Selection */}
                    <TouchableOpacity
                        style={styles.customerSelector}
                        onPress={() => setShowCustomerPicker(true)}
                    >
                        <View style={styles.customerIcon}>
                            <FontAwesome name="user" size={20} color={Colors.light.primary} />
                        </View>
                        <View style={styles.customerInfo}>
                            <Text style={styles.customerLabel}>{t('forms.sale.selectCustomer')}</Text>
                            <Text style={styles.customerName}>{customerName}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color={Colors.light.textTertiary} />
                    </TouchableOpacity>

                    {/* Items List */}
                    <View style={styles.section}>
                        <View style={styles.sectionHeader}>
                            <Text style={styles.sectionTitle}>{t('forms.sale.products')}</Text>
                            <TouchableOpacity
                                style={styles.addButton}
                                onPress={() => setShowProductPicker(true)}
                            >
                                <FontAwesome name="plus" size={14} color={Colors.light.white} />
                                <Text style={styles.addButtonText}>{t('forms.sale.addProduct')}</Text>
                            </TouchableOpacity>
                        </View>

                        {items.length === 0 ? (
                            <View style={styles.emptyItems}>
                                <FontAwesome name="shopping-cart" size={32} color={Colors.light.textTertiary} />
                                <Text style={styles.emptyText}>{t('forms.sale.addProductInstruction')}</Text>
                            </View>
                        ) : (
                            items.map((item, index) => (
                                <View key={item.productId} style={styles.itemCard}>
                                    <View style={styles.itemInfo}>
                                        <Text style={styles.itemName}>{item.productName}</Text>
                                        <Text style={styles.itemPrice}>₹{item.unitPrice} × {item.quantity}</Text>
                                    </View>
                                    <View style={styles.itemControls}>
                                        <TouchableOpacity
                                            style={styles.qtyButton}
                                            onPress={() => updateItemQuantity(index, item.quantity - 1)}
                                        >
                                            <FontAwesome name="minus" size={12} color={Colors.light.textSecondary} />
                                        </TouchableOpacity>
                                        <Text style={styles.qtyText}>{item.quantity}</Text>
                                        <TouchableOpacity
                                            style={styles.qtyButton}
                                            onPress={() => updateItemQuantity(index, item.quantity + 1)}
                                        >
                                            <FontAwesome name="plus" size={12} color={Colors.light.textSecondary} />
                                        </TouchableOpacity>
                                    </View>
                                    <Text style={styles.itemTotal}>₹{item.total}</Text>
                                </View>
                            ))
                        )}
                    </View>

                    {/* Discount */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.sale.discount')} (₹)</Text>
                        <TextInput
                            style={styles.input}
                            value={discount}
                            onChangeText={(text) => {
                                if (text === '') {
                                    setDiscount('');
                                    return;
                                }
                                const val = Number(text);
                                if (!isNaN(val)) {
                                    if (val > subtotal) {
                                        setDiscount(subtotal.toString());
                                    } else {
                                        setDiscount(text);
                                    }
                                }
                            }}
                            placeholder="0"
                            placeholderTextColor={Colors.light.textTertiary}
                            keyboardType="numeric"
                        />
                    </View>

                    {/* Payment Method */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.sale.paymentMethod')}</Text>
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

                    {/* Amount Paid */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.sale.amountPaid')} (₹)</Text>
                        <TextInput
                            style={[
                                styles.input,
                                !selectedCustomerId && styles.inputDisabled
                            ]}
                            value={amountPaid}
                            onChangeText={(text) => {
                                if (!selectedCustomerId) return;

                                if (text === '') {
                                    setAmountPaid('');
                                    return;
                                }

                                const val = Number(text);
                                if (isNaN(val)) return;

                                if (val > total) {
                                    setAmountPaid(total.toString());
                                } else {
                                    setAmountPaid(text);
                                }
                            }}
                            placeholder={total.toString()}
                            placeholderTextColor={Colors.light.textTertiary}
                            keyboardType="numeric"
                            editable={!!selectedCustomerId}
                        />
                    </View>

                    {/* Total Summary */}
                    <View style={styles.totalCard}>
                        <View style={styles.totalRow}>
                            <Text style={styles.totalLabel}>{t('forms.sale.subtotal')}</Text>
                            <Text style={styles.totalValue}>₹{subtotal.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={styles.totalRow}>
                            <Text style={styles.totalLabel}>{t('forms.sale.discount')}</Text>
                            <Text style={styles.discountValue}>-₹{discountAmount.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={[styles.totalRow, styles.grandTotalRow]}>
                            <Text style={styles.grandTotalLabel}>{t('forms.sale.total')}</Text>
                            <Text style={styles.grandTotalValue}>₹{total.toLocaleString('en-IN')}</Text>
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
                            <Text style={styles.submitButtonText}>{t('forms.sale.completeSale')}</Text>
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
                            <Text style={styles.pickerTitle}>{t('forms.sale.selectProduct')}</Text>
                            <TouchableOpacity onPress={() => setShowProductPicker(false)}>
                                <FontAwesome name="times" size={20} color={Colors.light.textSecondary} />
                            </TouchableOpacity>
                        </View>
                        <ScrollView>
                            {products.map((product) => (
                                <TouchableOpacity
                                    key={product.id}
                                    style={styles.pickerItem}
                                    onPress={() => addItem(product.id)}
                                >
                                    <Text style={styles.pickerItemName}>{product.name}</Text>
                                    <Text style={styles.pickerItemPrice}>₹{product.price}</Text>
                                </TouchableOpacity>
                            ))}
                            {products.length === 0 && (
                                <Text style={styles.emptyPicker}>{t('forms.sale.noProducts')}</Text>
                            )}
                        </ScrollView>
                    </View>
                </Modal>

                {/* Customer Picker Modal */}
                <Modal
                    visible={showCustomerPicker}
                    animationType="slide"
                    presentationStyle="pageSheet"
                    onRequestClose={() => setShowCustomerPicker(false)}
                >
                    <View style={styles.pickerContainer}>
                        <View style={styles.pickerHeader}>
                            <Text style={styles.pickerTitle}>{t('forms.sale.selectCustomer')}</Text>
                            <TouchableOpacity onPress={() => setShowCustomerPicker(false)}>
                                <FontAwesome name="times" size={20} color={Colors.light.textSecondary} />
                            </TouchableOpacity>
                        </View>
                        <ScrollView>
                            <TouchableOpacity
                                style={styles.pickerItem}
                                onPress={() => {
                                    setSelectedCustomerId(undefined);
                                    setCustomerName(t('forms.sale.walkInCustomer'));
                                    setShowCustomerPicker(false);
                                }}
                            >
                                <Text style={styles.pickerItemName}>{t('forms.sale.walkInCustomer')}</Text>
                            </TouchableOpacity>
                            {customers.map((customer) => (
                                <TouchableOpacity
                                    key={customer.id}
                                    style={styles.pickerItem}
                                    onPress={() => selectCustomer(customer.id)}
                                >
                                    <Text style={styles.pickerItemName}>{customer.name}</Text>
                                    <Text style={styles.pickerItemPrice}>{customer.phone}</Text>
                                </TouchableOpacity>
                            ))}
                        </ScrollView>
                    </View>
                </Modal>
            </KeyboardAvoidingView>
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
    customerSelector: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: Colors.light.card,
        padding: 16,
        borderRadius: 12,
        marginBottom: 16,
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
    section: {
        marginBottom: 16,
    },
    sectionHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 12,
    },
    sectionTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: Colors.light.text,
    },
    addButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        backgroundColor: Colors.light.primary,
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 8,
    },
    addButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.white,
    },
    emptyItems: {
        backgroundColor: Colors.light.card,
        padding: 32,
        borderRadius: 12,
        alignItems: 'center',
    },
    emptyText: {
        fontSize: 14,
        color: Colors.light.textSecondary,
        marginTop: 12,
        textAlign: 'center',
    },
    itemCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: Colors.light.card,
        padding: 12,
        borderRadius: 12,
        marginBottom: 8,
    },
    itemInfo: {
        flex: 1,
    },
    itemName: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.text,
    },
    itemPrice: {
        fontSize: 12,
        color: Colors.light.textSecondary,
        marginTop: 2,
    },
    itemControls: {
        flexDirection: 'row',
        alignItems: 'center',
        marginHorizontal: 12,
    },
    qtyButton: {
        width: 28,
        height: 28,
        borderRadius: 14,
        backgroundColor: Colors.light.background,
        alignItems: 'center',
        justifyContent: 'center',
    },
    qtyText: {
        fontSize: 16,
        fontWeight: '600',
        marginHorizontal: 12,
    },
    itemTotal: {
        fontSize: 16,
        fontWeight: '700',
        color: Colors.light.primary,
        minWidth: 60,
        textAlign: 'right',
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
        backgroundColor: Colors.light.card,
    },
    inputDisabled: {
        backgroundColor: Colors.light.background,
        color: Colors.light.textTertiary,
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
    totalCard: {
        backgroundColor: Colors.light.card,
        borderRadius: 12,
        padding: 16,
        marginTop: 8,
    },
    totalRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 8,
    },
    totalLabel: {
        fontSize: 14,
        color: Colors.light.textSecondary,
    },
    totalValue: {
        fontSize: 14,
        color: Colors.light.text,
    },
    discountValue: {
        fontSize: 14,
        color: Colors.light.error,
    },
    grandTotalRow: {
        borderTopWidth: 1,
        borderTopColor: Colors.light.border,
        paddingTop: 12,
        marginTop: 4,
        marginBottom: 0,
    },
    grandTotalLabel: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
    },
    grandTotalValue: {
        fontSize: 20,
        fontWeight: '700',
        color: Colors.light.primary,
    },
    submitButton: {
        backgroundColor: Colors.light.primary,
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
        color: Colors.light.white,
    },
    pickerContainer: {
        flex: 1,
        backgroundColor: Colors.light.card,
    },
    pickerHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: 16,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.border,
    },
    pickerTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
    },
    pickerItem: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: 16,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.background,
    },
    pickerItemName: {
        fontSize: 16,
        color: Colors.light.text,
    },
    pickerItemPrice: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.primary,
    },
    emptyPicker: {
        textAlign: 'center',
        padding: 32,
        color: Colors.light.textSecondary,
    },
});
