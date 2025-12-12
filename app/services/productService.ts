// ============================================
// Product Service - CRUD Operations
// ============================================

import { Product, Category, DEFAULT_CATEGORIES } from '@/types';
import { getData, setData, generateId, getTimestamp } from './storage';
import { getKeysForCurrentMode } from './modeStorage';

/**
 * Get all products
 */
export async function getProducts(): Promise<Product[]> {
    const keys = getKeysForCurrentMode();
    const products = await getData<Product[]>(keys.PRODUCTS);
    return products || [];
}

/**
 * Get product by ID
 */
export async function getProductById(id: string): Promise<Product | null> {
    const products = await getProducts();
    return products.find(p => p.id === id) || null;
}

/**
 * Add new product
 */
export async function addProduct(
    product: Omit<Product, 'id' | 'createdAt' | 'updatedAt'>
): Promise<Product> {
    const keys = getKeysForCurrentMode();
    const products = await getProducts();
    const newProduct: Product = {
        ...product,
        id: generateId(),
        createdAt: getTimestamp(),
        updatedAt: getTimestamp(),
    };
    products.push(newProduct);
    await setData(keys.PRODUCTS, products);
    return newProduct;
}

/**
 * Update existing product
 */
export async function updateProduct(
    id: string,
    updates: Partial<Omit<Product, 'id' | 'createdAt'>>
): Promise<Product | null> {
    const keys = getKeysForCurrentMode();
    const products = await getProducts();
    const index = products.findIndex(p => p.id === id);
    if (index === -1) return null;

    products[index] = {
        ...products[index],
        ...updates,
        updatedAt: getTimestamp(),
    };
    await setData(keys.PRODUCTS, products);
    return products[index];
}

/**
 * Delete product
 */
export async function deleteProduct(id: string): Promise<boolean> {
    const keys = getKeysForCurrentMode();
    const products = await getProducts();
    const filtered = products.filter(p => p.id !== id);
    if (filtered.length === products.length) return false;
    await setData(keys.PRODUCTS, filtered);
    return true;
}

/**
 * Get products with low stock (at or below minStock)
 */
export async function getLowStockProducts(): Promise<Product[]> {
    const products = await getProducts();
    return products.filter(p => p.stock <= p.minStock);
}

/**
 * Get products by category
 */
export async function getProductsByCategory(categoryId: string): Promise<Product[]> {
    const products = await getProducts();
    if (categoryId === 'all') return products;
    return products.filter(p => p.category === categoryId);
}

/**
 * Update product stock (for sales)
 */
export async function updateProductStock(
    id: string,
    quantityChange: number
): Promise<Product | null> {
    const product = await getProductById(id);
    if (!product) return null;

    const newStock = Math.max(0, product.stock + quantityChange);
    return updateProduct(id, { stock: newStock });
}

/**
 * Search products by name
 */
export async function searchProducts(query: string): Promise<Product[]> {
    const products = await getProducts();
    const lowerQuery = query.toLowerCase();
    return products.filter(p =>
        p.name.toLowerCase().includes(lowerQuery)
    );
}

// ============================================
// Category Operations
// ============================================

/**
 * Get all categories
 */
export async function getCategories(): Promise<Category[]> {
    const keys = getKeysForCurrentMode();
    const categories = await getData<Category[]>(keys.CATEGORIES);
    // Return default categories if none saved
    if (!categories || categories.length === 0) {
        await setData(keys.CATEGORIES, DEFAULT_CATEGORIES);
        return DEFAULT_CATEGORIES;
    }
    return categories;
}

/**
 * Add new category
 */
export async function addCategory(name: string): Promise<Category> {
    const keys = getKeysForCurrentMode();
    const categories = await getCategories();
    const newCategory: Category = {
        id: generateId(),
        name,
    };
    categories.push(newCategory);
    await setData(keys.CATEGORIES, categories);
    return newCategory;
}

