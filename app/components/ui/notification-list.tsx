import React from 'react';
import { View, Text, Modal, TouchableOpacity, StyleSheet, ScrollView, ActivityIndicator } from 'react-native';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useData } from '@/contexts/DataContext';
import { Transaction } from '@/types';

interface NotificationListProps {
    visible: boolean;
    onClose: () => void;
}

// Helper to format time ago
const formatTimeAgo = (timestamp: number): string => {
    const now = Date.now();
    const diff = now - timestamp;

    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (minutes < 1) return 'अभी';
    if (minutes < 60) return `${minutes} मिनट पहले`;
    if (hours < 24) return `${hours} घंटे पहले`;
    return `${days} दिन पहले`;
};

// Get icon and colors based on transaction type
const getTransactionStyle = (type: Transaction['type']) => {
    switch (type) {
        case 'SALE':
            return {
                iconName: 'shopping-cart' as const,
                iconColor: '#E3F2FD',
                iconTextColor: '#1976D2',
            };
        case 'EXPENSE':
            return {
                iconName: 'money' as const,
                iconColor: '#FFF3E0',
                iconTextColor: '#F57C00',
            };
        case 'PRODUCT_ADD':
            return {
                iconName: 'cube' as const,
                iconColor: '#E8F5E9',
                iconTextColor: '#2E7D32',
            };
        case 'PAYMENT_RECEIVED':
            return {
                iconName: 'rupee' as const,
                iconColor: '#E8F5E9',
                iconTextColor: '#2E7D32',
            };
        case 'PAYMENT_GIVEN':
            return {
                iconName: 'arrow-right' as const,
                iconColor: '#FFEBEE',
                iconTextColor: '#D32F2F',
            };
        default:
            return {
                iconName: 'bell' as const,
                iconColor: '#F5F5F5',
                iconTextColor: '#757575',
            };
    }
};

export function NotificationList({ visible, onClose }: NotificationListProps) {
    const { isLoading, getVisibleNotifications, dismissNotification, dismissAllNotifications } = useData();

    // Get visible notifications from context (shared across all pages)
    const visibleTransactions = getVisibleNotifications();

    return (
        <Modal
            animationType="fade"
            transparent={true}
            visible={visible}
            onRequestClose={onClose}
        >
            <TouchableOpacity
                style={styles.centeredView}
                activeOpacity={1}
                onPress={onClose}
            >
                <TouchableOpacity activeOpacity={1} style={styles.modalView}>
                    <View style={styles.header}>
                        <Text style={styles.modalTitle}>सूचनाएँ</Text>
                        <View style={styles.headerActions}>
                            {visibleTransactions.length > 0 && (
                                <TouchableOpacity onPress={dismissAllNotifications} style={styles.clearAllButton}>
                                    <Text style={styles.clearAllText}>सभी हटाएं</Text>
                                </TouchableOpacity>
                            )}
                            <TouchableOpacity onPress={onClose}>
                                <MaterialIcons name="close" size={24} color="#757575" />
                            </TouchableOpacity>
                        </View>
                    </View>

                    <ScrollView contentContainerStyle={styles.listContainer}>
                        {isLoading ? (
                            <View style={styles.emptyState}>
                                <ActivityIndicator size="large" color="#2E7D32" />
                            </View>
                        ) : visibleTransactions.length === 0 ? (
                            <View style={styles.emptyState}>
                                <FontAwesome name="bell-o" size={48} color="#BDBDBD" />
                                <Text style={styles.emptyTitle}>कोई सूचना नहीं</Text>
                                <Text style={styles.emptySubtitle}>नई सूचनाएं यहाँ दिखाई देंगी</Text>
                            </View>
                        ) : (
                            visibleTransactions.map((item, index) => {
                                const style = getTransactionStyle(item.type);
                                return (
                                    <View
                                        key={item.id}
                                        style={[
                                            styles.notificationItem,
                                            index !== visibleTransactions.length - 1 && styles.borderBottom
                                        ]}
                                    >
                                        <View style={[styles.iconContainer, { backgroundColor: style.iconColor }]}>
                                            <FontAwesome name={style.iconName} size={18} color={style.iconTextColor} />
                                        </View>
                                        <View style={styles.textContainer}>
                                            <Text style={styles.title} numberOfLines={2}>{item.description}</Text>
                                            <View style={styles.metaRow}>
                                                <Text style={styles.amount}>₹{item.amount.toLocaleString('en-IN')}</Text>
                                                <Text style={styles.time}>{formatTimeAgo(item.timestamp)}</Text>
                                            </View>
                                        </View>
                                        <TouchableOpacity
                                            style={styles.dismissButton}
                                            onPress={() => dismissNotification(item.id)}
                                            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                                        >
                                            <MaterialIcons name="close" size={18} color="#9E9E9E" />
                                        </TouchableOpacity>
                                    </View>
                                );
                            })
                        )}
                    </ScrollView>
                </TouchableOpacity>
            </TouchableOpacity>
        </Modal>
    );
}

const styles = StyleSheet.create({
    centeredView: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.5)',
        justifyContent: 'flex-start',
        paddingTop: 60,
    },
    modalView: {
        backgroundColor: 'white',
        marginHorizontal: 16,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.25,
        shadowRadius: 4,
        elevation: 5,
        maxHeight: 450,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#EEEEEE',
    },
    headerActions: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 16,
    },
    clearAllButton: {
        paddingVertical: 4,
        paddingHorizontal: 8,
    },
    clearAllText: {
        fontSize: 12,
        color: '#D32F2F',
        fontWeight: '500',
    },
    modalTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#000',
    },
    listContainer: {
        paddingVertical: 0,
    },
    notificationItem: {
        flexDirection: 'row',
        padding: 14,
        gap: 12,
        alignItems: 'center',
    },
    borderBottom: {
        borderBottomWidth: 1,
        borderBottomColor: '#EEEEEE',
    },
    iconContainer: {
        width: 40,
        height: 40,
        borderRadius: 20,
        justifyContent: 'center',
        alignItems: 'center',
    },
    textContainer: {
        flex: 1,
    },
    title: {
        fontSize: 14,
        color: '#212121',
        marginBottom: 4,
        lineHeight: 18,
    },
    metaRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
    },
    amount: {
        fontSize: 13,
        fontWeight: '600',
        color: '#424242',
    },
    time: {
        fontSize: 11,
        color: '#9E9E9E',
    },
    dismissButton: {
        padding: 4,
    },
    emptyState: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 48,
        paddingHorizontal: 24,
    },
    emptyTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#424242',
        marginTop: 16,
    },
    emptySubtitle: {
        fontSize: 14,
        color: '#757575',
        marginTop: 4,
        textAlign: 'center',
    },
});
