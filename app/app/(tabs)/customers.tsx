import React, { useState, useMemo } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, Linking, ActivityIndicator } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useTranslation } from 'react-i18next';
import { Header } from '@/components/ui/header';
import { FAB } from '@/components/ui/fab';
import { TransliterationInput } from '@/components/ui/TransliterationInput';
import { useData } from '@/contexts/DataContext';
import { AddCustomerForm } from '@/components/forms/AddCustomerForm';
import { AddSaleForm } from '@/components/forms/AddSaleForm';
import { AddExpenseForm } from '@/components/forms/AddExpenseForm';
import { AddProductForm } from '@/components/forms/AddProductForm';
import { AddIncomeForm } from '@/components/forms/AddIncomeForm';
import { AddLoanForm } from '@/components/forms/AddLoanForm';
import { ReceivePaymentForm } from '@/components/forms/ReceivePaymentForm';
import { useAppMode } from '@/contexts/ModeContext';
import { Customer } from '@/types';
import { Colors } from '@/constants/theme';

export default function CustomersScreen() {
    const { t } = useTranslation();
    const { isLoading, customers } = useData();
    const { appMode } = useAppMode();
    const isPersonalMode = appMode === 'personal';
    const [searchQuery, setSearchQuery] = useState('');

    // Form states
    const [showCustomerForm, setShowCustomerForm] = useState(false);
    const [showSaleForm, setShowSaleForm] = useState(false);
    const [showExpenseForm, setShowExpenseForm] = useState(false);
    const [showProductForm, setShowProductForm] = useState(false);
    const [showPaymentForm, setShowPaymentForm] = useState(false);
    const [showIncomeForm, setShowIncomeForm] = useState(false);
    const [showLoanForm, setShowLoanForm] = useState(false);
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
                <ActivityIndicator size="large" color={Colors.light.primary} />
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />

            {/* Header */}
            <Header title={t(isPersonalMode ? 'personal.people' : 'customers.title')} showLanguageToggle={true} />

            {/* Search Bar */}
            <View style={styles.searchContainer}>
                <FontAwesome name="search" size={18} color={Colors.light.textTertiary} />
                <TransliterationInput
                    style={styles.searchInput}
                    placeholder={t(isPersonalMode ? 'personal.friend.searchPlaceholder' : 'customers.searchPlaceholder')}
                    placeholderTextColor={Colors.light.textTertiary}
                    value={searchQuery}
                    onChangeText={setSearchQuery}
                />
            </View>

            {/* Customer/Friend Count */}
            <View style={styles.countContainer}>
                <Text style={styles.countText}>
                    {t(isPersonalMode ? 'personal.friend.friends_count' : 'customers.customers_count', { count: filteredCustomers.length })}
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
                                <View style={styles.customerNameRow}>
                                    <Text style={styles.customerName}>{customer.name}</Text>
                                    {isPersonalMode && customer.outstanding > 0 && (
                                        <Text style={styles.inlineOutstanding}>
                                            (₹{customer.outstanding.toLocaleString('en-IN')})
                                        </Text>
                                    )}
                                </View>
                                <Text style={styles.customerPhone}>{customer.phone}</Text>
                            </View>
                            <TouchableOpacity
                                style={styles.moreButton}
                                onPress={() => handleEdit(customer)}
                            >
                                <FontAwesome name="pencil" size={16} color={Colors.light.textSecondary} />
                            </TouchableOpacity>
                        </View>

                        {customer.outstanding > 0 && !isPersonalMode && (
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
                                <FontAwesome name="phone" size={14} color={Colors.light.primary} />
                                <Text style={styles.callButtonText}>{t('customers.call')}</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.messageButton}
                                onPress={() => handleMessage(customer.phone)}
                            >
                                <FontAwesome name="whatsapp" size={14} color={Colors.light.white} />
                                <Text style={styles.messageButtonText}>WhatsApp</Text>
                            </TouchableOpacity>
                            {customer.outstanding > 0 && !isPersonalMode && (
                                <TouchableOpacity
                                    style={styles.clearBakayaButton}
                                    onPress={() => handleReceivePayment(customer)}
                                >
                                    <FontAwesome name="money" size={14} color={Colors.light.secondary} />
                                    <Text style={styles.clearBakayaButtonText}>{t('customers.collectPayment')}</Text>
                                </TouchableOpacity>
                            )}
                        </View>
                    </View>
                ))}
                {filteredCustomers.length === 0 && (
                    <View style={styles.emptyState}>
                        <FontAwesome name="users" size={48} color={Colors.light.textTertiary} />
                        <Text style={styles.emptyStateText}>
                            {customers.length === 0
                                ? t(isPersonalMode ? 'personal.friend.noFriends' : 'customers.noCustomers')
                                : t(isPersonalMode ? 'personal.friend.noFriendsFound' : 'customers.noCustomersFound')}
                        </Text>
                        {customers.length === 0 && (
                            <TouchableOpacity
                                style={styles.addFirstButton}
                                onPress={() => setShowCustomerForm(true)}
                            >
                                <FontAwesome name="plus" size={14} color={Colors.light.white} />
                                <Text style={styles.addFirstButtonText}>{t(isPersonalMode ? 'personal.friend.addFriend' : 'customers.addCustomer')}</Text>
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
                onAddIncome={() => setShowIncomeForm(true)}
                onAddLoan={() => setShowLoanForm(true)}
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
            <AddIncomeForm visible={showIncomeForm} onClose={() => setShowIncomeForm(false)} />
            <AddLoanForm
                visible={showLoanForm}
                onClose={() => setShowLoanForm(false)}
                onAddPerson={() => {
                    setShowLoanForm(false);
                    setTimeout(() => setShowCustomerForm(true), 500);
                }}
            />
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
        backgroundColor: Colors.light.background,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: Colors.light.white,
    },
    searchContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: Colors.light.white,
        marginHorizontal: 16,
        marginVertical: 12,
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 12,
        gap: 12,
        borderWidth: 1,
        borderColor: Colors.light.border,
    },
    searchInput: {
        flex: 1,
        fontSize: 16,
        color: Colors.light.text,
    },
    countContainer: {
        paddingHorizontal: 16,
        paddingBottom: 8,
    },
    countText: {
        fontSize: 14,
        color: Colors.light.textSecondary,
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 100,
        paddingTop: 8,
    },
    customerCard: {
        backgroundColor: Colors.light.white,
        marginHorizontal: 16,
        marginBottom: 12,
        borderRadius: 12,
        padding: 16,
        borderLeftWidth: 4,
        borderLeftColor: Colors.light.primary,
        shadowColor: Colors.light.shadow,
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
        backgroundColor: Colors.light.primaryLight,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    avatarText: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.primary,
    },
    customerInfo: {
        flex: 1,
    },
    customerName: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
    },
    customerNameRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 2,
        gap: 8,
    },
    inlineOutstanding: {
        fontSize: 14,
        color: Colors.light.error,
        fontWeight: '600',
    },
    customerPhone: {
        fontSize: 14,
        color: Colors.light.textSecondary,
    },
    moreButton: {
        padding: 8,
    },
    outstandingBadge: {
        backgroundColor: Colors.light.secondaryLight,
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 8,
        marginTop: 12,
    },
    customerOutstanding: {
        fontSize: 14,
        color: Colors.light.textSecondary,
    },
    outstandingAmount: {
        fontWeight: '700',
        color: Colors.light.secondary,
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
        borderColor: Colors.light.primary,
    },
    callButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.primary,
    },
    messageButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
        paddingVertical: 10,
        borderRadius: 8,
        backgroundColor: Colors.light.success,
    },
    messageButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.white,
    },
    emptyState: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 80,
    },
    emptyStateText: {
        fontSize: 16,
        color: Colors.light.textSecondary,
        marginTop: 16,
        textAlign: 'center',
        paddingHorizontal: 32,
    },
    addFirstButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        backgroundColor: Colors.light.primary,
        paddingHorizontal: 20,
        paddingVertical: 12,
        borderRadius: 8,
        marginTop: 20,
    },
    addFirstButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.white,
    },
    clearBakayaButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
        paddingVertical: 10,
        borderRadius: 8,
        backgroundColor: Colors.light.secondaryLight,
        borderWidth: 1,
        borderColor: Colors.light.secondary,
    },
    clearBakayaButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.secondary,
    },
});
