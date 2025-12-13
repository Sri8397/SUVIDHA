import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, ScrollView, Image, StatusBar, TouchableOpacity, StyleSheet, Modal, TextInput, Alert, ActivityIndicator, KeyboardAvoidingView, Platform } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { router } from 'expo-router';
import { useSettings } from '@/contexts/settings-context';
import { useAuth } from '@/contexts/auth-context';
import * as ImagePicker from 'expo-image-picker';
import { TransliterationInput } from '../components/ui/TransliterationInput';
import { Colors } from '@/constants/theme';

interface EditModalProps {
    visible: boolean;
    title: string;
    value: string;
    onSave: (value: string) => void;
    onClose: () => void;
    keyboardType?: 'default' | 'phone-pad' | 'email-address';
    placeholder?: string;
}

function EditModal({ visible, title, value, onSave, onClose, keyboardType = 'default', placeholder }: EditModalProps) {
    const { t } = useTranslation();
    const [inputValue, setInputValue] = useState(value);
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        setInputValue(value);
    }, [value, visible]);

    const handleSave = async () => {
        setIsSaving(true);
        await onSave(inputValue);
        setIsSaving(false);
        onClose();
    };

    return (
        <Modal
            visible={visible}
            transparent
            animationType="fade"
            onRequestClose={onClose}
        >
            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                style={styles.modalOverlay}
            >
                <View style={styles.modalContent}>
                    <Text style={styles.modalTitle}>{title}</Text>
                    {keyboardType === 'default' ? (
                        <TransliterationInput
                            style={styles.modalInput}
                            value={inputValue}
                            onChangeText={setInputValue}
                            placeholder={placeholder}
                            placeholderTextColor={Colors.light.textTertiary}
                            autoFocus
                        />
                    ) : (
                        <TextInput
                            style={styles.modalInput}
                            value={inputValue}
                            onChangeText={setInputValue}
                            keyboardType={keyboardType}
                            placeholder={placeholder}
                            placeholderTextColor={Colors.light.textTertiary}
                            autoFocus
                        />
                    )}
                    <View style={styles.modalButtons}>
                        <TouchableOpacity
                            style={styles.cancelButton}
                            onPress={onClose}
                            disabled={isSaving}
                        >
                            <Text style={styles.cancelButtonText}>{t('common.cancel')}</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={styles.saveButton}
                            onPress={handleSave}
                            disabled={isSaving}
                        >
                            {isSaving ? (
                                <ActivityIndicator size="small" color={Colors.light.white} />
                            ) : (
                                <Text style={styles.saveButtonText}>{t('common.save')}</Text>
                            )}
                        </TouchableOpacity>
                    </View>
                </View>
            </KeyboardAvoidingView>
        </Modal>
    );
}

export default function ProfileScreen() {
    const { t } = useTranslation();
    const { userProfile, storeSettings, updateUserProfile, updateStoreSettings, isLoading } = useSettings();
    const { user } = useAuth();

    const [editField, setEditField] = useState<'name' | 'phone' | 'email' | 'address' | null>(null);
    const [showImagePicker, setShowImagePicker] = useState(false);

    const handleSave = async (field: string, value: string) => {
        const success = await updateUserProfile({ [field]: value });
        if (success) {
            Alert.alert(t('common.success'), t('profile.updateSuccess'));
        } else {
            Alert.alert(t('common.error'), t('profile.updateError'));
        }
    };

    const pickImage = async (useCamera: boolean) => {
        try {
            // Request permissions
            if (useCamera) {
                const permission = await ImagePicker.requestCameraPermissionsAsync();
                if (!permission.granted) {
                    Alert.alert(t('common.permission'), t('forms.cameraPermission'));
                    return;
                }
            } else {
                const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
                if (!permission.granted) {
                    Alert.alert(t('common.permission'), t('forms.galleryPermission'));
                    return;
                }
            }

            const result = useCamera
                ? await ImagePicker.launchCameraAsync({
                    mediaTypes: ['images'],
                    allowsEditing: true,
                    aspect: [1, 1],
                    quality: 0.7,
                })
                : await ImagePicker.launchImageLibraryAsync({
                    mediaTypes: ['images'],
                    allowsEditing: true,
                    aspect: [1, 1],
                    quality: 0.7,
                });

            if (!result.canceled && result.assets[0]) {
                const imageUri = result.assets[0].uri;
                const success = await updateUserProfile({ profileImage: imageUri });
                if (success) {
                    Alert.alert(t('common.success'), t('profile.photoUpdateSuccess'));
                }
            }
            setShowImagePicker(false);
        } catch (error) {
            console.error('Image picker error:', error);
            Alert.alert(t('common.error'), t('profile.photoError'));
            setShowImagePicker(false);
        }
    };

    const getDisplayName = () => {
        return userProfile.name || user?.name || t('profile.setName');
    };

    const getDisplayPhone = () => {
        return userProfile.phone || user?.phone || t('profile.notSet');
    };

    const getDisplayEmail = () => {
        return userProfile.email || t('profile.notSet');
    };

    const getDisplayAddress = () => {
        return storeSettings.address || t('profile.notSet');
    };

    const getStoreName = () => {
        return storeSettings.name || t('profile.setStoreName');
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
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color={Colors.light.text} />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>{t('profile.title')}</Text>
                    <View style={styles.backButton} />
                </View>
            </SafeAreaView>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Profile Section */}
                <View style={styles.profileSection}>
                    <TouchableOpacity
                        style={styles.profileImageContainer}
                        onPress={() => setShowImagePicker(true)}
                    >
                        <Image
                            source={
                                userProfile.profileImage
                                    ? { uri: userProfile.profileImage }
                                    : { uri: 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=400' }
                            }
                            style={styles.profileImage}
                        />
                        <View style={styles.cameraIconOverlay}>
                            <FontAwesome name="camera" size={16} color={Colors.light.white} />
                        </View>
                    </TouchableOpacity>
                    <TouchableOpacity onPress={() => setEditField('name')} style={styles.nameContainer}>
                        <Text style={styles.userName}>{getDisplayName()}</Text>
                    </TouchableOpacity>
                    <Text style={styles.storeName}>{getStoreName()}</Text>
                </View>

                {/* Editable Fields */}
                <View style={styles.fieldsContainer}>
                    {/* Phone Number */}
                    <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('phone')} activeOpacity={0.7}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('profile.phoneNumber')}</Text>
                            <Text style={styles.fieldValue}>{getDisplayPhone()}</Text>
                        </View>
                    </TouchableOpacity>

                    {/* Email */}
                    <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('email')} activeOpacity={0.7}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('profile.email')}</Text>
                            <Text style={styles.fieldValue}>{getDisplayEmail()}</Text>
                        </View>
                    </TouchableOpacity>

                    {/* Address */}
                    <TouchableOpacity style={styles.fieldCard} onPress={() => setEditField('address')} activeOpacity={0.7}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>{t('profile.address')}</Text>
                            <Text style={styles.fieldValue}>{getDisplayAddress()}</Text>
                        </View>
                    </TouchableOpacity>
                </View>


            </ScrollView>

            {/* Edit Modals */}
            <EditModal
                visible={editField === 'name'}
                title={t('profile.changeName')}
                value={userProfile.name || user?.name || ''}
                onSave={(value) => handleSave('name', value)}
                onClose={() => setEditField(null)}
                placeholder={t('profile.enterName')}
            />
            <EditModal
                visible={editField === 'phone'}
                title={t('profile.changePhone')}
                value={userProfile.phone || user?.phone || ''}
                onSave={(value) => handleSave('phone', value)}
                onClose={() => setEditField(null)}
                keyboardType="phone-pad"
                placeholder={t('profile.enterPhone')}
            />
            <EditModal
                visible={editField === 'email'}
                title={t('profile.changeEmail')}
                value={userProfile.email || ''}
                onSave={(value) => handleSave('email', value)}
                onClose={() => setEditField(null)}
                keyboardType="email-address"
                placeholder={t('profile.enterEmail')}
            />
            <EditModal
                visible={editField === 'address'}
                title={t('profile.changeAddress')}
                value={storeSettings.address || ''}
                onSave={async (value) => {
                    const success = await updateStoreSettings({ address: value });
                    if (success) {
                        Alert.alert(t('common.success'), t('profile.updateSuccess'));
                    } else {
                        Alert.alert(t('common.error'), t('profile.updateError'));
                    }
                }}
                onClose={() => setEditField(null)}
                placeholder={t('profile.enterAddress')}
            />

            {/* Image Picker Modal */}
            <Modal
                visible={showImagePicker}
                transparent
                animationType="fade"
                onRequestClose={() => setShowImagePicker(false)}
            >
                <View style={styles.imagePickerOverlay}>
                    <View style={styles.imagePickerContent}>
                        <Text style={styles.imagePickerTitle}>{t('profile.changeProfilePhoto')}</Text>
                        <TouchableOpacity
                            style={styles.imagePickerOption}
                            onPress={() => pickImage(true)}
                        >
                            <FontAwesome name="camera" size={20} color={Colors.light.primary} />
                            <Text style={styles.imagePickerOptionText}>{t('profile.takePhoto')}</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={styles.imagePickerOption}
                            onPress={() => pickImage(false)}
                        >
                            <FontAwesome name="photo" size={20} color={Colors.light.primary} />
                            <Text style={styles.imagePickerOptionText}>{t('profile.chooseFromGallery')}</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={styles.imagePickerCancel}
                            onPress={() => setShowImagePicker(false)}
                        >
                            <Text style={styles.imagePickerCancelText}>{t('common.cancel')}</Text>
                        </TouchableOpacity>
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
    headerSafeArea: {
        backgroundColor: Colors.light.white,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        backgroundColor: Colors.light.white,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.border,
    },
    backButton: {
        padding: 8,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: Colors.light.text,
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 24,
    },
    profileSection: {
        alignItems: 'center',
        paddingVertical: 32,
        backgroundColor: Colors.light.white,
        marginHorizontal: 16,
        marginTop: 16,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    profileImageContainer: {
        width: 120,
        height: 120,
        borderRadius: 60,
        overflow: 'hidden',
        marginBottom: 16,
        backgroundColor: Colors.light.white,
        borderWidth: 3,
        borderColor: Colors.light.white,
    },
    profileImage: {
        width: '100%',
        height: '100%',
    },
    cameraIconOverlay: {
        position: 'absolute',
        bottom: 0,
        right: 0,
        left: 0,
        backgroundColor: Colors.light.overlay,
        paddingVertical: 6,
        alignItems: 'center',
    },
    nameContainer: {
        paddingHorizontal: 16,
        paddingVertical: 8,
    },
    userName: {
        fontSize: 24,
        fontWeight: '700',
        color: Colors.light.text,
        marginBottom: 4,
        textAlign: 'center',
    },
    storeName: {
        fontSize: 16,
        color: Colors.light.textSecondary,
    },
    fieldsContainer: {
        paddingHorizontal: 16,
        marginTop: 8,
    },
    fieldCard: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: Colors.light.white,
        padding: 16,
        marginBottom: 12,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    fieldContent: {
        flex: 1,
    },
    fieldLabel: {
        fontSize: 14,
        color: Colors.light.textSecondary,
        marginBottom: 4,
    },
    fieldValue: {
        fontSize: 16,
        color: Colors.light.text,
        fontWeight: '500',
    },
    // Modal Styles
    modalOverlay: {
        flex: 1,
        backgroundColor: Colors.light.overlay,
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 24,
    },
    modalContent: {
        backgroundColor: Colors.light.white,
        borderRadius: 16,
        padding: 24,
        width: '100%',
        maxWidth: 340,
    },
    modalTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
        marginBottom: 16,
        textAlign: 'center',
    },
    modalInput: {
        borderWidth: 1,
        borderColor: Colors.light.border,
        borderRadius: 8,
        padding: 12,
        fontSize: 16,
        color: Colors.light.text,
        marginBottom: 20,
    },
    modalButtons: {
        flexDirection: 'row',
        gap: 12,
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
    saveButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        backgroundColor: Colors.light.primary,
        alignItems: 'center',
    },
    saveButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.white,
    },
    // Image Picker Modal Styles
    imagePickerOverlay: {
        flex: 1,
        backgroundColor: Colors.light.overlay,
        justifyContent: 'flex-end',
    },
    imagePickerContent: {
        backgroundColor: Colors.light.white,
        borderTopLeftRadius: 20,
        borderTopRightRadius: 20,
        paddingTop: 20,
        paddingBottom: 40,
        paddingHorizontal: 24,
    },
    imagePickerTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
        textAlign: 'center',
        marginBottom: 20,
    },
    imagePickerOption: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.background,
        gap: 16,
    },
    imagePickerOptionText: {
        fontSize: 16,
        color: Colors.light.text,
        fontWeight: '500',
    },
    imagePickerCancel: {
        marginTop: 12,
        paddingVertical: 16,
        alignItems: 'center',
    },
    imagePickerCancelText: {
        fontSize: 16,
        color: Colors.light.textSecondary,
        fontWeight: '600',
    },
});
