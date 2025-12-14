import React, { useState, useMemo } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { Transaction } from '@/types';
import { useData } from '@/contexts/DataContext';
import { useTranslation } from 'react-i18next';
import { Colors } from '@/constants/theme';

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
            return Colors.light.success; // Green
        case 'EXPENSE':
        case 'PAYMENT_GIVEN':
            return Colors.light.error; // Red
        case 'PRODUCT_ADD':
            return Colors.light.info; // Blue
        default:
            return Colors.light.textSecondary;
    }
};



const getTransactionLabel = (type: Transaction['type'], t: any) => {
    switch (type) {
        case 'SALE': return t('transactionType.sale');
        case 'EXPENSE': return t('transactionType.expense');
        case 'PRODUCT_ADD': return t('transactionType.productAdd');
        case 'PAYMENT_RECEIVED': return t('transactionType.paymentReceived');
        case 'PAYMENT_GIVEN': return t('transactionType.paymentGiven');
        default: return t('transactionType.other');
    }
};

const formatDate = (dateString: string, t: any): string => {
    const today = new Date().toISOString().split('T')[0];
    if (dateString === today) {
        return t('common.today');
    }
    const options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short' };
    return new Date(dateString).toLocaleDateString('hi-IN', options);
};

interface TransactionListProps {
    showFilters?: boolean;
    refreshing?: boolean;
    onRefresh?: () => void;
}

export const TransactionList = ({ showFilters = true, refreshing, onRefresh }: TransactionListProps) => {
    const { t } = useTranslation();
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
        const label = getTransactionLabel(item.type, t);

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
                    <Text style={styles.date}>{formatDate(item.date, t)}</Text>
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
                <FontAwesome name="exchange" size={48} color={Colors.light.textTertiary} />
                <Text style={styles.emptyText}>{t('transactions.empty.title')}</Text>
                <Text style={styles.emptySubtext}>{t('transactions.empty.subtitle')}</Text>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            {showFilters && (
                <View style={styles.filterRow}>
                    {[
                        { key: 'all', label: t('transactions.filter.all') },
                        { key: 'last10', label: t('transactions.filter.last10') },
                        { key: 'today', label: t('transactions.filter.today') },
                        { key: 'week', label: t('transactions.filter.week') },
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
                {filter === 'all' ? t('transactions.header.all') :
                    filter === 'last10' ? t('transactions.header.last10') :
                        filter === 'today' ? t('transactions.header.today') : t('transactions.header.week')}
                {' '}({filteredTransactions.length})
            </Text>

            {filteredTransactions.length === 0 ? (
                <View style={styles.noResults}>
                    <Text style={styles.noResultsText}>{t('transactions.noTransactionsFilter')}</Text>
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
        backgroundColor: Colors.light.card,
        borderRadius: 12,
        padding: 16,
        marginHorizontal: 16,
        marginBottom: 16,
        shadowColor: Colors.light.shadow,
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
        backgroundColor: Colors.light.background,
        borderWidth: 1,
        borderColor: Colors.light.border,
    },
    filterChipActive: {
        backgroundColor: Colors.light.primary,
        borderColor: Colors.light.primary,
    },
    filterText: {
        fontSize: 12,
        color: Colors.light.textSecondary,
        fontWeight: '500',
    },
    filterTextActive: {
        color: Colors.light.white,
    },
    headerTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: Colors.light.text,
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
        color: Colors.light.text,
    },
    separator: {
        fontSize: 13,
        color: Colors.light.textTertiary,
    },
    description: {
        fontSize: 13,
        color: Colors.light.textSecondary,
        flex: 1,
    },
    date: {
        fontSize: 12,
        color: Colors.light.textTertiary,
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
        backgroundColor: Colors.light.background,
    },
    emptyContainer: {
        backgroundColor: Colors.light.card,
        borderRadius: 12,
        padding: 48,
        marginHorizontal: 16,
        marginBottom: 16,
        alignItems: 'center',
    },
    emptyText: {
        fontSize: 16,
        color: Colors.light.textSecondary,
        marginTop: 16,
    },
    emptySubtext: {
        fontSize: 14,
        color: Colors.light.textTertiary,
        marginTop: 4,
    },
    noResults: {
        padding: 24,
        alignItems: 'center',
    },
    noResultsText: {
        color: Colors.light.textTertiary,
    },
});
