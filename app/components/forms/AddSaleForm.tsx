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
} from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useData } from '@/contexts/DataContext';
import { SaleItem, Sale } from '@/types';

interface AddSaleFormProps {
    visible: boolean;
    onClose: () => void;
}

export function AddSaleForm({ visible, onClose }: AddSaleFormProps) {
    const { products, customers, createSale } = useData();
    const [isLoading, setIsLoading] = useState(false);

    // Form state
    const [selectedCustomerId, setSelectedCustomerId] = useState<string | undefined>();
    const [customerName, setCustomerName] = useState('वॉक-इन ग्राहक');
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
        setCustomerName('वॉक-इन ग्राहक');
        setItems([]);
        setDiscount('0');
        setAmountPaid('');
        setPaymentMethod('cash');
    };

    const addItem = (productId: string) => {
        const product = products.find(p => p.id === productId);
        if (!product) return;

        if (product.stock <= 0) {
            Alert.alert('स्टॉक खत्म', 'यह प्रोडक्ट स्टॉक में नहीं है');
            return;
        }

        // Check if already in list
        const existingIndex = items.findIndex(i => i.productId === productId);
        if (existingIndex >= 0) {
            // Check stock limit
            if (items[existingIndex].quantity >= product.stock) {
                Alert.alert('स्टॉक सीमा', `केवल ${product.stock} आइटम उपलब्ध हैं`);
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
                Alert.alert('स्टॉक सीमा', `केवल ${product.stock} आइटम उपलब्ध हैं`);
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
            Alert.alert('त्रुटि', 'कम से कम एक प्रोडक्ट जोड़ें');
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

            Alert.alert('सफलता', 'बिक्री दर्ज हो गई');
            resetForm();
            onClose();
        } catch (error) {
            console.error('Save sale error:', error);
            Alert.alert('त्रुटि', 'बिक्री सेव करने में समस्या हुई');
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
                    <Text style={styles.title}>नई बिक्री</Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView style={styles.form} showsVerticalScrollIndicator={false}>
                    {/* Customer Selection */}
                    <TouchableOpacity
                        style={styles.customerSelector}
                        onPress={() => setShowCustomerPicker(true)}
                    >
                        <View style={styles.customerIcon}>
                            <FontAwesome name="user" size={20} color="#2E7D32" />
                        </View>
                        <View style={styles.customerInfo}>
                            <Text style={styles.customerLabel}>ग्राहक</Text>
                            <Text style={styles.customerName}>{customerName}</Text>
                        </View>
                        <FontAwesome name="chevron-right" size={16} color="#9E9E9E" />
                    </TouchableOpacity>

                    {/* Items List */}
                    <View style={styles.section}>
                        <View style={styles.sectionHeader}>
                            <Text style={styles.sectionTitle}>प्रोडक्ट्स</Text>
                            <TouchableOpacity
                                style={styles.addButton}
                                onPress={() => setShowProductPicker(true)}
                            >
                                <FontAwesome name="plus" size={14} color="#FFFFFF" />
                                <Text style={styles.addButtonText}>जोड़ें</Text>
                            </TouchableOpacity>
                        </View>

                        {items.length === 0 ? (
                            <View style={styles.emptyItems}>
                                <FontAwesome name="shopping-cart" size={32} color="#BDBDBD" />
                                <Text style={styles.emptyText}>प्रोडक्ट जोड़ने के लिए "जोड़ें" बटन दबाएं</Text>
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
                                            <FontAwesome name="minus" size={12} color="#757575" />
                                        </TouchableOpacity>
                                        <Text style={styles.qtyText}>{item.quantity}</Text>
                                        <TouchableOpacity
                                            style={styles.qtyButton}
                                            onPress={() => updateItemQuantity(index, item.quantity + 1)}
                                        >
                                            <FontAwesome name="plus" size={12} color="#757575" />
                                        </TouchableOpacity>
                                    </View>
                                    <Text style={styles.itemTotal}>₹{item.total}</Text>
                                </View>
                            ))
                        )}
                    </View>

                    {/* Discount */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>छूट (₹)</Text>
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
                            placeholderTextColor="#9E9E9E"
                            keyboardType="numeric"
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

                    {/* Amount Paid */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>भुगतान राशि (₹)</Text>
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
                            placeholderTextColor="#9E9E9E"
                            keyboardType="numeric"
                            editable={!!selectedCustomerId}
                        />
                    </View>

                    {/* Total Summary */}
                    <View style={styles.totalCard}>
                        <View style={styles.totalRow}>
                            <Text style={styles.totalLabel}>उप-योग</Text>
                            <Text style={styles.totalValue}>₹{subtotal.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={styles.totalRow}>
                            <Text style={styles.totalLabel}>छूट</Text>
                            <Text style={styles.discountValue}>-₹{discountAmount.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={[styles.totalRow, styles.grandTotalRow]}>
                            <Text style={styles.grandTotalLabel}>कुल</Text>
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
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <Text style={styles.submitButtonText}>बिक्री पूरी करें</Text>
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
                            <Text style={styles.pickerTitle}>प्रोडक्ट चुनें</Text>
                            <TouchableOpacity onPress={() => setShowProductPicker(false)}>
                                <FontAwesome name="times" size={20} color="#757575" />
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
                                <Text style={styles.emptyPicker}>कोई प्रोडक्ट नहीं है। पहले प्रोडक्ट जोड़ें।</Text>
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
                            <Text style={styles.pickerTitle}>ग्राहक चुनें</Text>
                            <TouchableOpacity onPress={() => setShowCustomerPicker(false)}>
                                <FontAwesome name="times" size={20} color="#757575" />
                            </TouchableOpacity>
                        </View>
                        <ScrollView>
                            <TouchableOpacity
                                style={styles.pickerItem}
                                onPress={() => {
                                    setSelectedCustomerId(undefined);
                                    setCustomerName('वॉक-इन ग्राहक');
                                    setShowCustomerPicker(false);
                                }}
                            >
                                <Text style={styles.pickerItemName}>वॉक-इन ग्राहक</Text>
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
    customerSelector: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        padding: 16,
        borderRadius: 12,
        marginBottom: 16,
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
        color: '#212121',
    },
    addButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        backgroundColor: '#2E7D32',
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 8,
    },
    addButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    emptyItems: {
        backgroundColor: '#FFFFFF',
        padding: 32,
        borderRadius: 12,
        alignItems: 'center',
    },
    emptyText: {
        fontSize: 14,
        color: '#757575',
        marginTop: 12,
        textAlign: 'center',
    },
    itemCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
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
        color: '#212121',
    },
    itemPrice: {
        fontSize: 12,
        color: '#757575',
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
        backgroundColor: '#F5F5F5',
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
        color: '#2E7D32',
        minWidth: 60,
        textAlign: 'right',
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
        backgroundColor: '#FFFFFF',
    },
    inputDisabled: {
        backgroundColor: '#F5F5F5',
        color: '#9E9E9E',
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
    totalCard: {
        backgroundColor: '#FFFFFF',
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
        color: '#757575',
    },
    totalValue: {
        fontSize: 14,
        color: '#212121',
    },
    discountValue: {
        fontSize: 14,
        color: '#D32F2F',
    },
    grandTotalRow: {
        borderTopWidth: 1,
        borderTopColor: '#E0E0E0',
        paddingTop: 12,
        marginTop: 4,
        marginBottom: 0,
    },
    grandTotalLabel: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
    },
    grandTotalValue: {
        fontSize: 20,
        fontWeight: '700',
        color: '#2E7D32',
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
