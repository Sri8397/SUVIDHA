import React, { useState } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, Modal } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { Header } from '@/components/ui/header';
import { useAuth } from '@/contexts/auth-context';
import { router } from 'expo-router';
import { FAB } from '@/components/ui';

const menuItems = [
    { id: 'profile', icon: 'user', label: 'मेरी प्रोफाइल', type: 'fontawesome' },
    { id: 'store', icon: 'settings', label: 'स्टोर की सेटिंग', type: 'material' },
    { id: 'employees', icon: 'people', label: 'कर्मचारी प्रबंधन', type: 'material' },
    { id: 'billing', icon: 'folder', label: 'बिलिंग और भुगतान', type: 'material' },
    { id: 'support', icon: 'help-outline', label: 'सहायता और समर्थन', type: 'material' },
    { id: 'logout', icon: 'sign-out', label: 'लॉग आउट', type: 'fontawesome', danger: true },
];

export default function MoreScreen() {
    const [showLogoutModal, setShowLogoutModal] = useState(false);
    const { logout } = useAuth();

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

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            {/* Header */}
            <Header title="अधिक" showLanguageToggle={true} />

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
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
                            <View style={{ width: 22, height: 22, alignItems: 'center', justifyContent: 'center' }}>
                                {item.type === 'material' ? (
                                    <MaterialIcons
                                        name={item.icon as any}
                                        size={22}
                                        color={item.danger ? '#D32F2F' : '#616161'}
                                    />
                                ) : (
                                    <FontAwesome
                                        name={item.icon as any}
                                        size={22}
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

            {/* FAB */}
            <FAB
                onNewSale={() => console.log('New Sale')}
                onNewExpense={() => console.log('New Expense')}
                onAddProduct={() => console.log('Add Product')}
                onAddCustomer={() => console.log('Add Customer')}
            />

            {/* Logout Confirmation Modal */}
            <Modal
                visible={showLogoutModal}
                transparent
                animationType="fade"
                onRequestClose={() => setShowLogoutModal(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <Text style={styles.modalTitle}>लॉग आउट</Text>
                        <Text style={styles.modalMessage}>
                            क्या आप अपना स्टोर से लॉग{'\n'}आउट करना चाहते हैं?
                        </Text>
                        <View style={styles.modalButtons}>
                            <TouchableOpacity
                                style={styles.cancelButton}
                                onPress={() => setShowLogoutModal(false)}
                            >
                                <Text style={styles.cancelButtonText}>रद्द करें</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.logoutButton}
                                onPress={handleLogout}
                            >
                                <Text style={styles.logoutButtonText}>लॉग आउट</Text>
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
        backgroundColor: '#F5F5F5',
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 100,
        paddingTop: 16,
    },
    menuCard: {
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
        overflow: 'hidden',
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 16,
        paddingVertical: 16,
        gap: 16,
    },
    menuItemBorder: {
        borderBottomWidth: 1,
        borderBottomColor: '#F5F5F5',
    },
    menuItemText: {
        fontSize: 16,
        fontWeight: '500',
        color: '#212121',
    },
    menuItemTextDanger: {
        color: '#D32F2F',
    },
    
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
