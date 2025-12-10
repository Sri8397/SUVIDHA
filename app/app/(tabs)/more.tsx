import React from 'react';
import { View, Text, ScrollView, Image, StatusBar, TouchableOpacity, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { Header } from '@/components/ui/header';

const menuItems = [
    { id: 'profile', icon: 'user', label: 'मेरी प्रोफाइल', type: 'fontawesome' },
    { id: 'store', icon: 'settings', label: 'स्टोर की सेटिंग', type: 'material' },
    { id: 'employees', icon: 'people', label: 'कर्मचारी प्रबंधन', type: 'material' },
    { id: 'billing', icon: 'folder', label: 'बिलिंग और भुगतान', type: 'material' },
    { id: 'support', icon: 'help-outline', label: 'सहायता और समर्थन', type: 'material' },
    { id: 'logout', icon: 'sign-out', label: 'लॉग आउट', type: 'fontawesome', danger: true },
];

export default function MoreScreen() {
    const handleMenuPress = (itemId: string) => {
        console.log('Menu pressed:', itemId);
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
            <TouchableOpacity style={styles.fab}>
                <FontAwesome name="plus" size={24} color="#FFFFFF" />
            </TouchableOpacity>
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
    fab: {
        position: 'absolute',
        bottom: 20,
        right: 20,
        width: 56,
        height: 56,
        borderRadius: 28,
        backgroundColor: '#2E7D32',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 6,
        elevation: 8,
    },
});
