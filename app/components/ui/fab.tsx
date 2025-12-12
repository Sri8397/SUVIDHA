import React, { useState } from 'react';
import { TouchableOpacity, StyleSheet, View, Text, Animated, Pressable, Dimensions } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { MaterialIcons } from '@expo/vector-icons';
import { useTranslation } from 'react-i18next';
import { useAppMode } from '@/contexts/ModeContext';

interface FABMenuItem {
    id: string;
    label: string;
    icon: keyof typeof MaterialIcons.glyphMap;
    onPress: () => void;
}

interface FABProps {
    onNewSale?: () => void;
    onNewExpense?: () => void;
    onAddProduct?: () => void;
    onAddCustomer?: () => void;
    onAddIncome?: () => void;
    onAddLoan?: () => void;
}

const { height: SCREEN_HEIGHT, width: SCREEN_WIDTH } = Dimensions.get('window');

export function FAB({
    onNewSale,
    onNewExpense,
    onAddProduct,
    onAddCustomer,
    onAddIncome,
    onAddLoan
}: FABProps) {
    const { t } = useTranslation();
    const { appMode } = useAppMode();
    const [isOpen, setIsOpen] = useState(false);
    const [animation] = useState(new Animated.Value(0));

    const isPersonalMode = appMode === 'personal';

    const menuItems: FABMenuItem[] = isPersonalMode ? [
        { id: 'income', label: t('personal.income.title', 'Add Income'), icon: 'attach-money', onPress: onAddIncome || (() => { }) },
        { id: 'expense', label: t('forms.newExpense'), icon: 'receipt-long', onPress: onNewExpense || (() => { }) },
        { id: 'loan', label: t('personal.loan.title', 'Add Loan'), icon: 'account-balance-wallet', onPress: onAddLoan || (() => { }) },
        { id: 'friend', label: t('personal.friend.title'), icon: 'person-add', onPress: onAddCustomer || (() => { }) }, // Reusing AddCustomer for friends
    ] : [
        { id: 'sale', label: t('forms.newSale'), icon: 'sell', onPress: onNewSale || (() => { }) },
        { id: 'expense', label: t('forms.newExpense'), icon: 'receipt-long', onPress: onNewExpense || (() => { }) },
        { id: 'product', label: t('inventory.addProduct'), icon: 'inventory-2', onPress: onAddProduct || (() => { }) },
        { id: 'customer', label: t('customers.addCustomer'), icon: 'person-add', onPress: onAddCustomer || (() => { }) },
    ];

    const toggleMenu = () => {
        const toValue = isOpen ? 0 : 1;
        Animated.spring(animation, {
            toValue,
            friction: 6,
            tension: 40,
            useNativeDriver: true,
        }).start();
        setIsOpen(!isOpen);
    };

    const handleMenuItemPress = (item: FABMenuItem) => {
        toggleMenu();
        item.onPress();
    };

    const rotation = animation.interpolate({
        inputRange: [0, 1],
        outputRange: ['0deg', '45deg'],
    });

    const menuScale = animation.interpolate({
        inputRange: [0, 1],
        outputRange: [0, 1],
    });

    const menuOpacity = animation.interpolate({
        inputRange: [0, 0.5, 1],
        outputRange: [0, 0.5, 1],
    });

    return (
        <>
            {isOpen && (
                <Pressable style={styles.backdrop} onPress={toggleMenu} />
            )}
            <View style={styles.container} pointerEvents="box-none">
                {/* Menu */}
                <Animated.View
                    style={[
                        styles.menu,
                        {
                            transform: [{ scale: menuScale }],
                            opacity: menuOpacity,
                        },
                    ]}
                    pointerEvents={isOpen ? 'auto' : 'none'}
                >
                    {menuItems.map((item) => (
                        <TouchableOpacity
                            key={item.id}
                            style={styles.menuItem}
                            onPress={() => handleMenuItemPress(item)}
                            activeOpacity={0.7}
                        >
                            <MaterialIcons name={item.icon} size={20} color="#333" style={styles.menuIcon} />
                            <Text style={styles.menuLabel}>{item.label}</Text>
                        </TouchableOpacity>
                    ))}
                </Animated.View>

                {/* FAB Button */}
                <TouchableOpacity
                    onPress={toggleMenu}
                    style={styles.fab}
                    activeOpacity={0.8}
                >
                    <Animated.View style={{ transform: [{ rotate: rotation }] }}>
                        <FontAwesome name="plus" size={24} color="#FFFFFF" />
                    </Animated.View>
                </TouchableOpacity>
            </View>
        </>
    );
}

const styles = StyleSheet.create({
    backdrop: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.3)',
        zIndex: 99,
    },
    container: {
        position: 'absolute',
        bottom: 20,
        right: 20,
        alignItems: 'flex-end',
        zIndex: 100,
    },
    fab: {
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
    menu: {
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        paddingVertical: 8,
        marginBottom: 16,
        minWidth: 160,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.25,
        shadowRadius: 8,
        elevation: 10,
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 12,
        paddingHorizontal: 16,
    },
    menuIcon: {
        marginRight: 12,
    },
    menuLabel: {
        fontSize: 16,
        color: '#333',
        fontWeight: '500',
    },
});
