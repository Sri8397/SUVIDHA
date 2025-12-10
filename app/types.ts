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

// Sale/Transaction Entity
export interface Sale {
    id: string;
    customerId?: string; // Optional - for walk-in customers
    customerName: string;
    items: SaleItem[];
    subtotal: number;
    discount: number;
    total: number;
    amountPaid: number;
    paymentStatus: 'paid' | 'partial' | 'pending';
    paymentMethod: 'cash' | 'upi' | 'card' | 'credit';
    date: string;
    createdAt: string;
}

// Expense Category
export type ExpenseCategory =
    | 'rent'
    | 'electricity'
    | 'salary'
    | 'inventory'
    | 'transport'
    | 'maintenance'
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

// Store/Business Settings
export interface StoreSettings {
    name: string;
    address?: string;
    phone?: string;
    gstin?: string;
    ownerName: string;
    currency: string;
}

// User Profile
export interface UserProfile {
    name: string;
    phone: string;
    email?: string;
    language: 'hi' | 'en';
}

// Storage Keys
export const STORAGE_KEYS = {
    PRODUCTS: '@suvidha/products',
    CATEGORIES: '@suvidha/categories',
    CUSTOMERS: '@suvidha/customers',
    SALES: '@suvidha/sales',
    EXPENSES: '@suvidha/expenses',
    STORE_SETTINGS: '@suvidha/store_settings',
    USER_PROFILE: '@suvidha/user_profile',
    FIRST_LAUNCH: '@suvidha/first_launch',
} as const;

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
    other: 'अन्य',
};

