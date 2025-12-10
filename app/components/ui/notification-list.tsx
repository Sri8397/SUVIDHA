import React from 'react';
import { View, Text, Modal, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import FontAwesome from '@expo/vector-icons/FontAwesome';

interface NotificationListProps {
    visible: boolean;
    onClose: () => void;
}

const notifications = [
    {
        id: '1',
        type: 'alert',
        title: 'कम स्टॉक अलर्ट: पारले-जी बिस्कुट केवल 5 बचे हैं।',
        time: '2 मिनट पहले',
        iconColor: '#FFEBEE',
        iconTextColor: '#D32F2F',
        iconName: 'bell',
    },
    {
        id: '2',
        type: 'order',
        title: 'नया ऑर्डर प्राप्त हुआ: रमेश कुमार से ₹ 500 का ऑर्डर।',
        time: '1 घंटा पहले',
        iconColor: '#E3F2FD',
        iconTextColor: '#1976D2',
        iconName: 'shopping-cart',
    },
    {
        id: '3',
        type: 'payment',
        title: 'भुगतान प्राप्त: विक्रम सिंह से ₹ 2,500 प्राप्त हुए।',
        time: '3 घंटे पहले',
        iconColor: '#E8F5E9',
        iconTextColor: '#2E7D32',
        iconName: 'file-text', // Close approximation for money receipt
    },
    {
        id: '4',
        type: 'system',
        title: 'सिस्टम अपडेट: ऐप का नया संस्करण उपलब्ध है।',
        time: '1 दिन पहले',
        iconColor: '#E3F2FD',
        iconTextColor: '#1976D2',
        iconName: 'refresh',
    },
];

export function NotificationList({ visible, onClose }: NotificationListProps) {
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
                        <TouchableOpacity onPress={onClose}>
                            <MaterialIcons name="arrow-back" size={24} color="black" />
                        </TouchableOpacity>
                    </View>

                    <ScrollView contentContainerStyle={styles.listContainer}>
                        {notifications.map((item, index) => (
                            <View key={item.id} style={[styles.notificationItem, index !== notifications.length - 1 && styles.borderBottom]}>
                                <View style={[styles.iconContainer, { backgroundColor: item.iconColor }]}>
                                    <FontAwesome name={item.iconName as any} size={20} color={item.iconTextColor} />
                                </View>
                                <View style={styles.textContainer}>
                                    <Text style={styles.title}>{item.title}</Text>
                                    <Text style={styles.time}>{item.time}</Text>
                                </View>
                            </View>
                        ))}
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
        justifyContent: 'flex-start', // Align to top
        paddingTop: 60, // approximate status bar/header height
    },
    modalView: {
        backgroundColor: 'white',
        marginHorizontal: 16,
        borderRadius: 8,
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.25,
        shadowRadius: 4,
        elevation: 5,
        maxHeight: 400, // Limit height
    },
    header: {
        flexDirection: 'row-reverse', // To put back arrow on left if needed, but mock shows title left and arrow left? Actually mock shows back arrow left and title right.
        // Wait, mock shows "Soochnaye" title and back arrow. It looks like a full screen or persistent dropdown.
        // Let's implement as a dropdown menu first for simplicity in current context, or close to it.
        // Actually, looking at the mock, it seems like a dropdown directly under the bell icon or a header overlay.
        // Let's stick to a simple modal with a header for now suited for the "popup" look.
        flexDirection: 'row',
        alignItems: 'center',
        padding: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#EEEEEE',
        gap: 16,
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
        padding: 16,
        gap: 16,
        alignItems: 'flex-start',
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
        color: '#000',
        marginBottom: 4,
        lineHeight: 20,
    },
    time: {
        fontSize: 12,
        color: '#757575',
    },
});
