import React from 'react';
import { View, Text, Modal, TouchableOpacity, StyleSheet, ScrollView, ActivityIndicator } from 'react-native';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useData } from '@/contexts/DataContext';
import { Transaction } from '@/types';
import { useTranslation } from 'react-i18next';
import { Colors } from '@/constants/theme';

interface NotificationListProps {
    visible: boolean;
    onClose: () => void;
}

// Helper to format time ago
const formatTimeAgo = (timestamp: number, t: any): string => {
    const now = Date.now();
    const diff = now - timestamp;

    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (minutes < 1) return t('notifications.justNow');
    if (minutes < 60) return t('notifications.minutesAgo', { count: minutes });
    if (hours < 24) return t('notifications.hoursAgo', { count: hours });
    return t('notifications.daysAgo', { count: days });
};

// Get icon and colors based on transaction type
const getTransactionStyle = (type: Transaction['type']) => {
    switch (type) {
        case 'SALE':
            return {
                iconName: 'shopping-cart' as const,
                iconColor: Colors.light.primaryLight,
                iconTextColor: Colors.light.primary,
            };
        case 'EXPENSE':
            return {
                iconName: 'money' as const,
                iconColor: Colors.light.secondaryLight,
                iconTextColor: Colors.light.secondary,
            };
        case 'PRODUCT_ADD':
            return {
                iconName: 'cube' as const,
                iconColor: Colors.light.primaryLight,
                iconTextColor: Colors.light.primary,
            };
        case 'PAYMENT_RECEIVED':
            return {
                iconName: 'rupee' as const,
                iconColor: Colors.light.primaryLight,
                iconTextColor: Colors.light.primary,
            };
        case 'PAYMENT_GIVEN':
            return {
                iconName: 'arrow-right' as const,
                iconColor: Colors.light.errorLight,
                iconTextColor: Colors.light.error,
            };
        default:
            return {
                iconName: 'bell' as const,
                iconColor: Colors.light.background,
                iconTextColor: Colors.light.textSecondary,
            };
    }
};

export function NotificationList({ visible, onClose }: NotificationListProps) {
    const { t } = useTranslation();
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
                        <Text style={styles.modalTitle}>{t('notifications.title')}</Text>
                        <View style={styles.headerActions}>
                            {visibleTransactions.length > 0 && (
                                <TouchableOpacity onPress={dismissAllNotifications} style={styles.clearAllButton}>
                                    <Text style={styles.clearAllText}>{t('notifications.clearAll')}</Text>
                                </TouchableOpacity>
                            )}
                            <TouchableOpacity onPress={onClose}>
                                <MaterialIcons name="close" size={24} color={Colors.light.textSecondary} />
                            </TouchableOpacity>
                        </View>
                    </View>

                    <ScrollView contentContainerStyle={styles.listContainer}>
                        {isLoading ? (
                            <View style={styles.emptyState}>
                                <ActivityIndicator size="large" color={Colors.light.primary} />
                            </View>
                        ) : visibleTransactions.length === 0 ? (
                            <View style={styles.emptyState}>
                                <FontAwesome name="bell-o" size={48} color={Colors.light.textTertiary} />
                                <Text style={styles.emptyTitle}>{t('notifications.noNotifications')}</Text>
                                <Text style={styles.emptySubtitle}>{t('notifications.emptySubtitle')}</Text>
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
                                                <Text style={styles.time}>{formatTimeAgo(item.timestamp, t)}</Text>
                                            </View>
                                        </View>
                                        <TouchableOpacity
                                            style={styles.dismissButton}
                                            onPress={() => dismissNotification(item.id)}
                                            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                                        >
                                            <MaterialIcons name="close" size={18} color={Colors.light.textTertiary} />
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
        backgroundColor: Colors.light.modalOverlay,
        justifyContent: 'flex-start',
        paddingTop: 60,
    },
    modalView: {
        backgroundColor: Colors.light.card,
        marginHorizontal: 16,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
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
        borderBottomColor: Colors.light.border,
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
        color: Colors.light.error,
        fontWeight: '500',
    },
    modalTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        color: Colors.light.text,
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
        borderBottomColor: Colors.light.border,
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
        color: Colors.light.text,
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
        color: Colors.light.text,
    },
    time: {
        fontSize: 11,
        color: Colors.light.textTertiary,
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
        color: Colors.light.text,
        marginTop: 16,
    },
    emptySubtitle: {
        fontSize: 14,
        color: Colors.light.textSecondary,
        marginTop: 4,
        textAlign: 'center',
    },
});
