import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, Image, StatusBar, TouchableOpacity, StyleSheet, Modal, TextInput, Alert, ActivityIndicator } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { router } from 'expo-router';
import { useSettings } from '@/contexts/settings-context';
import { useAuth } from '@/contexts/auth-context';
import * as ImagePicker from 'expo-image-picker';

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
            <View style={styles.modalOverlay}>
                <View style={styles.modalContent}>
                    <Text style={styles.modalTitle}>{title}</Text>
                    <TextInput
                        style={styles.modalInput}
                        value={inputValue}
                        onChangeText={setInputValue}
                        keyboardType={keyboardType}
                        placeholder={placeholder}
                        placeholderTextColor="#9E9E9E"
                        autoFocus
                    />
                    <View style={styles.modalButtons}>
                        <TouchableOpacity
                            style={styles.cancelButton}
                            onPress={onClose}
                            disabled={isSaving}
                        >
                            <Text style={styles.cancelButtonText}>रद्द करें</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={styles.saveButton}
                            onPress={handleSave}
                            disabled={isSaving}
                        >
                            {isSaving ? (
                                <ActivityIndicator size="small" color="#FFFFFF" />
                            ) : (
                                <Text style={styles.saveButtonText}>सेव करें</Text>
                            )}
                        </TouchableOpacity>
                    </View>
                </View>
            </View>
        </Modal>
    );
}

export default function ProfileScreen() {
    const { userProfile, storeSettings, updateUserProfile, updateStoreSettings, isLoading } = useSettings();
    const { user } = useAuth();

    const [editField, setEditField] = useState<'name' | 'phone' | 'email' | 'address' | null>(null);
    const [showImagePicker, setShowImagePicker] = useState(false);

    const handleSave = async (field: string, value: string) => {
        const success = await updateUserProfile({ [field]: value });
        if (success) {
            Alert.alert('सफलता', 'जानकारी सफलतापूर्वक अपडेट हो गई।');
        } else {
            Alert.alert('त्रुटि', 'कुछ गलत हो गया।');
        }
    };

    const pickImage = async (useCamera: boolean) => {
        try {
            // Request permissions
            if (useCamera) {
                const permission = await ImagePicker.requestCameraPermissionsAsync();
                if (!permission.granted) {
                    Alert.alert('अनुमति आवश्यक', 'कैमरा उपयोग करने के लिए अनुमति दें');
                    return;
                }
            } else {
                const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
                if (!permission.granted) {
                    Alert.alert('अनुमति आवश्यक', 'गैलरी एक्सेस करने के लिए अनुमति दें');
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
                    Alert.alert('सफलता', 'प्रोफाइल फोटो अपडेट हो गई।');
                }
            }
            setShowImagePicker(false);
        } catch (error) {
            console.error('Image picker error:', error);
            Alert.alert('त्रुटि', 'फोटो चुनने में समस्या हुई');
            setShowImagePicker(false);
        }
    };

    const getDisplayName = () => {
        return userProfile.name || user?.name || 'नाम सेट करें';
    };

    const getDisplayPhone = () => {
        return userProfile.phone || user?.phone || 'सेट नहीं है';
    };

    const getDisplayEmail = () => {
        return userProfile.email || 'सेट नहीं है';
    };

    const getDisplayAddress = () => {
        return storeSettings.address || 'सेट नहीं है';
    };

    const getStoreName = () => {
        return storeSettings.name || 'स्टोर का नाम सेट करें';
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
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color="#212121" />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>मेरी प्रोफाइल</Text>
                    <TouchableOpacity style={styles.notificationButton}>
                        <FontAwesome name="bell" size={20} color="#212121" />
                    </TouchableOpacity>
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
                            <FontAwesome name="camera" size={16} color="#FFFFFF" />
                        </View>
                    </TouchableOpacity>
                    <TouchableOpacity onPress={() => setEditField('name')}>
                        <Text style={styles.userName}>{getDisplayName()}</Text>
                        <FontAwesome name="pencil" size={14} color="#2E7D32" style={styles.nameEditIcon} />
                    </TouchableOpacity>
                    <Text style={styles.storeName}>{getStoreName()}</Text>
                </View>

                {/* Editable Fields */}
                <View style={styles.fieldsContainer}>
                    {/* Phone Number */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>फ़ोन नंबर:</Text>
                            <Text style={styles.fieldValue}>{getDisplayPhone()}</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon} onPress={() => setEditField('phone')}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Email */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>ईमेल:</Text>
                            <Text style={styles.fieldValue}>{getDisplayEmail()}</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon} onPress={() => setEditField('email')}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Address */}
                    <View style={styles.fieldCard}>
                        <View style={styles.fieldContent}>
                            <Text style={styles.fieldLabel}>पता:</Text>
                            <Text style={styles.fieldValue}>{getDisplayAddress()}</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon} onPress={() => setEditField('address')}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>
                </View>

                {/* Account Settings Section */}
                <View style={styles.settingsSection}>
                    <Text style={styles.sectionTitle}>खाता सेटिंग</Text>

                    <TouchableOpacity style={styles.settingCard}>
                        <Text style={styles.settingText}>पासवर्ड बदलें</Text>
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.settingCard}>
                        <Text style={styles.settingText}>ऐप सेटिंग</Text>
                    </TouchableOpacity>
                </View>
            </ScrollView>

            {/* Edit Modals */}
            <EditModal
                visible={editField === 'name'}
                title="नाम बदलें"
                value={userProfile.name || user?.name || ''}
                onSave={(value) => handleSave('name', value)}
                onClose={() => setEditField(null)}
                placeholder="अपना नाम दर्ज करें"
            />
            <EditModal
                visible={editField === 'phone'}
                title="फ़ोन नंबर बदलें"
                value={userProfile.phone || user?.phone || ''}
                onSave={(value) => handleSave('phone', value)}
                onClose={() => setEditField(null)}
                keyboardType="phone-pad"
                placeholder="फ़ोन नंबर दर्ज करें"
            />
            <EditModal
                visible={editField === 'email'}
                title="ईमेल बदलें"
                value={userProfile.email || ''}
                onSave={(value) => handleSave('email', value)}
                onClose={() => setEditField(null)}
                keyboardType="email-address"
                placeholder="ईमेल दर्ज करें"
            />
            <EditModal
                visible={editField === 'address'}
                title="पता बदलें"
                value={storeSettings.address || ''}
                onSave={async (value) => {
                    const success = await updateStoreSettings({ address: value });
                    if (success) {
                        Alert.alert('सफलता', 'पता सफलतापूर्वक अपडेट हो गया।');
                    } else {
                        Alert.alert('त्रुटि', 'कुछ गलत हो गया।');
                    }
                }}
                onClose={() => setEditField(null)}
                placeholder="पता दर्ज करें"
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
                        <Text style={styles.imagePickerTitle}>प्रोफाइल फोटो बदलें</Text>
                        <TouchableOpacity
                            style={styles.imagePickerOption}
                            onPress={() => pickImage(true)}
                        >
                            <FontAwesome name="camera" size={20} color="#2E7D32" />
                            <Text style={styles.imagePickerOptionText}>कैमरा से फोटो लें</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={styles.imagePickerOption}
                            onPress={() => pickImage(false)}
                        >
                            <FontAwesome name="photo" size={20} color="#2E7D32" />
                            <Text style={styles.imagePickerOptionText}>गैलरी से चुनें</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={styles.imagePickerCancel}
                            onPress={() => setShowImagePicker(false)}
                        >
                            <Text style={styles.imagePickerCancelText}>रद्द करें</Text>
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
        backgroundColor: '#F5F5F5',
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#F5F5F5',
    },
    headerSafeArea: {
        backgroundColor: '#FFFFFF',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    backButton: {
        padding: 8,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: '#212121',
    },
    notificationButton: {
        padding: 8,
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
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginTop: 16,
        borderRadius: 12,
        shadowColor: '#000',
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
        backgroundColor: '#FFFFFF',
        borderWidth: 3,
        borderColor: '#FFFFFF',
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
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        paddingVertical: 6,
        alignItems: 'center',
    },
    userName: {
        fontSize: 24,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 4,
        textAlign: 'center',
    },
    nameEditIcon: {
        position: 'absolute',
        right: -24,
        top: 4,
    },
    storeName: {
        fontSize: 16,
        color: '#757575',
    },
    fieldsContainer: {
        paddingHorizontal: 16,
        marginTop: 8,
    },
    fieldCard: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: '#FFFFFF',
        padding: 16,
        marginBottom: 12,
        borderRadius: 12,
        shadowColor: '#000',
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
        color: '#757575',
        marginBottom: 4,
    },
    fieldValue: {
        fontSize: 16,
        color: '#212121',
        fontWeight: '500',
    },
    editIcon: {
        padding: 8,
    },
    settingsSection: {
        paddingHorizontal: 16,
        marginTop: 24,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 16,
    },
    settingCard: {
        backgroundColor: '#FFFFFF',
        padding: 16,
        marginBottom: 12,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    settingText: {
        fontSize: 16,
        color: '#212121',
        fontWeight: '500',
    },
    // Modal Styles
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 24,
    },
    modalContent: {
        backgroundColor: '#FFFFFF',
        borderRadius: 16,
        padding: 24,
        width: '100%',
        maxWidth: 340,
    },
    modalTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 16,
        textAlign: 'center',
    },
    modalInput: {
        borderWidth: 1,
        borderColor: '#E0E0E0',
        borderRadius: 8,
        padding: 12,
        fontSize: 16,
        color: '#212121',
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
        borderColor: '#E0E0E0',
        alignItems: 'center',
    },
    cancelButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#757575',
    },
    saveButton: {
        flex: 1,
        paddingVertical: 12,
        borderRadius: 8,
        backgroundColor: '#2E7D32',
        alignItems: 'center',
    },
    saveButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    // Image Picker Modal Styles
    imagePickerOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'flex-end',
    },
    imagePickerContent: {
        backgroundColor: '#FFFFFF',
        borderTopLeftRadius: 20,
        borderTopRightRadius: 20,
        paddingTop: 20,
        paddingBottom: 40,
        paddingHorizontal: 24,
    },
    imagePickerTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        textAlign: 'center',
        marginBottom: 20,
    },
    imagePickerOption: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#F5F5F5',
        gap: 16,
    },
    imagePickerOptionText: {
        fontSize: 16,
        color: '#212121',
        fontWeight: '500',
    },
    imagePickerCancel: {
        marginTop: 12,
        paddingVertical: 16,
        alignItems: 'center',
    },
    imagePickerCancelText: {
        fontSize: 16,
        color: '#757575',
        fontWeight: '600',
    },
});
