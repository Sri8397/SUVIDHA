import React, { useState } from 'react';
import { View, Text, ScrollView, Image, StatusBar, TouchableOpacity, TextInput, StyleSheet, Linking } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { Header } from '@/components/ui/header';
import { FAB } from '@/components/ui/fab';

// Mock data
const mockCustomers = [
    { id: '1', name: 'रमेश कुमार', phone: '9876543210', outstanding: 1200 },
    { id: '2', name: 'सुनीता शर्मा', phone: '9988776655', outstanding: 500 },
    { id: '3', name: 'विक्रम सिंह', phone: '9123456789', outstanding: 2500 },
];

export default function CustomersScreen() {
    const [searchQuery, setSearchQuery] = useState('');

    const filteredCustomers = mockCustomers.filter((customer) =>
        customer.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        customer.phone.includes(searchQuery)
    );

    const handleCall = (phone: string) => {
        Linking.openURL(`tel:${phone}`);
    };

    const handleMessage = (phone: string) => {
        Linking.openURL(`https://wa.me/91${phone}`);
    };

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
                    placeholder="खोजें..."
                    placeholderTextColor="#9E9E9E"
                    value={searchQuery}
                    onChangeText={setSearchQuery}
                />
            </View>

            {/* Customer List */}
            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {filteredCustomers.map((customer) => (
                    <View key={customer.id} style={styles.customerCard}>
                        <Text style={styles.customerName}>{customer.name}</Text>
                        <Text style={styles.customerPhone}>{customer.phone}</Text>
                        <Text style={styles.customerOutstanding}>
                            कुल बकाया: <Text style={styles.outstandingAmount}>₹ {customer.outstanding.toLocaleString('en-IN')}</Text>
                        </Text>

                        <View style={styles.customerActions}>
                            <TouchableOpacity
                                style={styles.callButton}
                                onPress={() => handleCall(customer.phone)}
                            >
                                <FontAwesome name="phone" size={14} color="#2E7D32" />
                                <Text style={styles.callButtonText}>कॉल करें</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.messageButton}
                                onPress={() => handleMessage(customer.phone)}
                            >
                                <FontAwesome name="whatsapp" size={14} color="#FFFFFF" />
                                <Text style={styles.messageButtonText}>संदेश भेजें</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                ))}

                {filteredCustomers.length === 0 && (
                    <View style={styles.emptyState}>
                        <FontAwesome name="users" size={48} color="#BDBDBD" />
                        <Text style={styles.emptyStateText}>कोई ग्राहक नहीं मिला</Text>
                    </View>
                )}
            </ScrollView>

            {/* FAB */}
            <FAB
                onNewSale={() => console.log('New Sale')}
                onNewExpense={() => console.log('New Expense')}
                onAddProduct={() => console.log('Add Product')}
                onAddCustomer={() => console.log('Add Customer')}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F5F5F5',
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
    customerName: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 4,
    },
    customerPhone: {
        fontSize: 14,
        color: '#757575',
        marginBottom: 4,
    },
    customerOutstanding: {
        fontSize: 14,
        color: '#757575',
        marginBottom: 16,
    },
    outstandingAmount: {
        fontWeight: '700',
        color: '#212121',
    },
    customerActions: {
        flexDirection: 'row',
        gap: 12,
    },
    callButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        paddingHorizontal: 16,
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
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        paddingHorizontal: 16,
        paddingVertical: 10,
        borderRadius: 8,
        backgroundColor: '#2E7D32',
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
    },
});
