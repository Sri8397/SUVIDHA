import React from 'react';
import { View, Text, Modal, TouchableOpacity, StyleSheet } from 'react-native';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';

interface LanguageModalProps {
    visible: boolean;
    onClose: () => void;
    selectedLanguage: string;
    onSelectLanguage: (lang: string) => void;
}

const languages = [
    { id: 'hi', label: 'हिंदी' },
    { id: 'en', label: 'English' },
    { id: 'mr', label: 'मराठी' },
    { id: 'gu', label: 'गुजरती' },
    { id: 'Bn', label: 'বাংলা' },
];

export function LanguageModal({ visible, onClose, selectedLanguage, onSelectLanguage }: LanguageModalProps) {
    return (
        <Modal
            animationType="fade"
            transparent={true}
            visible={visible}
            onRequestClose={onClose}
        >
            <View style={styles.centeredView}>
                <View style={styles.modalView}>
                    <Text style={styles.modalTitle}>भाषा चुनें</Text>

                    <View style={styles.radioGroup}>
                        {languages.map((lang) => (
                            <TouchableOpacity
                                key={lang.id}
                                style={styles.radioButton}
                                onPress={() => onSelectLanguage(lang.id)}
                            >
                                <MaterialIcons
                                    name={selectedLanguage === lang.id ? "radio-button-on" : "radio-button-off"}
                                    size={24}
                                    color={selectedLanguage === lang.id ? "#2E7D32" : "#757575"}
                                />
                                <Text style={styles.radioLabel}>{lang.label}</Text>
                            </TouchableOpacity>
                        ))}
                    </View>

                    <View style={styles.buttonContainer}>
                        <TouchableOpacity style={styles.buttonCancel} onPress={onClose}>
                            <Text style={styles.textCancel}>रद्द करें</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.buttonApply} onPress={onClose}>
                            <Text style={styles.textApply}>लागू करें</Text>
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
        backgroundColor: 'rgba(0,0,0,0.5)',
    },
    modalView: {
        width: 300,
        backgroundColor: 'white',
        borderRadius: 8,
        padding: 24,
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.25,
        shadowRadius: 4,
        elevation: 5,
    },
    modalTitle: {
        fontSize: 20,
        fontWeight: 'bold',
        marginBottom: 20,
        color: '#000',
    },
    radioGroup: {
        gap: 16,
        marginBottom: 24,
    },
    radioButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    radioLabel: {
        fontSize: 16,
        color: '#000',
    },
    buttonContainer: {
        flexDirection: 'row',
        justifyContent: 'flex-end',
        gap: 20,
    },
    buttonCancel: {
        padding: 8,
    },
    textCancel: {
        color: '#757575',
        fontSize: 14,
        fontWeight: '600',
    },
    buttonApply: {
        padding: 8,
    },
    textApply: {
        color: '#2E7D32',
        fontSize: 14,
        fontWeight: '600',
    },
});
