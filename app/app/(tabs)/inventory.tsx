import React, { useState, useMemo } from 'react';
import { View, Text, ScrollView, Image, StatusBar, TouchableOpacity, TextInput, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { Header } from '@/components/ui/header';
import { FAB } from '@/components/ui/fab';
import { useData } from '@/contexts/DataContext';
import { AddProductForm } from '@/components/forms/AddProductForm';
import { AddSaleForm } from '@/components/forms/AddSaleForm';
import { AddExpenseForm } from '@/components/forms/AddExpenseForm';
import { AddCustomerForm } from '@/components/forms/AddCustomerForm';
import { StockAdjustmentForm } from '@/components/forms/StockAdjustmentForm';
import { Product } from '@/types';

export default function InventoryScreen() {
    const { isLoading, products, categories, deleteProduct } = useData();
    const [selectedCategory, setSelectedCategory] = useState('all');
    const [searchQuery, setSearchQuery] = useState('');

    // Form states
    const [showProductForm, setShowProductForm] = useState(false);
    const [showSaleForm, setShowSaleForm] = useState(false);
    const [showExpenseForm, setShowExpenseForm] = useState(false);
    const [showCustomerForm, setShowCustomerForm] = useState(false);
    const [showStockAdjustForm, setShowStockAdjustForm] = useState(false);
    const [editingProduct, setEditingProduct] = useState<Product | undefined>();
    const [adjustingProduct, setAdjustingProduct] = useState<Product | undefined>();

    // Filter products
    const filteredProducts = useMemo(() => {
        return products.filter((product) => {
            const matchesCategory = selectedCategory === 'all' || product.category === selectedCategory;
            const matchesSearch = product.name.toLowerCase().includes(searchQuery.toLowerCase());
            return matchesCategory && matchesSearch;
        });
    }, [products, selectedCategory, searchQuery]);

    // Category list with "All" option
    const categoryList = useMemo(() => [
        { id: 'all', name: 'सभी' },
        ...categories
    ], [categories]);

    const handleEdit = (product: Product) => {
        setEditingProduct(product);
        setShowProductForm(true);
    };

    const handleDelete = (product: Product) => {
        Alert.alert(
            'प्रोडक्ट हटाएं',
            `क्या आप "${product.name}" को हटाना चाहते हैं?`,
            [
                { text: 'रद्द करें', style: 'cancel' },
                {
                    text: 'हटाएं',
                    style: 'destructive',
                    onPress: async () => {
                        const success = await deleteProduct(product.id);
                        if (success) {
                            Alert.alert('सफल', 'प्रोडक्ट हटा दिया गया');
                        }
                    }
                }
            ]
        );
    };

    const handleCloseProductForm = () => {
        setShowProductForm(false);
        setEditingProduct(undefined);
    };

    const handleStockAdjust = (product: Product) => {
        setAdjustingProduct(product);
        setShowStockAdjustForm(true);
    };

    const handleCloseStockAdjustForm = () => {
        setShowStockAdjustForm(false);
        setAdjustingProduct(undefined);
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
                    {categoryList.map((cat) => (
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
                                {cat.name}
                            </Text>
                        </TouchableOpacity>
                    ))}
                </ScrollView>
            </View>

            {/* Product Count */}
            <View style={styles.countContainer}>
                <Text style={styles.countText}>
                    {filteredProducts.length} प्रोडक्ट्स
                </Text>
            </View>

            {/* Product List */}
            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {filteredProducts.map((product) => (
                    <View key={product.id} style={styles.productCard}>
                        {product.image ? (
                            <Image source={{ uri: product.image }} style={styles.productImage} />
                        ) : (
                            <View style={[styles.productImage, styles.productImagePlaceholder]}>
                                <FontAwesome name="cube" size={32} color="#BDBDBD" />
                            </View>
                        )}
                        <View style={styles.productInfo}>
                            <Text style={styles.productName}>{product.name}</Text>
                            <View style={product.stock <= product.minStock ? styles.lowStockBadge : null}>
                                <Text style={[
                                    styles.productStock,
                                    product.stock <= product.minStock && styles.lowStockText
                                ]}>
                                    स्टॉक: {product.stock}
                                </Text>
                            </View>
                            <Text style={styles.productPrice}>₹ {product.price.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={styles.productActions}>
                            <TouchableOpacity
                                style={styles.stockButton}
                                onPress={() => handleStockAdjust(product)}
                            >
                                <FontAwesome name="cubes" size={14} color="#1976D2" />
                                <Text style={styles.stockButtonText}>स्टॉक</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.editButton}
                                onPress={() => handleEdit(product)}
                            >
                                <FontAwesome name="pencil" size={14} color="#2E7D32" />
                                <Text style={styles.editButtonText}>एडिट</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.deleteButton}
                                onPress={() => handleDelete(product)}
                            >
                                <FontAwesome name="trash" size={14} color="#D32F2F" />
                                <Text style={styles.deleteButtonText}>हटाएं</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                ))}

                {filteredProducts.length === 0 && (
                    <View style={styles.emptyState}>
                        <FontAwesome name="inbox" size={48} color="#BDBDBD" />
                        <Text style={styles.emptyStateText}>
                            {products.length === 0
                                ? 'कोई प्रोडक्ट नहीं है। नया प्रोडक्ट जोड़ें।'
                                : 'कोई प्रोडक्ट नहीं मिला'}
                        </Text>
                        {products.length === 0 && (
                            <TouchableOpacity
                                style={styles.addFirstButton}
                                onPress={() => setShowProductForm(true)}
                            >
                                <FontAwesome name="plus" size={14} color="#FFFFFF" />
                                <Text style={styles.addFirstButtonText}>प्रोडक्ट जोड़ें</Text>
                            </TouchableOpacity>
                        )}
                    </View>
                )}
            </ScrollView>

            {/* FAB */}
            <FAB
                onNewSale={() => setShowSaleForm(true)}
                onNewExpense={() => setShowExpenseForm(true)}
                onAddProduct={() => setShowProductForm(true)}
                onAddCustomer={() => setShowCustomerForm(true)}
            />

            {/* Forms */}
            <AddProductForm
                visible={showProductForm}
                onClose={handleCloseProductForm}
                editProduct={editingProduct}
            />
            <AddSaleForm visible={showSaleForm} onClose={() => setShowSaleForm(false)} />
            <AddExpenseForm visible={showExpenseForm} onClose={() => setShowExpenseForm(false)} />
            <AddCustomerForm visible={showCustomerForm} onClose={() => setShowCustomerForm(false)} />
            <StockAdjustmentForm
                visible={showStockAdjustForm}
                onClose={handleCloseStockAdjustForm}
                preSelectedProduct={adjustingProduct}
            />
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
    countContainer: {
        paddingHorizontal: 16,
        paddingBottom: 8,
    },
    countText: {
        fontSize: 14,
        color: '#757575',
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
    productImagePlaceholder: {
        alignItems: 'center',
        justifyContent: 'center',
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
    lowStockBadge: {
        backgroundColor: '#FFEBEE',
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 6,
        alignSelf: 'flex-start',
        marginBottom: 6,
    },
    lowStockText: {
        color: '#D32F2F',
        fontWeight: '600',
        marginBottom: 0,
    },
    productPrice: {
        fontSize: 18,
        fontWeight: '700',
        color: '#2E7D32',
    },
    productActions: {
        flexDirection: 'row',
        paddingTop: 12,
        borderTopWidth: 1,
        borderTopColor: '#F5F5F5',
        marginTop: 0,
    },
    stockButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
    },
    stockButtonText: {
        fontSize: 14,
        fontWeight: '500',
        color: '#1976D2',
    },
    editButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 6,
    },
    editButtonText: {
        fontSize: 14,
        fontWeight: '500',
        color: '#2E7D32',
    },
    deleteButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
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
        textAlign: 'center',
        paddingHorizontal: 32,
    },
    addFirstButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        backgroundColor: '#2E7D32',
        paddingHorizontal: 20,
        paddingVertical: 12,
        borderRadius: 8,
        marginTop: 20,
    },
    addFirstButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#FFFFFF',
    },
});
