import React, { useState, useMemo } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, Modal, Image, ActivityIndicator } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { useAuth } from '@/contexts/auth-context';
import { useSettings } from '@/contexts/settings-context';
import { useData } from '@/contexts/DataContext';
import { router } from 'expo-router';
import { Header } from '@/components/ui/header';
import { useTranslation } from 'react-i18next';



// Format large numbers with K, L suffixes
const formatValue = (value: number): string => {
    if (value >= 100000) {
        return `₹${(value / 100000).toFixed(1)}L`;
    } else if (value >= 1000) {
        return `₹${(value / 1000).toFixed(1)}K`;
    }
    return value.toLocaleString('en-IN');
};

export default function MoreScreen() {
    const { t } = useTranslation();
    const [showLogoutModal, setShowLogoutModal] = useState(false);
    const { logout, user } = useAuth();
    const { userProfile, storeSettings, isLoading } = useSettings();
    const { products, sales, dashboardStats } = useData();

    const menuItems = useMemo(() => [
        { id: 'profile', icon: 'user', label: t('profile.title'), type: 'fontawesome' },
        { id: 'store', icon: 'settings', label: t('storeSettings.title'), type: 'material' },
        { id: 'employees', icon: 'people', label: t('employees.title'), type: 'material' },
        { id: 'billing', icon: 'folder', label: t('billing.title'), type: 'material' },
        { id: 'support', icon: 'help-outline', label: t('support.title'), type: 'material' },
        { id: 'logout', icon: 'logout', label: t('more.logout'), type: 'material', danger: true },
    ], [t]);

    // Calculate stats
    const stats = useMemo(() => {
        const totalProducts = products.length;
        const totalOrders = sales.length;
        const totalSales = dashboardStats?.todaySales || 0;
        const totalProfit = dashboardStats?.profit || 0;

        return {
            products: totalProducts,
            orders: totalOrders,
            sales: totalSales,
            profit: totalProfit,
        };
    }, [products, sales, dashboardStats]);

    const handleMenuPress = (itemId: string) => {
        if (itemId === 'logout') {
            setShowLogoutModal(true);
        } else if (itemId === 'profile') {
            router.push('/profile');
        } else if (itemId === 'store') {
            router.push('/store-settings');
        } else if (itemId === 'employees') {
            router.push('/employees');
        } else if (itemId === 'billing') {
            router.push('/billing');
        } else if (itemId === 'support') {
            router.push('/support');
        } else {
            console.log('Menu pressed:', itemId);
        }
    };

    const handleLogout = async () => {
        setShowLogoutModal(false);
        await logout();
        router.replace('/(auth)/login');
    };

    const getDisplayName = () => {
        return userProfile?.name || user?.name || t('profile.setName');
    };

    const getStoreName = () => {
        return storeSettings?.name || t('profile.setStoreName');
    };

    const getProfileImage = () => {
        return userProfile?.profileImage
            ? { uri: userProfile.profileImage }
            : { uri: 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=400' };
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
            <Header title={t('more.title')} showLanguageToggle={true} />

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Profile Section */}
                <View
                    style={styles.profileSection}
                >
                    <View style={styles.profileImageContainer}>
                        <Image
                            source={getProfileImage()}
                            style={styles.profileImage}
                        />
                    </View>
                    <View style={styles.profileInfo}>
                        <Text style={styles.userName}>{getDisplayName()}</Text>
                        <Text style={styles.storeName}>{getStoreName()}</Text>
                    </View>
                </View>

                {/* Stats Row */}
                <View style={styles.statsRow}>
                    <View style={styles.statItem}>
                        <Text style={styles.statLabel}>{t('more.products')}:</Text>
                        <Text style={styles.statValue}>{stats.products.toLocaleString('en-IN')}</Text>
                    </View>
                    <View style={styles.statDivider} />
                    <View style={styles.statItem}>
                        <Text style={styles.statLabel}>{t('more.orders')}:</Text>
                        <Text style={styles.statValue}>{stats.orders.toLocaleString('en-IN')}</Text>
                    </View>
                    <View style={styles.statDivider} />
                    <View style={styles.statItem}>
                        <Text style={styles.statLabel}>{t('reports.sales')}:</Text>
                        <Text style={styles.statValue}>{formatValue(stats.sales)}</Text>
                    </View>
                    <View style={styles.statDivider} />
                    <View style={styles.statItem}>
                        <Text style={styles.statLabel}>{t('reports.profit')}:</Text>
                        <Text style={[styles.statValue, stats.profit < 0 && styles.negativeValue]}>
                            {formatValue(stats.profit)}
                        </Text>
                    </View>
                </View>

                {/* Menu Card */}
                <View style={styles.menuCard}>
                    {menuItems.map((item, index) => (
                        <TouchableOpacity
                            key={item.id}
                            onPress={() => handleMenuPress(item.id)}
                            style={[
                                styles.menuItem,
                                index !== menuItems.length - 1 && styles.menuItemBorder
                            ]}
                        >
                            <View style={styles.menuIconContainer}>
                                {item.type === 'material' ? (
                                    <MaterialIcons
                                        name={item.icon as any}
                                        size={22}
                                        color={item.danger ? '#D32F2F' : '#616161'}
                                    />
                                ) : (
                                    <FontAwesome
                                        name={item.icon as any}
                                        size={20}
                                        color={item.danger ? '#D32F2F' : '#616161'}
                                    />
                                )}
                            </View>
                            <Text style={[
                                styles.menuItemText,
                                item.danger && styles.menuItemTextDanger
                            ]}>
                                {item.label}
                            </Text>
                        </TouchableOpacity>
                    ))}
                </View>
            </ScrollView>

            {/* Logout Confirmation Modal */}
            <Modal
                visible={showLogoutModal}
                transparent
                animationType="fade"
                onRequestClose={() => setShowLogoutModal(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <Text style={styles.modalTitle}>{t('more.logout')}</Text>
                        <Text style={styles.modalMessage}>
                            {t('more.logoutConfirm')}
                        </Text>
                        <View style={styles.modalButtons}>
                            <TouchableOpacity
                                style={styles.cancelButton}
                                onPress={() => setShowLogoutModal(false)}
                            >
                                <Text style={styles.cancelButtonText}>{t('common.cancel')}</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.logoutButton}
                                onPress={handleLogout}
                            >
                                <Text style={styles.logoutButtonText}>{t('more.logout')}</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FAFAFA',
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#FAFAFA',
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 100,
    },

    // Profile Section
    profileSection: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 20,
        paddingTop: 20,
        paddingBottom: 24,
        backgroundColor: '#FAFAFA',
    },
    profileImageContainer: {
        width: 110,
        height: 110,
        borderRadius: 55,
        overflow: 'hidden',
        backgroundColor: '#E8E8E8',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
    },
    profileImage: {
        width: '100%',
        height: '100%',
    },
    profileInfo: {
        marginLeft: 16,
        flex: 1,
    },
    userName: {
        fontSize: 22,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 4,
    },
    storeName: {
        fontSize: 14,
        color: '#757575',
        fontWeight: '400',
    },

    // Stats Row
    statsRow: {
        flexDirection: 'row',
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        borderRadius: 12,
        paddingVertical: 16,
        paddingHorizontal: 8,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 3,
        elevation: 1,
        marginBottom: 16,
    },
    statItem: {
        flex: 1,
        alignItems: 'center',
    },
    statLabel: {
        fontSize: 12,
        color: '#9E9E9E',
        marginBottom: 4,
    },
    statValue: {
        fontSize: 16,
        fontWeight: '700',
        color: '#212121',
    },
    negativeValue: {
        color: '#D32F2F',
    },
    statDivider: {
        width: 1,
        backgroundColor: '#E0E0E0',
        marginVertical: 4,
    },

    // Menu Card
    menuCard: {
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 3,
        elevation: 1,
        overflow: 'hidden',
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 20,
        paddingVertical: 18,
        gap: 16,
    },
    menuItemBorder: {
        borderBottomWidth: 1,
        borderBottomColor: '#F0F0F0',
    },
    menuIconContainer: {
        width: 24,
        height: 24,
        alignItems: 'center',
        justifyContent: 'center',
    },
    menuItemText: {
        fontSize: 16,
        fontWeight: '500',
        color: '#212121',
    },
    menuItemTextDanger: {
        color: '#D32F2F',
    },

    // Modal Styles
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 40,
    },
    modalContent: {
        backgroundColor: '#FFFFFF',
        borderRadius: 16,
        padding: 24,
        width: '100%',
        alignItems: 'center',
    },
    modalTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 12,
    },
    modalMessage: {
        fontSize: 14,
        color: '#757575',
        textAlign: 'center',
        lineHeight: 22,
        marginBottom: 24,
    },
    modalButtons: {
        flexDirection: 'row',
        gap: 12,
        width: '100%',
    },
    cancelButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: '#E0E0E0',
        alignItems: 'center',
    },
    cancelButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#757575',
    },
    logoutButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        backgroundColor: '#2E7D32',
        alignItems: 'center',
    },
    logoutButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#FFFFFF',
    },
});
