import React, { useState, useMemo } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, TextInput, StyleSheet, Linking, Alert, ActivityIndicator } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { Header } from '@/components/ui/header';
import { FAB } from '@/components/ui/fab';
import { useData } from '@/contexts/DataContext';
import { AddCustomerForm } from '@/components/forms/AddCustomerForm';
import { AddSaleForm } from '@/components/forms/AddSaleForm';
import { AddExpenseForm } from '@/components/forms/AddExpenseForm';
import { AddProductForm } from '@/components/forms/AddProductForm';
import { ReceivePaymentForm } from '@/components/forms/ReceivePaymentForm';
import { Customer } from '@/types';

export default function CustomersScreen() {
    const { isLoading, customers, deleteCustomer, updateCustomer } = useData();
    const [searchQuery, setSearchQuery] = useState('');

    // Form states
    const [showCustomerForm, setShowCustomerForm] = useState(false);
    const [showSaleForm, setShowSaleForm] = useState(false);
    const [showExpenseForm, setShowExpenseForm] = useState(false);
    const [showProductForm, setShowProductForm] = useState(false);
    const [showPaymentForm, setShowPaymentForm] = useState(false);
    const [editingCustomer, setEditingCustomer] = useState<Customer | undefined>();
    const [paymentCustomer, setPaymentCustomer] = useState<Customer | undefined>();

    // Filter customers
    const filteredCustomers = useMemo(() => {
        return customers.filter((customer) =>
            customer.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            customer.phone.includes(searchQuery)
        );
    }, [customers, searchQuery]);

    const handleCall = (phone: string) => {
        Linking.openURL(`tel:${phone}`);
    };

    const handleMessage = (phone: string) => {
        Linking.openURL(`https://wa.me/91${phone}`);
    };

    const handleEdit = (customer: Customer) => {
        setEditingCustomer(customer);
        setShowCustomerForm(true);
    };

    const handleDelete = (customer: Customer) => {
        Alert.alert(
            'ग्राहक हटाएं',
            `क्या आप "${customer.name}" को हटाना चाहते हैं?`,
            [
                { text: 'रद्द करें', style: 'cancel' },
                {
                    text: 'हटाएं',
                    style: 'destructive',
                    onPress: async () => {
                        const success = await deleteCustomer(customer.id);
                        if (success) {
                            Alert.alert('सफल', 'ग्राहक हटा दिया गया');
                        }
                    }
                }
            ]
        );
    };

    const handleReceivePayment = (customer: Customer) => {
        setPaymentCustomer(customer);
        setShowPaymentForm(true);
    };

    const handleCloseCustomerForm = () => {
        setShowCustomerForm(false);
        setEditingCustomer(undefined);
    };

    const handleClosePaymentForm = () => {
        setShowPaymentForm(false);
        setPaymentCustomer(undefined);
    };

    if (isLoading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#2E7D32" />
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            {/* Header */}
            <Header title="ग्राहक" showLanguageToggle={true} />

            {/* Search Bar */}
            <View style={styles.searchContainer}>
                <FontAwesome name="search" size={18} color="#9E9E9E" />
                <TextInput
                    style={styles.searchInput}
                    placeholder="नाम या फोन से खोजें..."
                    placeholderTextColor="#9E9E9E"
                    value={searchQuery}
                    onChangeText={setSearchQuery}
                />
            </View>

            {/* Customer Count */}
            <View style={styles.countContainer}>
                <Text style={styles.countText}>
                    {filteredCustomers.length} ग्राहक
                </Text>
            </View>

            {/* Customer List */}
            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {filteredCustomers.map((customer) => (
                    <View key={customer.id} style={styles.customerCard}>
                        <View style={styles.customerHeader}>
                            <View style={styles.customerAvatar}>
                                <Text style={styles.avatarText}>
                                    {customer.name.charAt(0).toUpperCase()}
                                </Text>
                            </View>
                            <View style={styles.customerInfo}>
                                <Text style={styles.customerName}>{customer.name}</Text>
                                <Text style={styles.customerPhone}>{customer.phone}</Text>
                            </View>
                            <TouchableOpacity
                                style={styles.moreButton}
                                onPress={() => handleEdit(customer)}
                            >
                                <FontAwesome name="pencil" size={16} color="#757575" />
                            </TouchableOpacity>
                        </View>

                        {customer.outstanding > 0 && (
                            <View style={styles.outstandingBadge}>
                                <Text style={styles.customerOutstanding}>
                                    बकाया: <Text style={styles.outstandingAmount}>₹{customer.outstanding.toLocaleString('en-IN')}</Text>
                                </Text>
                            </View>
                        )}

                        <View style={styles.customerActions}>
                            <TouchableOpacity
                                style={styles.callButton}
                                onPress={() => handleCall(customer.phone)}
                            >
                                <FontAwesome name="phone" size={14} color="#2E7D32" />
                                <Text style={styles.callButtonText}>कॉल</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.messageButton}
                                onPress={() => handleMessage(customer.phone)}
                            >
                                <FontAwesome name="whatsapp" size={14} color="#FFFFFF" />
                                <Text style={styles.messageButtonText}>WhatsApp</Text>
                            </TouchableOpacity>
                            {customer.outstanding > 0 && (
                                <TouchableOpacity
                                    style={styles.clearBakayaButton}
                                    onPress={() => handleReceivePayment(customer)}
                                >
                                    <FontAwesome name="money" size={14} color="#E65100" />
                                    <Text style={styles.clearBakayaButtonText}>बकाया लें</Text>
                                </TouchableOpacity>
                            )}
                        </View>
                    </View>
                ))}
                {filteredCustomers.length === 0 && (
                    <View style={styles.emptyState}>
                        <FontAwesome name="users" size={48} color="#BDBDBD" />
                        <Text style={styles.emptyStateText}>
                            {customers.length === 0
                                ? 'कोई ग्राहक नहीं है। नया ग्राहक जोड़ें।'
                                : 'कोई ग्राहक नहीं मिला'}
                        </Text>
                        {customers.length === 0 && (
                            <TouchableOpacity
                                style={styles.addFirstButton}
                                onPress={() => setShowCustomerForm(true)}
                            >
                                <FontAwesome name="plus" size={14} color="#FFFFFF" />
                                <Text style={styles.addFirstButtonText}>ग्राहक जोड़ें</Text>
                            </TouchableOpacity>
                        )}
                    </View>
                )
                }
            </ScrollView >

            {/* FAB */}
            < FAB
                onNewSale={() => setShowSaleForm(true)}
                onNewExpense={() => setShowExpenseForm(true)}
                onAddProduct={() => setShowProductForm(true)}
                onAddCustomer={() => setShowCustomerForm(true)}
            />

            {/* Forms */}
            <AddCustomerForm
                visible={showCustomerForm}
                onClose={handleCloseCustomerForm}
                editCustomer={editingCustomer}
            />
            <AddSaleForm visible={showSaleForm} onClose={() => setShowSaleForm(false)} />
            <AddExpenseForm visible={showExpenseForm} onClose={() => setShowExpenseForm(false)} />
            <AddProductForm visible={showProductForm} onClose={() => setShowProductForm(false)} />
            <ReceivePaymentForm
                visible={showPaymentForm}
                onClose={handleClosePaymentForm}
                customer={paymentCustomer}
            />
        </View >
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F5F5F5',
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
    },
    searchContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginVertical: 12,
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 12,
        gap: 12,
        borderWidth: 1,
        borderColor: '#E0E0E0',
    },
    searchInput: {
        flex: 1,
        fontSize: 16,
        color: '#212121',
    },
    countContainer: {
        paddingHorizontal: 16,
        paddingBottom: 8,
    },
    countText: {
        fontSize: 14,
        color: '#757575',
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 100,
        paddingTop: 8,
    },
    customerCard: {
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginBottom: 12,
        borderRadius: 12,
        padding: 16,
        borderLeftWidth: 4,
        borderLeftColor: '#2E7D32',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    customerHeader: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    customerAvatar: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: '#E8F5E9',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    avatarText: {
        fontSize: 18,
        fontWeight: '700',
        color: '#2E7D32',
    },
    customerInfo: {
        flex: 1,
    },
    customerName: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 2,
    },
    customerPhone: {
        fontSize: 14,
        color: '#757575',
    },
    moreButton: {
        padding: 8,
    },
    outstandingBadge: {
        backgroundColor: '#FFF8E1',
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 8,
        marginTop: 12,
    },
    customerOutstanding: {
        fontSize: 14,
        color: '#757575',
    },
    outstandingAmount: {
        fontWeight: '700',
        color: '#F57C00',
    },
    customerActions: {
        flexDirection: 'row',
        gap: 12,
        marginTop: 16,
    },
    callButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
        paddingVertical: 10,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: '#2E7D32',
    },
    callButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#2E7D32',
    },
    messageButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
        paddingVertical: 10,
        borderRadius: 8,
        backgroundColor: '#25D366',
    },
    messageButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    emptyState: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 80,
    },
    emptyStateText: {
        fontSize: 16,
        color: '#757575',
        marginTop: 16,
        textAlign: 'center',
        paddingHorizontal: 32,
    },
    addFirstButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        backgroundColor: '#2E7D32',
        paddingHorizontal: 20,
        paddingVertical: 12,
        borderRadius: 8,
        marginTop: 20,
    },
    addFirstButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    clearBakayaButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
        paddingVertical: 10,
        borderRadius: 8,
        backgroundColor: '#FFF3E0',
        borderWidth: 1,
        borderColor: '#FFB74D',
    },
    clearBakayaButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#E65100',
    },
});
