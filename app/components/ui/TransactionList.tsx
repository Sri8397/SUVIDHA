import React, { useState, useMemo } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { Transaction } from '@/types';
import { useData } from '@/contexts/DataContext';

type FilterType = 'all' | 'last10' | 'today' | 'week';

const getTransactionIcon = (type: Transaction['type']): 'shopping-cart' | 'minus-circle' | 'cube' | 'check-circle' | 'arrow-circle-up' | 'circle' => {
    switch (type) {
        case 'SALE':
            return 'shopping-cart';
        case 'EXPENSE':
            return 'minus-circle';
        case 'PRODUCT_ADD':
            return 'cube';
        case 'PAYMENT_RECEIVED':
            return 'check-circle';
        case 'PAYMENT_GIVEN':
            return 'arrow-circle-up';
        default:
            return 'circle';
    }
};

const getTransactionColor = (type: Transaction['type']) => {
    switch (type) {
        case 'SALE':
        case 'PAYMENT_RECEIVED':
            return '#2E7D32'; // Green
        case 'EXPENSE':
        case 'PAYMENT_GIVEN':
            return '#D32F2F'; // Red
        case 'PRODUCT_ADD':
            return '#1976D2'; // Blue
        default:
            return '#757575';
    }
};

const getTransactionLabel = (type: Transaction['type']) => {
    switch (type) {
        case 'SALE': return 'बिक्री';
        case 'EXPENSE': return 'खर्च';
        case 'PRODUCT_ADD': return 'स्टॉक';
        case 'PAYMENT_RECEIVED': return 'भुगतान';
        case 'PAYMENT_GIVEN': return 'दिया गया';
        default: return 'अन्य';
    }
};

const formatDate = (dateString: string): string => {
    const today = new Date().toISOString().split('T')[0];
    if (dateString === today) {
        return 'आज';
    }
    const options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short' };
    return new Date(dateString).toLocaleDateString('hi-IN', options);
};

interface TransactionListProps {
    showFilters?: boolean;
}

export const TransactionList = ({ showFilters = true }: TransactionListProps) => {
    const { transactions } = useData();
    const [filter, setFilter] = useState<FilterType>('all');

    const filteredTransactions = useMemo(() => {
        const sorted = [...transactions].sort((a, b) => b.timestamp - a.timestamp);
        const today = new Date().toISOString().split('T')[0];
        const weekAgo = new Date();
        weekAgo.setDate(weekAgo.getDate() - 7);
        const weekAgoStr = weekAgo.toISOString().split('T')[0];

        switch (filter) {
            case 'last10':
                return sorted.slice(0, 10);
            case 'today':
                return sorted.filter(t => t.date === today);
            case 'week':
                return sorted.filter(t => t.date >= weekAgoStr);
            default:
                return sorted;
        }
    }, [transactions, filter]);

    const renderItem = (item: Transaction) => {
        const isPositive = item.type === 'SALE' || item.type === 'PAYMENT_RECEIVED';
        const amountPrefix = isPositive ? '+' : item.type === 'PRODUCT_ADD' ? '' : '-';
        const color = getTransactionColor(item.type);
        const label = getTransactionLabel(item.type);

        return (
            <View key={item.id} style={styles.row}>
                <View style={[styles.iconContainer, { backgroundColor: color + '20' }]}>
                    <FontAwesome name={getTransactionIcon(item.type)} size={16} color={color} />
                </View>
                <View style={styles.content}>
                    <View style={styles.titleRow}>
                        <Text style={styles.typeLabel}>{label}</Text>
                        <Text style={styles.separator}> - </Text>
                        <Text style={styles.description} numberOfLines={1}>{item.description}</Text>
                    </View>
                    <Text style={styles.date}>{formatDate(item.date)}</Text>
                </View>
                <View style={styles.amountContainer}>
                    {item.amount > 0 && (
                        <Text style={[styles.amount, { color }]}>
                            {amountPrefix}₹{item.amount.toLocaleString('en-IN')}
                        </Text>
                    )}
                </View>
            </View>
        );
    };

    if (transactions.length === 0) {
        return (
            <View style={styles.emptyContainer}>
                <FontAwesome name="exchange" size={48} color="#BDBDBD" />
                <Text style={styles.emptyText}>कोई लेन-देन नहीं मिला</Text>
                <Text style={styles.emptySubtext}>बिक्री या खर्च जोड़ें</Text>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            {showFilters && (
                <View style={styles.filterRow}>
                    {[
                        { key: 'all', label: 'सभी' },
                        { key: 'last10', label: 'पिछले 10' },
                        { key: 'today', label: 'आज' },
                        { key: 'week', label: 'हफ्ता' },
                    ].map(f => (
                        <TouchableOpacity
                            key={f.key}
                            style={[styles.filterChip, filter === f.key && styles.filterChipActive]}
                            onPress={() => setFilter(f.key as FilterType)}
                        >
                            <Text style={[styles.filterText, filter === f.key && styles.filterTextActive]}>
                                {f.label}
                            </Text>
                        </TouchableOpacity>
                    ))}
                </View>
            )}

            <Text style={styles.headerTitle}>
                {filter === 'all' ? 'सभी लेन-देन' :
                    filter === 'last10' ? 'पिछले 10 लेन-देन' :
                        filter === 'today' ? 'आज के लेन-देन' : 'इस हफ्ते के लेन-देन'}
                {' '}({filteredTransactions.length})
            </Text>

            {filteredTransactions.length === 0 ? (
                <View style={styles.noResults}>
                    <Text style={styles.noResultsText}>इस फ़िल्टर में कोई लेन-देन नहीं</Text>
                </View>
            ) : (
                filteredTransactions.map(item => (
                    <View key={item.id}>
                        {renderItem(item)}
                        <View style={styles.divider} />
                    </View>
                ))
            )}
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        padding: 16,
        marginHorizontal: 16,
        marginBottom: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 2,
        elevation: 2,
    },
    filterRow: {
        flexDirection: 'row',
        gap: 8,
        marginBottom: 16,
    },
    filterChip: {
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 16,
        backgroundColor: '#F5F5F5',
        borderWidth: 1,
        borderColor: '#E0E0E0',
    },
    filterChipActive: {
        backgroundColor: '#2E7D32',
        borderColor: '#2E7D32',
    },
    filterText: {
        fontSize: 12,
        color: '#757575',
        fontWeight: '500',
    },
    filterTextActive: {
        color: '#FFFFFF',
    },
    headerTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 16,
    },
    row: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 12,
    },
    iconContainer: {
        width: 40,
        height: 40,
        borderRadius: 20,
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 12,
    },
    content: {
        flex: 1,
    },
    titleRow: {
        flexDirection: 'row',
        alignItems: 'center',
        flexWrap: 'wrap',
    },
    typeLabel: {
        fontSize: 13,
        fontWeight: '700',
        color: '#424242',
    },
    separator: {
        fontSize: 13,
        color: '#9E9E9E',
    },
    description: {
        fontSize: 13,
        color: '#757575',
        flex: 1,
    },
    date: {
        fontSize: 12,
        color: '#9E9E9E',
        marginTop: 2,
    },
    amountContainer: {
        alignItems: 'flex-end',
        marginLeft: 8,
    },
    amount: {
        fontSize: 14,
        fontWeight: '700',
    },
    divider: {
        height: 1,
        backgroundColor: '#F5F5F5',
    },
    emptyContainer: {
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        padding: 48,
        marginHorizontal: 16,
        marginBottom: 16,
        alignItems: 'center',
    },
    emptyText: {
        fontSize: 16,
        color: '#757575',
        marginTop: 16,
    },
    emptySubtext: {
        fontSize: 14,
        color: '#9E9E9E',
        marginTop: 4,
    },
    noResults: {
        padding: 24,
        alignItems: 'center',
    },
    noResultsText: {
        color: '#9E9E9E',
    },
});
