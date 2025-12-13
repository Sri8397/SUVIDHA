import React from 'react';
import { View, Text, Modal, TouchableOpacity, StyleSheet } from 'react-native';
import { Colors } from '@/constants/theme';
import { useTranslation } from 'react-i18next';

interface InfoModalProps {
    visible: boolean;
    onClose: () => void;
    title: string;
    message: string;
}

export function InfoModal({ visible, onClose, title, message }: InfoModalProps) {
    const { t } = useTranslation();

    return (
        <Modal
            animationType="fade"
            transparent={true}
            visible={visible}
            onRequestClose={onClose}
        >
            <View style={styles.centeredView}>
                <View style={styles.modalView}>
                    <Text style={styles.modalTitle}>{title}</Text>
                    <Text style={styles.modalMessage}>{message}</Text>

                    <View style={styles.buttonContainer}>
                        <TouchableOpacity style={styles.buttonOk} onPress={onClose}>
                            <Text style={styles.textOk}>{t('common.ok', 'OK')}</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </View>
        </Modal>
    );
}

const styles = StyleSheet.create({
    centeredView: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: Colors.light.modalOverlay,
        padding: 20,
    },
    modalView: {
        width: '100%',
        maxWidth: 320,
        backgroundColor: Colors.light.card,
        borderRadius: 12,
        padding: 24,
        shadowColor: Colors.light.shadow,
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.25,
        shadowRadius: 4,
        elevation: 5,
    },
    modalTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 12,
        color: Colors.light.text,
    },
    modalMessage: {
        fontSize: 16,
        color: Colors.light.textSecondary,
        marginBottom: 24,
        lineHeight: 22,
    },
    buttonContainer: {
        flexDirection: 'row',
        justifyContent: 'flex-end',
    },
    buttonOk: {
        paddingHorizontal: 16,
        paddingVertical: 8,
        backgroundColor: Colors.light.primary,
        borderRadius: 6,
    },
    textOk: {
        color: Colors.light.white,
        fontSize: 14,
        fontWeight: '600',
    },
});
