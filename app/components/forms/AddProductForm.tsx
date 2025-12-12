// ============================================
// Add Product Form - Modal Form Component
// ============================================

import React, { useState, useEffect } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    Modal,
    ScrollView,
    StyleSheet,
    Image,
    Alert,
    ActivityIndicator,
    KeyboardAvoidingView,
    Platform,
} from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { TransliterationInput } from '../ui/TransliterationInput';
import { useTranslation } from 'react-i18next';
import * as ImagePicker from 'expo-image-picker';
import { useData } from '@/contexts/DataContext';
import { Colors } from '@/constants/theme';



interface AddProductFormProps {
    visible: boolean;
    onClose: () => void;
    editProduct?: {
        id: string;
        name: string;
        price: number;
        costPrice: number;
        stock: number;
        minStock: number;
        category: string;
        image?: string;
    };
}

export function AddProductForm({ visible, onClose, editProduct }: AddProductFormProps) {
    const { t } = useTranslation();
    const { addProduct, updateProduct, categories } = useData();
    const [isLoading, setIsLoading] = useState(false);

    // Form state
    const [name, setName] = useState('');
    const [price, setPrice] = useState('');
    const [costPrice, setCostPrice] = useState('');
    const [stock, setStock] = useState('');
    const [minStock, setMinStock] = useState('5');
    const [category, setCategory] = useState('grocery');
    const [image, setImage] = useState<string | undefined>(undefined);

    // Sync form state when editProduct changes or modal opens
    useEffect(() => {
        if (visible) {
            if (editProduct) {
                setName(editProduct.name || '');
                setPrice(editProduct.price?.toString() || '');
                setCostPrice(editProduct.costPrice?.toString() || '');
                setStock(editProduct.stock?.toString() || '');
                setMinStock(editProduct.minStock?.toString() || '5');
                setCategory(editProduct.category || 'grocery');
                setImage(editProduct.image);
            } else {
                // Reset form for new product
                setName('');
                setPrice('');
                setCostPrice('');
                setStock('');
                setMinStock('5');
                setCategory('grocery');
                setImage(undefined);
            }
        }
    }, [visible, editProduct]);

    const resetForm = () => {
        setName('');
        setPrice('');
        setCostPrice('');
        setStock('');
        setMinStock('5');
        setCategory('grocery');
        setImage(undefined);
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
                setImage(result.assets[0].uri);
            }
        } catch (error) {
            console.error('Image picker error:', error);
            Alert.alert(t('common.error'), t('forms.imageError'));
        }
    };

    const handleSubmit = async () => {
        // Validation
        if (!name.trim()) {
            Alert.alert(t('common.error'), t('forms.productNameRequired'));
            return;
        }
        if (!price || isNaN(Number(price)) || Number(price) <= 0) {
            Alert.alert(t('common.error'), t('forms.priceRequired'));
            return;
        }
        if (!stock || isNaN(Number(stock)) || Number(stock) < 0) {
            Alert.alert(t('common.error'), t('forms.stockRequired'));
            return;
        }

        setIsLoading(true);
        try {
            if (editProduct) {
                // When editing, don't update stock (use StockAdjustmentForm instead)
                const updateData = {
                    name: name.trim(),
                    price: Number(price),
                    costPrice: Number(costPrice) || 0,
                    minStock: Number(minStock) || 5,
                    category,
                    image,
                };
                await updateProduct(editProduct.id, updateData);
                Alert.alert(t('common.success'), t('inventory.productUpdated'));
            } else {
                // New product - include stock
                const productData = {
                    name: name.trim(),
                    price: Number(price),
                    costPrice: Number(costPrice) || 0,
                    stock: Number(stock),
                    minStock: Number(minStock) || 5,
                    category,
                    image,
                };
                await addProduct(productData);
                Alert.alert(t('common.success'), t('inventory.productAdded'));
            }

            resetForm();
            onClose();
        } catch (error) {
            console.error('Save product error:', error);
            Alert.alert(t('common.error'), t('forms.saveError'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        if (!editProduct) resetForm();
        onClose();
    };

    return (
        <Modal
            visible={visible}
            animationType="slide"
            presentationStyle="pageSheet"
            onRequestClose={handleClose}
        >
            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                style={styles.container}
            >
                {/* Header */}
                <View style={styles.header}>
                    <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
                        <FontAwesome name="times" size={20} color={Colors.light.textSecondary} />
                    </TouchableOpacity>
                    <Text style={styles.title}>
                        {editProduct ? t('common.edit') + ' ' + t('inventory.product') : t('inventory.addProduct')}
                    </Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView
                    style={styles.form}
                    showsVerticalScrollIndicator={false}
                    keyboardShouldPersistTaps="handled"
                >
                    {/* Product Image */}
                    <View style={styles.imageSection}>
                        <Text style={styles.label}>{t('forms.productImage')}</Text>
                        <View style={styles.imageContainer}>
                            {image ? (
                                <Image source={{ uri: image }} style={styles.productImage} />
                            ) : (
                                <View style={styles.imagePlaceholder}>
                                    <FontAwesome name="image" size={40} color={Colors.light.textTertiary} />
                                </View>
                            )}
                        </View>
                        <View style={styles.imageButtons}>
                            <TouchableOpacity
                                style={styles.imageButton}
                                onPress={() => pickImage(true)}
                            >
                                <FontAwesome name="camera" size={16} color={Colors.light.primary} />
                                <Text style={styles.imageButtonText}>{t('forms.camera')}</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.imageButton}
                                onPress={() => pickImage(false)}
                            >
                                <FontAwesome name="photo" size={16} color={Colors.light.primary} />
                                <Text style={styles.imageButtonText}>{t('forms.gallery')}</Text>
                            </TouchableOpacity>
                        </View>
                    </View>

                    {/* Product Name */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.productName')} *</Text>
                        <TransliterationInput
                            style={styles.input}
                            value={name}
                            onChangeText={setName}
                            placeholder={t('forms.productName')}
                            placeholderTextColor={Colors.light.textTertiary}
                        />
                    </View>

                    {/* Price */}
                    <View style={styles.row}>
                        <View style={[styles.inputGroup, styles.halfInputLeft]}>
                            <Text style={styles.label}>{t('forms.costPrice')} (₹) *</Text>
                            <TextInput
                                style={styles.input}
                                value={costPrice}
                                onChangeText={(text) => setCostPrice(text.replace(/[^0-9.]/g, ''))}
                                placeholder="0"
                                placeholderTextColor={Colors.light.textTertiary}
                                keyboardType="numeric"
                            />
                        </View>
                        <View style={[styles.inputGroup, styles.halfInputRight]}>
                            <Text style={styles.label}>{t('forms.sellingPrice')} (₹) *</Text>
                            <TextInput
                                style={styles.input}
                                value={price}
                                onChangeText={(text) => setPrice(text.replace(/[^0-9.]/g, ''))}
                                placeholder="0"
                                placeholderTextColor={Colors.light.textTertiary}
                                keyboardType="numeric"
                            />
                        </View>
                    </View>

                    {/* Stock */}
                    <View style={styles.row}>
                        <View style={[styles.inputGroup, styles.halfInputLeft]}>
                            <Text style={styles.label}>{t('forms.currentStock')} {editProduct ? '' : '*'}</Text>
                            <TextInput
                                style={[styles.input, editProduct && styles.inputDisabled]}
                                value={stock}
                                onChangeText={editProduct ? undefined : (text) => setStock(text.replace(/[^0-9.]/g, ''))}
                                placeholder="0"
                                placeholderTextColor={Colors.light.textTertiary}
                                keyboardType="numeric"
                                editable={!editProduct}
                            />
                            {editProduct && (
                                <Text style={styles.helperText}>
                                    {t('forms.stockAdjustmentHelper', "Use 'Stock Adjustment' to change stock")}
                                </Text>
                            )}
                        </View>
                        <View style={[styles.inputGroup, styles.halfInputRight]}>
                            <Text style={styles.label}>{t('forms.minStock')}</Text>
                            <TextInput
                                style={styles.input}
                                value={minStock}
                                onChangeText={(text) => setMinStock(text.replace(/[^0-9.]/g, ''))}
                                placeholder="5"
                                placeholderTextColor={Colors.light.textTertiary}
                                keyboardType="numeric"
                            />
                        </View>
                    </View>

                    {/* Category */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.category')}</Text>
                        <ScrollView
                            horizontal
                            showsHorizontalScrollIndicator={false}
                            style={styles.categoryScroll}
                        >
                            {categories.map((cat) => (
                                <TouchableOpacity
                                    key={cat.id}
                                    style={[
                                        styles.categoryChip,
                                        category === cat.id && styles.categoryChipActive,
                                    ]}
                                    onPress={() => setCategory(cat.id)}
                                >
                                    <Text
                                        style={[
                                            styles.categoryChipText,
                                            category === cat.id && styles.categoryChipTextActive,
                                        ]}
                                    >
                                        {t(`categories.${cat.id}`, { defaultValue: cat.name })}
                                    </Text>
                                </TouchableOpacity>
                            ))}
                        </ScrollView>
                    </View>

                    {/* Submit Button */}
                    <TouchableOpacity
                        style={[styles.submitButton, isLoading && styles.submitButtonDisabled]}
                        onPress={handleSubmit}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <ActivityIndicator color={Colors.light.white} />
                        ) : (
                            <Text style={styles.submitButtonText}>
                                {editProduct ? t('common.save') : t('inventory.addProduct')}
                            </Text>
                        )}
                    </TouchableOpacity>
                </ScrollView>
            </KeyboardAvoidingView>
        </Modal>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.light.card,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.border,
    },
    closeButton: {
        width: 40,
        height: 40,
        alignItems: 'center',
        justifyContent: 'center',
    },
    title: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
    },
    form: {
        flex: 1,
        padding: 16,
    },
    imageSection: {
        alignItems: 'center',
        marginBottom: 24,
    },
    imageContainer: {
        marginVertical: 12,
    },
    productImage: {
        width: 120,
        height: 120,
        borderRadius: 12,
    },
    imagePlaceholder: {
        width: 120,
        height: 120,
        borderRadius: 12,
        backgroundColor: Colors.light.background,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 2,
        borderColor: Colors.light.border,
        borderStyle: 'dashed',
    },
    imageButtons: {
        flexDirection: 'row',
        gap: 12,
    },
    imageButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        paddingHorizontal: 16,
        paddingVertical: 10,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: Colors.light.primary,
    },
    imageButtonText: {
        fontSize: 14,
        fontWeight: '500',
        color: Colors.light.primary,
    },
    inputGroup: {
        marginBottom: 16,
    },
    label: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.text,
        marginBottom: 8,
    },
    input: {
        borderWidth: 1,
        borderColor: Colors.light.border,
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 14,
        fontSize: 16,
        color: Colors.light.text,
        backgroundColor: Colors.light.background,
    },
    inputDisabled: {
        backgroundColor: Colors.light.background,
        color: Colors.light.textTertiary,
    },
    helperText: {
        fontSize: 11,
        color: Colors.light.textSecondary,
        marginTop: 4,
        fontStyle: 'italic',
    },
    row: {
        flexDirection: 'row',
    },
    categoryScroll: {
        marginTop: 4,
    },
    categoryChip: {
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 20,
        borderWidth: 1,
        borderColor: Colors.light.border,
        backgroundColor: Colors.light.card,
        marginRight: 8,
    },
    categoryChipActive: {
        backgroundColor: Colors.light.primary,
        borderColor: Colors.light.primary,
    },
    categoryChipText: {
        fontSize: 14,
        color: Colors.light.textSecondary,
    },
    categoryChipTextActive: {
        color: Colors.light.white,
        fontWeight: '600',
    },
    submitButton: {
        backgroundColor: Colors.light.primary,
        borderRadius: 12,
        paddingVertical: 16,
        alignItems: 'center',
        marginTop: 24,
        marginBottom: 32,
    },
    submitButtonDisabled: {
        opacity: 0.7,
    },
    submitButtonText: {
        fontSize: 16,
        fontWeight: '700',
        color: Colors.light.white,
    },
    halfInputLeft: {
        flex: 1,
        marginRight: 8,
    },
    halfInputRight: {
        flex: 1,
        marginLeft: 8,
    },
});
