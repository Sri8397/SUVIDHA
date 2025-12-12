// ============================================
// Legacy Invoice Types (from earlier development)
// ============================================

export interface InvoiceItem {
    id: string;
    description: string;
    quantity: number;
    unitPrice: number;
    taxRate: number; // e.g., 18 for 18%
}

export interface ClientDetails {
    name: string;
    gstin: string;
    address: string;
}

export type TaxType = 'CGST_SGST' | 'IGST';

export interface Invoice {
    id: string;
    date: string;
    client: ClientDetails;
    items: InvoiceItem[];
    subtotal: number;
    taxType: TaxType;
    taxAmount: number;
    total: number;
    status: 'Paid' | 'Pending';
}

export interface CompanyDetails {
    name: string;
    gstin: string;
    address: string;
    phone: string;
}

// ============================================
// Suvidha App - Dynamic Data Types
// ============================================

// Product Entity
export interface Product {
    id: string;
    name: string;
    price: number; // Selling price
    costPrice: number; // Cost/Purchase price for profit calculation
    stock: number;
    minStock: number; // Alert threshold
    category: string;
    image?: string; // Local file URI from camera/gallery
    createdAt: string;
    updatedAt: string;
}

// Product Category
export interface Category {
    id: string;
    name: string;
    icon?: string;
}

// Employee Entity
export interface Employee {
    id: string;
    name: string;
    role: string;  // e.g., 'सेल्समैन', 'कैशियर', 'स्टॉक मैनेजर'
    phone: string;
    salary?: number;
    createdAt: string;
    updatedAt: string;
}

// Customer Entity
export interface Customer {
    id: string;
    name: string;
    phone: string;
    address?: string;
    outstanding: number; // Total pending payment
    createdAt: string;
    updatedAt: string;
}

// Sale Item (line item in a sale)
export interface SaleItem {
    productId: string;
    productName: string;
    quantity: number;
    unitPrice: number;
    costPrice: number; // Cost price at time of sale for profit calculation
    total: number;
}

// Sale Type - distinguishes between product sale, bakaya payment and income
export type SaleType = 'PRODUCT_SALE' | 'BAKAYA_PAYMENT' | 'INCOME';

// Income Source (for Personal Finance mode)
export type IncomeSource = 'salary' | 'freelance' | 'business' | 'investment' | 'gift' | 'other';

// Sale/Transaction Entity
export interface Sale {
    id: string;
    saleType: SaleType;          // NEW: Type of sale
    customerId?: string;          // Optional - for walk-in customers
    items: SaleItem[];            // Empty for BAKAYA_PAYMENT
    subtotal: number;
    discount: number;
    total: number;
    amountPaid: number;
    paymentStatus: 'paid' | 'partial' | 'pending';
    paymentMethod: 'cash' | 'upi' | 'card' | 'credit';
    date: string;
    createdAt: string;
    incomeSource?: IncomeSource;  // For INCOME type sales in Personal Finance mode
}

// Expense Category
export type ExpenseCategory =
    | 'rent'
    | 'electricity'
    | 'salary'
    | 'inventory'
    | 'transport'
    | 'maintenance'
    | 'stock_received'    // Stock received (purchase, return)
    | 'stock_wastage'     // Damaged, expired, lost
    | 'other';

// Expense Entity
export interface Expense {
    id: string;
    category: ExpenseCategory;
    amount: number;
    description: string;
    date: string;
    createdAt: string;
}

// Dashboard Statistics (computed)
export interface DashboardStats {
    todaySales: number;
    pendingPayments: number;
    profit: number;
    expenses: number;
}

// Weekly Data for Charts
export interface WeeklyData {
    sales: number[];
    profit: number[];
    labels: string[];
}

// Category Sales Breakdown
export interface CategoryBreakdown {
    name: string;
    percentage: number;
    amount: number;
    color: string;
}

// Profit Calculation Method
export type ProfitCalculationMethod = 'accrual' | 'cash';

// Store/Business Settings
export interface StoreSettings {
    name: string;
    address?: string;
    phone?: string;
    gstin?: string;
    ownerName: string;
    currency: string;
    businessType?: string;
    profitCalculationMethod: ProfitCalculationMethod;
}

// User Profile
export interface UserProfile {
    name: string;
    phone: string;
    email?: string;
    profileImage?: string;
    language: 'hi' | 'en';
}

// Storage Keys - Base keys (shared across modes)
export const SHARED_STORAGE_KEYS = {
    USER_PROFILE: '@suvidha/user_profile',
    FIRST_LAUNCH: '@suvidha/first_launch',
    APP_MODE: '@suvidha_app_mode',
} as const;

// Storage Keys - Mode-specific (data separated by mode)
// These are the base keys; actual keys will be prefixed with mode
const MODE_SPECIFIC_KEYS = {
    PRODUCTS: 'products',
    CATEGORIES: 'categories',
    CUSTOMERS: 'customers',
    SALES: 'sales',
    EXPENSES: 'expenses',
    EMPLOYEES: 'employees',
    STORE_SETTINGS: 'store_settings',
} as const;

// Function to get mode-prefixed storage keys
export function getStorageKeys(mode: 'shopkeeper' | 'personal' | null) {
    const prefix = mode ? `@suvidha/${mode}/` : '@suvidha/';
    return {
        PRODUCTS: `${prefix}${MODE_SPECIFIC_KEYS.PRODUCTS}`,
        CATEGORIES: `${prefix}${MODE_SPECIFIC_KEYS.CATEGORIES}`,
        CUSTOMERS: `${prefix}${MODE_SPECIFIC_KEYS.CUSTOMERS}`,
        SALES: `${prefix}${MODE_SPECIFIC_KEYS.SALES}`,
        EXPENSES: `${prefix}${MODE_SPECIFIC_KEYS.EXPENSES}`,
        EMPLOYEES: `${prefix}${MODE_SPECIFIC_KEYS.EMPLOYEES}`,
        STORE_SETTINGS: `${prefix}${MODE_SPECIFIC_KEYS.STORE_SETTINGS}`,
    };
}

// Legacy STORAGE_KEYS for backward compatibility (defaults to no mode prefix)
// Services should migrate to using getStorageKeys(mode) instead
export const STORAGE_KEYS = {
    PRODUCTS: '@suvidha/products',
    CATEGORIES: '@suvidha/categories',
    CUSTOMERS: '@suvidha/customers',
    SALES: '@suvidha/sales',
    EXPENSES: '@suvidha/expenses',
    EMPLOYEES: '@suvidha/employees',
    STORE_SETTINGS: '@suvidha/store_settings',
    USER_PROFILE: '@suvidha/user_profile',
    FIRST_LAUNCH: '@suvidha/first_launch',
    // Note: Transactions are derived from Sales + Expenses, not stored
} as const;

// Transaction Entity
export type TransactionType = 'SALE' | 'EXPENSE' | 'PRODUCT_ADD' | 'PAYMENT_RECEIVED' | 'PAYMENT_GIVEN';

export interface Transaction {
    id: string;
    type: TransactionType;
    amount: number;
    description: string;
    date: string;       // YYYY-MM-DD
    timestamp: number;  // For sorting
    referenceId?: string; // ID of the related sale/product/expense
}

// Default Categories
export const DEFAULT_CATEGORIES: Category[] = [
    { id: 'grocery', name: 'किराना' },
    { id: 'snacks', name: 'स्नैक्स' },
    { id: 'beverages', name: 'पेय' },
    { id: 'dairy', name: 'डेयरी' },
    { id: 'personal', name: 'पर्सनल केयर' },
    { id: 'household', name: 'घरेलू सामान' },
    { id: 'other', name: 'अन्य' },
];

// Expense Category Labels (Hindi)
export const EXPENSE_CATEGORY_LABELS: Record<ExpenseCategory, string> = {
    rent: 'किराया',
    electricity: 'बिजली',
    salary: 'वेतन',
    inventory: 'स्टॉक खरीद',
    transport: 'ट्रांसपोर्ट',
    maintenance: 'रखरखाव',
    stock_received: 'माल आया',
    stock_wastage: 'बर्बादी/नुकसान',
    other: 'अन्य',
};

