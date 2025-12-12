import React, { useState, useMemo } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, Modal, Image, ActivityIndicator } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { useTranslation } from 'react-i18next';
import { useAuth } from '@/contexts/auth-context';
import { useSettings } from '@/contexts/settings-context';
import { useData } from '@/contexts/DataContext';
import { useAppMode } from '@/contexts/ModeContext';
import { router } from 'expo-router';
import { Header } from '@/components/ui/header';
import { Colors } from '@/constants/theme';




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
    const { appMode } = useAppMode();
    const isPersonalMode = appMode === 'personal';

    const menuItems = useMemo(() => {
        const items = [
            { id: 'profile', icon: 'user', label: t('profile.title'), type: 'fontawesome' },
            { id: 'store', icon: 'settings', label: isPersonalMode ? t('personal.appSettings') : t('storeSettings.title'), type: 'material' },
            { id: 'employees', icon: 'people', label: t('employees.title'), type: 'material', hideInPersonal: true },
            { id: 'billing', icon: 'folder', label: t('billing.title'), type: 'material', hideInPersonal: true },
            { id: 'support', icon: 'help-outline', label: t('support.title'), type: 'material' },
            { id: 'switchMode', icon: 'swap-horiz', label: t('more.switchMode'), type: 'material' },
            { id: 'logout', icon: 'logout', label: t('more.logout'), type: 'material', danger: true },
        ];
        // Filter out items that should be hidden in personal mode
        return isPersonalMode ? items.filter(item => !item.hideInPersonal) : items;
    }, [t, isPersonalMode]);

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
        } else if (itemId === 'switchMode') {
            router.push('/mode-select');
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
                <ActivityIndicator size="large" color={Colors.light.primary} />
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />

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
                    {!isPersonalMode && (
                        <>
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
                        </>
                    )}
                    <View style={styles.statItem}>
                        <Text style={styles.statLabel}>{t(isPersonalMode ? 'reports.income' : 'reports.sales')}:</Text>
                        <Text style={styles.statValue}>{formatValue(stats.sales)}</Text>
                    </View>
                    <View style={styles.statDivider} />
                    <View style={styles.statItem}>
                        <Text style={styles.statLabel}>{t(isPersonalMode ? 'personal.savings' : 'reports.profit')}:</Text>
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
                                        color={item.danger ? Colors.light.error : Colors.light.textSecondary}
                                    />
                                ) : (
                                    <FontAwesome
                                        name={item.icon as any}
                                        size={20}
                                        color={item.danger ? Colors.light.error : Colors.light.textSecondary}
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
        backgroundColor: Colors.light.background,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: Colors.light.background,
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
        backgroundColor: Colors.light.background,
    },
    profileImageContainer: {
        width: 110,
        height: 110,
        borderRadius: 55,
        overflow: 'hidden',
        backgroundColor: Colors.light.border,
        shadowColor: Colors.light.shadow,
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
        color: Colors.light.text,
        marginBottom: 4,
    },
    storeName: {
        fontSize: 14,
        color: Colors.light.textSecondary,
        fontWeight: '400',
    },

    // Stats Row
    statsRow: {
        flexDirection: 'row',
        backgroundColor: Colors.light.white,
        marginHorizontal: 16,
        borderRadius: 12,
        paddingVertical: 16,
        paddingHorizontal: 8,
        shadowColor: Colors.light.shadow,
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
        color: Colors.light.textTertiary,
        marginBottom: 4,
    },
    statValue: {
        fontSize: 16,
        fontWeight: '700',
        color: Colors.light.text,
    },
    negativeValue: {
        color: Colors.light.error,
    },
    statDivider: {
        width: 1,
        backgroundColor: Colors.light.border,
        marginVertical: 4,
    },

    // Menu Card
    menuCard: {
        backgroundColor: Colors.light.white,
        marginHorizontal: 16,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
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
        borderBottomColor: Colors.light.border, // Slightly lighter border for items
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
        color: Colors.light.text,
    },
    menuItemTextDanger: {
        color: Colors.light.error,
    },

    // Modal Styles
    modalOverlay: {
        flex: 1,
        backgroundColor: Colors.light.overlay,
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 40,
    },
    modalContent: {
        backgroundColor: Colors.light.white,
        borderRadius: 16,
        padding: 24,
        width: '100%',
        alignItems: 'center',
    },
    modalTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: Colors.light.text,
        marginBottom: 12,
    },
    modalMessage: {
        fontSize: 14,
        color: Colors.light.textSecondary,
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
        borderColor: Colors.light.border,
        alignItems: 'center',
    },
    cancelButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.textSecondary,
    },
    logoutButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        backgroundColor: Colors.light.primary,
        alignItems: 'center',
    },
    logoutButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.white,
    },
});
