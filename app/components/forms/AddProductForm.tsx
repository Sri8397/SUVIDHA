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
} from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import * as ImagePicker from 'expo-image-picker';
import { useData } from '@/contexts/DataContext';
import { Category } from '@/types';

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
                setImage(result.assets[0].uri);
            }
        } catch (error) {
            console.error('Image picker error:', error);
            Alert.alert('त्रुटि', 'फोटो चुनने में समस्या हुई');
        }
    };

    const handleSubmit = async () => {
        // Validation
        if (!name.trim()) {
            Alert.alert('त्रुटि', 'प्रोडक्ट का नाम दर्ज करें');
            return;
        }
        if (!price || isNaN(Number(price)) || Number(price) <= 0) {
            Alert.alert('त्रुटि', 'सही कीमत दर्ज करें');
            return;
        }
        if (!stock || isNaN(Number(stock)) || Number(stock) < 0) {
            Alert.alert('त्रुटि', 'सही स्टॉक मात्रा दर्ज करें');
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
                Alert.alert('सफलता', 'प्रोडक्ट अपडेट हो गया');
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
                Alert.alert('सफलता', 'प्रोडक्ट जोड़ दिया गया');
            }

            resetForm();
            onClose();
        } catch (error) {
            console.error('Save product error:', error);
            Alert.alert('त्रुटि', 'प्रोडक्ट सेव करने में समस्या हुई');
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
            <View style={styles.container}>
                {/* Header */}
                <View style={styles.header}>
                    <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
                        <FontAwesome name="times" size={20} color="#757575" />
                    </TouchableOpacity>
                    <Text style={styles.title}>
                        {editProduct ? 'प्रोडक्ट एडिट करें' : 'नया प्रोडक्ट जोड़ें'}
                    </Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView style={styles.form} showsVerticalScrollIndicator={false}>
                    {/* Product Image */}
                    <View style={styles.imageSection}>
                        <Text style={styles.label}>प्रोडक्ट फोटो</Text>
                        <View style={styles.imageContainer}>
                            {image ? (
                                <Image source={{ uri: image }} style={styles.productImage} />
                            ) : (
                                <View style={styles.imagePlaceholder}>
                                    <FontAwesome name="image" size={40} color="#BDBDBD" />
                                </View>
                            )}
                        </View>
                        <View style={styles.imageButtons}>
                            <TouchableOpacity
                                style={styles.imageButton}
                                onPress={() => pickImage(true)}
                            >
                                <FontAwesome name="camera" size={16} color="#2E7D32" />
                                <Text style={styles.imageButtonText}>कैमरा</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.imageButton}
                                onPress={() => pickImage(false)}
                            >
                                <FontAwesome name="photo" size={16} color="#2E7D32" />
                                <Text style={styles.imageButtonText}>गैलरी</Text>
                            </TouchableOpacity>
                        </View>
                    </View>

                    {/* Product Name */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>प्रोडक्ट का नाम *</Text>
                        <TextInput
                            style={styles.input}
                            value={name}
                            onChangeText={setName}
                            placeholder="जैसे: पारले-जी बिस्कुट"
                            placeholderTextColor="#9E9E9E"
                        />
                    </View>

                    {/* Price */}
                    <View style={styles.row}>
                        <View style={[styles.inputGroup, { flex: 1, marginRight: 8 }]}>
                            <Text style={styles.label}>खरीद मूल्य (₹) *</Text>
                            <TextInput
                                style={styles.input}
                                value={costPrice}
                                onChangeText={setCostPrice}
                                placeholder="0"
                                placeholderTextColor="#9E9E9E"
                                keyboardType="numeric"
                            />
                        </View>
                        <View style={[styles.inputGroup, { flex: 1, marginLeft: 8 }]}>
                            <Text style={styles.label}>बिक्री मूल्य (₹) *</Text>
                            <TextInput
                                style={styles.input}
                                value={price}
                                onChangeText={setPrice}
                                placeholder="0"
                                placeholderTextColor="#9E9E9E"
                                keyboardType="numeric"
                            />
                        </View>
                    </View>

                    {/* Stock */}
                    <View style={styles.row}>
                        <View style={[styles.inputGroup, { flex: 1, marginRight: 8 }]}>
                            <Text style={styles.label}>वर्तमान स्टॉक {editProduct ? '' : '*'}</Text>
                            <TextInput
                                style={[styles.input, editProduct && styles.inputDisabled]}
                                value={stock}
                                onChangeText={editProduct ? undefined : setStock}
                                placeholder="0"
                                placeholderTextColor="#9E9E9E"
                                keyboardType="numeric"
                                editable={!editProduct}
                            />
                            {editProduct && (
                                <Text style={styles.helperText}>
                                    स्टॉक बदलने के लिए 'स्टॉक समायोजन' करें
                                </Text>
                            )}
                        </View>
                        <View style={[styles.inputGroup, { flex: 1, marginLeft: 8 }]}>
                            <Text style={styles.label}>न्यूनतम स्टॉक</Text>
                            <TextInput
                                style={styles.input}
                                value={minStock}
                                onChangeText={setMinStock}
                                placeholder="5"
                                placeholderTextColor="#9E9E9E"
                                keyboardType="numeric"
                            />
                        </View>
                    </View>

                    {/* Category */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>श्रेणी</Text>
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
                                        {cat.name}
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
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <Text style={styles.submitButtonText}>
                                {editProduct ? 'अपडेट करें' : 'प्रोडक्ट जोड़ें'}
                            </Text>
                        )}
                    </TouchableOpacity>
                </ScrollView>
            </View>
        </Modal>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
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
        color: '#212121',
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
        backgroundColor: '#F5F5F5',
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 2,
        borderColor: '#E0E0E0',
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
        borderColor: '#2E7D32',
    },
    imageButtonText: {
        fontSize: 14,
        fontWeight: '500',
        color: '#2E7D32',
    },
    inputGroup: {
        marginBottom: 16,
    },
    label: {
        fontSize: 14,
        fontWeight: '600',
        color: '#424242',
        marginBottom: 8,
    },
    input: {
        borderWidth: 1,
        borderColor: '#E0E0E0',
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 14,
        fontSize: 16,
        color: '#212121',
        backgroundColor: '#FAFAFA',
    },
    inputDisabled: {
        backgroundColor: '#F0F0F0',
        color: '#9E9E9E',
    },
    helperText: {
        fontSize: 11,
        color: '#757575',
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
        borderColor: '#E0E0E0',
        backgroundColor: '#FFFFFF',
        marginRight: 8,
    },
    categoryChipActive: {
        backgroundColor: '#2E7D32',
        borderColor: '#2E7D32',
    },
    categoryChipText: {
        fontSize: 14,
        color: '#616161',
    },
    categoryChipTextActive: {
        color: '#FFFFFF',
        fontWeight: '600',
    },
    submitButton: {
        backgroundColor: '#2E7D32',
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
        color: '#FFFFFF',
    },
});
