import React, { useState, useMemo, useEffect } from 'react';
import { View, Text, ScrollView, Image, StatusBar, TouchableOpacity, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useLocalSearchParams } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { Header } from '@/components/ui/header';
import { FAB } from '@/components/ui/fab';
import { TransliterationInput } from '@/components/ui/TransliterationInput';
import { useData } from '@/contexts/DataContext';
import { AddProductForm } from '@/components/forms/AddProductForm';
import { AddSaleForm } from '@/components/forms/AddSaleForm';
import { AddExpenseForm } from '@/components/forms/AddExpenseForm';
import { AddCustomerForm } from '@/components/forms/AddCustomerForm';
import { StockAdjustmentForm } from '@/components/forms/StockAdjustmentForm';
import { Product } from '@/types';
import { Colors } from '@/constants/theme';

export default function InventoryScreen() {
    const { t } = useTranslation();
    const { filter } = useLocalSearchParams<{ filter?: string }>();
    const { isLoading, products, categories, lowStockProducts, deleteProduct } = useData();
    const [selectedCategory, setSelectedCategory] = useState('all');
    const [searchQuery, setSearchQuery] = useState('');

    // Set initial filter based on URL params
    useEffect(() => {
        if (filter === 'low-stock') {
            setSelectedCategory('low-stock');
        }
    }, [filter]);

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
        // Handle low stock filter
        if (selectedCategory === 'low-stock') {
            return lowStockProducts.filter((product) =>
                product.name.toLowerCase().includes(searchQuery.toLowerCase())
            );
        }
        return products.filter((product) => {
            const matchesCategory = selectedCategory === 'all' || product.category === selectedCategory;
            const matchesSearch = product.name.toLowerCase().includes(searchQuery.toLowerCase());
            return matchesCategory && matchesSearch;
        });
    }, [products, lowStockProducts, selectedCategory, searchQuery]);

    // Category list with "All" and "Low Stock" options
    const categoryList = useMemo(() => [
        { id: 'all', name: t('inventory.all') },
        { id: 'low-stock', name: t('dashboard.lowStockAlerts'), count: lowStockProducts.length },
        ...categories.map(cat => ({
            ...cat,
            name: t(`categories.${cat.id}`, { defaultValue: cat.name })
        }))
    ], [categories, lowStockProducts.length, t]);

    const handleEdit = (product: Product) => {
        setEditingProduct(product);
        setShowProductForm(true);
    };

    const handleDelete = (product: Product) => {
        Alert.alert(
            t('inventory.deleteProduct'),
            t('inventory.deleteProductConfirm', { name: product.name }),
            [
                { text: t('common.cancel'), style: 'cancel' },
                {
                    text: t('common.delete'),
                    style: 'destructive',
                    onPress: async () => {
                        const success = await deleteProduct(product.id);
                        if (success) {
                            Alert.alert(t('common.success'), t('inventory.productDeleted'));
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
                <ActivityIndicator size="large" color={Colors.light.primary} />
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />

            {/* Header */}
            <Header title={t('inventory.title')} showLanguageToggle={true} />

            {/* Search Bar */}
            <View style={styles.searchContainer}>
                <FontAwesome name="search" size={18} color={Colors.light.textTertiary} />
                <TransliterationInput
                    style={styles.searchInput}
                    placeholder={t('common.search')}
                    placeholderTextColor={Colors.light.textTertiary}
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
                                selectedCategory === cat.id && styles.filterChipActive,
                                cat.id === 'low-stock' && styles.lowStockChip
                            ]}
                        >
                            <Text style={[
                                styles.filterChipText,
                                selectedCategory === cat.id && styles.filterChipTextActive,
                                cat.id === 'low-stock' && selectedCategory !== cat.id && styles.lowStockChipText
                            ]}>
                                {cat.name}{'count' in cat && cat.count > 0 ? ` (${cat.count})` : ''}
                            </Text>
                        </TouchableOpacity>
                    ))}
                </ScrollView>
            </View>

            {/* Product Count */}
            <View style={styles.countContainer}>
                <Text style={styles.countText}>
                    {filteredProducts.length} {t('inventory.products')}
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
                                <FontAwesome name="cube" size={32} color={Colors.light.textTertiary} />
                            </View>
                        )}
                        <View style={styles.productInfo}>
                            <Text style={styles.productName}>{product.name}</Text>
                            <View style={product.stock <= product.minStock ? styles.lowStockBadge : null}>
                                <Text style={[
                                    styles.productStock,
                                    product.stock <= product.minStock && styles.lowStockText
                                ]}>
                                    {t('inventory.stock')}: {product.stock}
                                </Text>
                            </View>
                            <Text style={styles.productPrice}>₹ {product.price.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={styles.productActions}>
                            <TouchableOpacity
                                style={styles.stockButton}
                                onPress={() => handleStockAdjust(product)}
                            >
                                <FontAwesome name="cubes" size={14} color={Colors.light.info} />
                                <Text style={styles.stockButtonText}>{t('inventory.stock')}</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.editButton}
                                onPress={() => handleEdit(product)}
                            >
                                <FontAwesome name="pencil" size={14} color={Colors.light.primary} />
                                <Text style={styles.editButtonText}>{t('common.edit')}</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.deleteButton}
                                onPress={() => handleDelete(product)}
                            >
                                <FontAwesome name="trash" size={14} color={Colors.light.error} />
                                <Text style={styles.deleteButtonText}>{t('common.delete')}</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                ))}

                {filteredProducts.length === 0 && (
                    <View style={styles.emptyState}>
                        <FontAwesome name="inbox" size={48} color={Colors.light.textTertiary} />
                        <Text style={styles.emptyStateText}>
                            {products.length === 0
                                ? t('inventory.noProducts')
                                : t('inventory.noProductsFound')}
                        </Text>
                        {products.length === 0 && (
                            <TouchableOpacity
                                style={styles.addFirstButton}
                                onPress={() => setShowProductForm(true)}
                            >
                                <FontAwesome name="plus" size={14} color={Colors.light.white} />
                                <Text style={styles.addFirstButtonText}>{t('inventory.addProduct')}</Text>
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
        backgroundColor: Colors.light.background,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: Colors.light.white,
    },

    searchContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: Colors.light.white,
        marginHorizontal: 16,
        marginVertical: 12,
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 12,
        gap: 12,
        borderWidth: 1,
        borderColor: Colors.light.border,
    },
    searchInput: {
        flex: 1,
        fontSize: 16,
        color: Colors.light.text,
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
        borderColor: Colors.light.border,
        backgroundColor: Colors.light.white,
        marginRight: 8,
    },
    filterChipActive: {
        backgroundColor: Colors.light.primary,
        borderColor: Colors.light.primary,
    },
    filterChipText: {
        fontSize: 14,
        fontWeight: '500',
        color: Colors.light.textSecondary,
    },
    filterChipTextActive: {
        color: Colors.light.white,
    },
    lowStockChip: {
        borderColor: Colors.light.errorLight,
        backgroundColor: Colors.light.errorLight,
    },
    lowStockChipText: {
        color: Colors.light.error,
    },
    countContainer: {
        paddingHorizontal: 16,
        paddingBottom: 8,
    },
    countText: {
        fontSize: 14,
        color: Colors.light.textSecondary,
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 100,
        paddingTop: 8,
    },
    productCard: {
        backgroundColor: Colors.light.white,
        marginHorizontal: 16,
        marginBottom: 12,
        borderRadius: 12,
        padding: 16,
        shadowColor: Colors.light.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    productImage: {
        width: 80,
        height: 80,
        borderRadius: 8,
        backgroundColor: Colors.light.background,
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
        color: Colors.light.text,
        marginBottom: 4,
    },
    productStock: {
        fontSize: 14,
        color: Colors.light.textSecondary,
        marginBottom: 4,
    },
    lowStockBadge: {
        backgroundColor: Colors.light.errorLight,
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 6,
        alignSelf: 'flex-start',
        marginBottom: 6,
    },
    lowStockText: {
        color: Colors.light.error,
        fontWeight: '600',
        marginBottom: 0,
    },
    productPrice: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.primary,
    },
    productActions: {
        flexDirection: 'row',
        paddingTop: 12,
        borderTopWidth: 1,
        borderTopColor: Colors.light.background,
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
        color: Colors.light.info,
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
        color: Colors.light.primary,
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
        color: Colors.light.error,
    },
    emptyState: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 80,
    },
    emptyStateText: {
        fontSize: 16,
        color: Colors.light.textSecondary,
        marginTop: 16,
        textAlign: 'center',
        paddingHorizontal: 32,
    },
    addFirstButton: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        backgroundColor: Colors.light.primary,
        paddingHorizontal: 20,
        paddingVertical: 12,
        borderRadius: 8,
        marginTop: 20,
    },
    addFirstButtonText: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.white,
    },
});
