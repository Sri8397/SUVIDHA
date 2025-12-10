import React, { useState } from 'react';
import { View, Text, ScrollView, Image, StatusBar, TouchableOpacity, TextInput, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { Header } from '@/components/ui/header';

// Mock data
const categories = [
    { id: 'all', label: 'सभी' },
    { id: 'grocery', label: 'किराना' },
    { id: 'snacks', label: 'स्नैक्स' },
    { id: 'beverages', label: 'पेय' },
];

const mockProducts = [
    { id: '1', name: 'पारले-जी बिस्कुट', stock: 5, price: 20, image: 'https://m.media-amazon.com/images/I/61WfCf0CEQL.jpg', category: 'snacks' },
    { id: '2', name: 'आशीर्वाद आटा', stock: 2, price: 450, image: 'https://m.media-amazon.com/images/I/71xWPpJdpwL.jpg', category: 'grocery' },
    { id: '3', name: 'मैगी नूडल्स', stock: 15, price: 144, image: 'https://m.media-amazon.com/images/I/71dZMB+-ObL.jpg', category: 'snacks' },
];

export default function InventoryScreen() {
    const [selectedCategory, setSelectedCategory] = useState('all');
    const [searchQuery, setSearchQuery] = useState('');

    const filteredProducts = mockProducts.filter((product) => {
        const matchesCategory = selectedCategory === 'all' || product.category === selectedCategory;
        const matchesSearch = product.name.toLowerCase().includes(searchQuery.toLowerCase());
        return matchesCategory && matchesSearch;
    });

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            {/* Header */}
            <Header title="इन्वेंटरी" showLanguageToggle={true} />

            {/* Search Bar */}
            <View style={styles.searchContainer}>
                <FontAwesome name="search" size={18} color="#9E9E9E" />
                <TextInput
                    style={styles.searchInput}
                    placeholder="खोजें..."
                    placeholderTextColor="#9E9E9E"
                    value={searchQuery}
                    onChangeText={setSearchQuery}
                />
            </View>

            {/* Category Filters */}
            <View style={styles.filterWrapper}>
                <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    contentContainerStyle={styles.filtersContainer}
                    style={styles.filterScrollView}
                >
                    {categories.map((cat) => (
                        <TouchableOpacity
                            key={cat.id}
                            onPress={() => setSelectedCategory(cat.id)}
                            style={[
                                styles.filterChip,
                                selectedCategory === cat.id && styles.filterChipActive
                            ]}
                        >
                            <Text style={[
                                styles.filterChipText,
                                selectedCategory === cat.id && styles.filterChipTextActive
                            ]}>
                                {cat.label}
                            </Text>
                        </TouchableOpacity>
                    ))}
                </ScrollView>
            </View>

            {/* Product List */}
            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {filteredProducts.map((product) => (
                    <View key={product.id} style={styles.productCard}>
                        <Image source={{ uri: product.image }} style={styles.productImage} />
                        <View style={styles.productInfo}>
                            <Text style={styles.productName}>{product.name}</Text>
                            <Text style={styles.productStock}>स्टॉक: {product.stock}</Text>
                            <Text style={styles.productPrice}>₹ {product.price}</Text>
                        </View>
                        <View style={styles.productActions}>
                            <TouchableOpacity style={styles.editButton}>
                                <FontAwesome name="pencil" size={14} color="#2E7D32" />
                                <Text style={styles.editButtonText}>एडिट करें</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.deleteButton}>
                                <FontAwesome name="trash" size={14} color="#D32F2F" />
                                <Text style={styles.deleteButtonText}>हटाएं</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                ))}

                {filteredProducts.length === 0 && (
                    <View style={styles.emptyState}>
                        <FontAwesome name="inbox" size={48} color="#BDBDBD" />
                        <Text style={styles.emptyStateText}>कोई प्रोडक्ट नहीं मिला</Text>
                    </View>
                )}
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
    headerSafeArea: {
        backgroundColor: '#FFFFFF',
    },
    searchContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginVertical: 12,
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 12,
        gap: 12,
        borderWidth: 1,
        borderColor: '#E0E0E0',
    },
    searchInput: {
        flex: 1,
        fontSize: 16,
        color: '#212121',
    },
    filterWrapper: {
        height: 48,
    },
    filterScrollView: {
        flexGrow: 0,
    },
    filtersContainer: {
        flexDirection: 'row',
        paddingHorizontal: 16,
        paddingVertical: 8,
        alignItems: 'center',
    },
    filterChip: {
        paddingHorizontal: 16,
        paddingVertical: 6,
        borderRadius: 16,
        borderWidth: 1,
        borderColor: '#E0E0E0',
        backgroundColor: '#FFFFFF',
        marginRight: 8,
    },
    filterChipActive: {
        backgroundColor: '#2E7D32',
        borderColor: '#2E7D32',
    },
    filterChipText: {
        fontSize: 14,
        fontWeight: '500',
        color: '#616161',
    },
    filterChipTextActive: {
        color: '#FFFFFF',
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 100,
        paddingTop: 8,
    },
    productCard: {
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginBottom: 12,
        borderRadius: 12,
        padding: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    productImage: {
        width: 80,
        height: 80,
        borderRadius: 8,
        backgroundColor: '#F5F5F5',
        position: 'absolute',
        left: 16,
        top: 16,
    },
    productInfo: {
        marginLeft: 96,
        marginBottom: 12,
        minHeight: 80,
        justifyContent: 'center',
    },
    productName: {
        fontSize: 16,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 4,
    },
    productStock: {
        fontSize: 14,
        color: '#757575',
        marginBottom: 4,
    },
    productPrice: {
        fontSize: 18,
        fontWeight: '700',
        color: '#2E7D32',
    },
    productActions: {
        flexDirection: 'row',
        gap: 16,
        paddingTop: 12,
        borderTopWidth: 1,
        borderTopColor: '#F5F5F5',
        marginTop: 0,
    },
    editButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
    },
    editButtonText: {
        fontSize: 14,
        fontWeight: '500',
        color: '#2E7D32',
    },
    deleteButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
    },
    deleteButtonText: {
        fontSize: 14,
        fontWeight: '500',
        color: '#D32F2F',
    },
    emptyState: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 80,
    },
    emptyStateText: {
        fontSize: 16,
        color: '#757575',
        marginTop: 16,
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
