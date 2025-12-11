import React from 'react';
import { View, Text, Modal, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { useTranslation } from 'react-i18next';
import { changeLanguage } from '@/utils/i18n';

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
    { id: 'gu', label: 'ગુજરાતી' },
    { id: 'bn', label: 'বাংলা' },
    { id: 'ta', label: 'தமிழ்' },
    { id: 'te', label: 'తెలుగు' },
    { id: 'kn', label: 'ಕನ್ನಡ' },
    { id: 'ml', label: 'മലയാളം' },
    { id: 'pa', label: 'ਪੰਜਾਬੀ' },
    { id: 'or', label: 'ଓଡ଼ିଆ' },
];

export function LanguageModal({ visible, onClose, selectedLanguage, onSelectLanguage }: LanguageModalProps) {
    const { t } = useTranslation();

    const handleLanguageSelect = async (langId: string) => {
        await changeLanguage(langId);
        onSelectLanguage(langId);
        onClose();
    };

    return (
        <Modal
            animationType="fade"
            transparent={true}
            visible={visible}
            onRequestClose={onClose}
        >
            <View style={styles.centeredView}>
                <View style={styles.modalView}>
                    <Text style={styles.modalTitle}>{t('more.selectLanguage', 'भाषा चुनें')}</Text>

                    <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={true}>
                        <View style={styles.radioGroup}>
                            {languages.map((lang) => (
                                <TouchableOpacity
                                    key={lang.id}
                                    style={styles.radioButton}
                                    onPress={() => handleLanguageSelect(lang.id)}
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
                    </ScrollView>

                    <View style={styles.buttonContainer}>
                        <TouchableOpacity style={styles.buttonCancel} onPress={onClose}>
                            <Text style={styles.textCancel}>{t('common.cancel')}</Text>
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
        paddingHorizontal: 20,
    },
    modalView: {
        width: '100%',
        maxWidth: 400,
        backgroundColor: 'white',
        borderRadius: 12,
        paddingVertical: 24,
        paddingHorizontal: 16,
        maxHeight: '80%',
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
        color: '#212121',
        textAlign: 'center',
    },
    scrollView: {
        marginBottom: 24,
    },
    radioGroup: {
        gap: 12,
    },
    radioButton: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 12,
        paddingHorizontal: 16,
        backgroundColor: '#F5F5F5',
        borderRadius: 8,
        gap: 12,
    },
    radioLabel: {
        fontSize: 16,
        color: '#212121',
    },
    buttonContainer: {
        flexDirection: 'row',
        gap: 12,
        justifyContent: 'center',
    },
    buttonCancel: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        backgroundColor: '#F5F5F5',
    },
    textCancel: {
        color: '#212121',
        fontWeight: 'bold',
        textAlign: 'center',
    },
});
