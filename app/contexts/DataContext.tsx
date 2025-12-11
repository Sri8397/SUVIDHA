// ============================================
// Data Context - Global App State Management
// ============================================

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { Product, Customer, Sale, Expense, Category, DashboardStats, WeeklyData, CategoryBreakdown, Transaction } from '@/types';
import * as productService from '@/services/productService';
import * as customerService from '@/services/customerService';
import * as saleService from '@/services/saleService';
import * as expenseService from '@/services/expenseService';
import * as statsService from '@/services/statsService';
import * as transactionService from '@/services/transactionService';

// Context Types
interface DataContextType {
    // Loading state
    isLoading: boolean;

    // Transactions & Notifications
    transactions: Transaction[];
    refreshTransactions: () => Promise<void>;
    dismissedNotificationIds: Set<string>;
    dismissNotification: (id: string) => void;
    dismissAllNotifications: () => void;
    getVisibleNotifications: () => Transaction[];

    // Products
    products: Product[];
    categories: Category[];
    lowStockProducts: Product[];
    addProduct: (product: Omit<Product, 'id' | 'createdAt' | 'updatedAt'>) => Promise<Product>;
    updateProduct: (id: string, updates: Partial<Product>) => Promise<Product | null>;
    deleteProduct: (id: string) => Promise<boolean>;
    refreshProducts: () => Promise<void>;

    // Customers
    customers: Customer[];
    addCustomer: (customer: Omit<Customer, 'id' | 'outstanding' | 'createdAt' | 'updatedAt'>) => Promise<Customer>;
    updateCustomer: (id: string, updates: Partial<Customer>) => Promise<Customer | null>;
    deleteCustomer: (id: string) => Promise<boolean>;
    refreshCustomers: () => Promise<void>;

    // Sales
    sales: Sale[];
    todaySales: Sale[];
    createSale: (sale: Omit<Sale, 'id' | 'createdAt'>) => Promise<Sale>;
    recordPayment: (saleId: string, amount: number) => Promise<Sale | null>;
    refreshSales: () => Promise<void>;
    // Expenses
    expenses: Expense[];
    addExpense: (expense: Omit<Expense, 'id' | 'createdAt'>) => Promise<Expense>;
    updateExpense: (id: string, updates: Partial<Expense>) => Promise<Expense | null>;
    deleteExpense: (id: string) => Promise<boolean>;
    refreshExpenses: () => Promise<void>;

    // Dashboard Stats
    dashboardStats: DashboardStats;
    weeklyData: WeeklyData;
    categoryBreakdown: CategoryBreakdown[];
    refreshStats: () => Promise<void>;
    getChartDataByPeriod: (period: 'daily' | 'weekly' | 'monthly') => Promise<WeeklyData>;

    // Refresh all data
    refreshAll: () => Promise<void>;
}

const defaultStats: DashboardStats = {
    todaySales: 0,
    pendingPayments: 0,
    profit: 0,
    expenses: 0,
};

const defaultWeeklyData: WeeklyData = {
    sales: [0, 0, 0, 0, 0, 0, 0],
    profit: [0, 0, 0, 0, 0, 0, 0],
    labels: ['सोम', 'मंग', 'बुध', 'गुरु', 'शुक्र', 'शनि', 'रवि'],
};

// Create Context
const DataContext = createContext<DataContextType | undefined>(undefined);

// Provider Component
export function DataProvider({ children }: { children: ReactNode }) {
    const [isLoading, setIsLoading] = useState(true);

    // Data state
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [dismissedNotificationIds, setDismissedNotificationIds] = useState<Set<string>>(new Set());
    const [products, setProducts] = useState<Product[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [lowStockProducts, setLowStockProducts] = useState<Product[]>([]);
    const [customers, setCustomers] = useState<Customer[]>([]);
    const [sales, setSales] = useState<Sale[]>([]);
    const [todaySales, setTodaySales] = useState<Sale[]>([]);
    const [expenses, setExpenses] = useState<Expense[]>([]);
    const [dashboardStats, setDashboardStats] = useState<DashboardStats>(defaultStats);
    const [weeklyData, setWeeklyData] = useState<WeeklyData>(defaultWeeklyData);
    const [categoryBreakdown, setCategoryBreakdown] = useState<CategoryBreakdown[]>([]);

    // Refresh functions
    const refreshTransactions = useCallback(async () => {
        const trans = await transactionService.getTransactions();
        setTransactions(trans);
    }, []);

    // Notification dismiss functions
    const dismissNotification = useCallback((id: string) => {
        setDismissedNotificationIds(prev => new Set([...prev, id]));
    }, []);

    const dismissAllNotifications = useCallback(() => {
        const allIds = transactions.map(t => t.id);
        setDismissedNotificationIds(new Set(allIds));
    }, [transactions]);

    const getVisibleNotifications = useCallback(() => {
        return transactions.filter(t => !dismissedNotificationIds.has(t.id)).slice(0, 20);
    }, [transactions, dismissedNotificationIds]);

    const refreshProducts = useCallback(async () => {
        const [prods, cats, lowStock] = await Promise.all([
            productService.getProducts(),
            productService.getCategories(),
            productService.getLowStockProducts(),
        ]);
        setProducts(prods);
        setCategories(cats);
        setLowStockProducts(lowStock);
    }, []);

    const refreshCustomers = useCallback(async () => {
        const custs = await customerService.getCustomers();
        setCustomers(custs);
    }, []);

    const refreshSales = useCallback(async () => {
        const [allSales, today] = await Promise.all([
            saleService.getSales(),
            saleService.getTodaySales(),
        ]);
        setSales(allSales);
        setTodaySales(today);
    }, []);

    const refreshExpenses = useCallback(async () => {
        const exps = await expenseService.getExpenses();
        setExpenses(exps);
    }, []);

    const refreshStats = useCallback(async () => {
        const [stats, weekly, breakdown] = await Promise.all([
            statsService.getDashboardStats(),
            statsService.getWeeklyChartData(),
            statsService.getCategoryBreakdown(),
        ]);
        setDashboardStats(stats);
        setWeeklyData(weekly);
        setCategoryBreakdown(breakdown);
    }, []);

    const refreshAll = useCallback(async () => {
        setIsLoading(true);
        try {
            await Promise.all([
                refreshTransactions(),
                refreshProducts(),
                refreshCustomers(),
                refreshSales(),
                refreshExpenses(),
                refreshStats(),
            ]);
        } finally {
            setIsLoading(false);
        }
    }, [refreshTransactions, refreshProducts, refreshCustomers, refreshSales, refreshExpenses, refreshStats]);

    // Initial load
    useEffect(() => {
        refreshAll();
    }, []);

    // Note: Transactions are now derived from Sales + Expenses, not stored separately
    // The transactionService.getTransactions() computes them on-the-fly

    // Product operations
    const addProduct = useCallback(async (product: Omit<Product, 'id' | 'createdAt' | 'updatedAt'>) => {
        const newProduct = await productService.addProduct(product);
        const today = new Date().toISOString().split('T')[0];

        // Auto-create expense for inventory purchase if stock > 0 and costPrice > 0
        // This expense will automatically appear in derived transactions
        if (product.stock > 0 && product.costPrice > 0) {
            const inventoryExpense = product.costPrice * product.stock;
            await expenseService.addExpense({
                category: 'inventory',
                amount: inventoryExpense,
                description: `${product.name} (${product.stock} × ₹${product.costPrice})`,
                date: today,
            });
            await refreshExpenses();
        }

        await Promise.all([refreshProducts(), refreshStats(), refreshTransactions()]);
        return newProduct;
    }, [refreshProducts, refreshExpenses, refreshStats, refreshTransactions]);

    const updateProduct = useCallback(async (id: string, updates: Partial<Product>) => {
        const updated = await productService.updateProduct(id, updates);
        if (updated) await refreshProducts();
        return updated;
    }, [refreshProducts]);

    const deleteProduct = useCallback(async (id: string) => {
        const success = await productService.deleteProduct(id);
        if (success) await refreshProducts();
        return success;
    }, [refreshProducts]);

    // Customer operations
    const addCustomer = useCallback(async (customer: Omit<Customer, 'id' | 'outstanding' | 'createdAt' | 'updatedAt'>) => {
        const newCustomer = await customerService.addCustomer(customer);
        await refreshCustomers();
        return newCustomer;
    }, [refreshCustomers]);

    const updateCustomer = useCallback(async (id: string, updates: Partial<Customer>) => {
        const updated = await customerService.updateCustomer(id, updates);
        if (updated) {
            await refreshCustomers();
            // Also refresh stats if outstanding changed (affects pendingPayments on dashboard)
            if ('outstanding' in updates) {
                await refreshStats();
            }
        }
        return updated;
    }, [refreshCustomers, refreshStats]);

    const deleteCustomer = useCallback(async (id: string) => {
        const success = await customerService.deleteCustomer(id);
        if (success) await refreshCustomers();
        return success;
    }, [refreshCustomers]);

    // Sale operations
    const createSale = useCallback(async (sale: Omit<Sale, 'id' | 'createdAt'>) => {
        const newSale = await saleService.createSale(sale);
        // Transaction is derived automatically from the sale
        await Promise.all([refreshSales(), refreshProducts(), refreshCustomers(), refreshStats(), refreshTransactions()]);
        return newSale;
    }, [refreshSales, refreshProducts, refreshCustomers, refreshStats, refreshTransactions]);

    const recordPayment = useCallback(async (saleId: string, amount: number) => {
        // This is for recording payment against a specific sale
        const updated = await saleService.recordPayment(saleId, amount);
        if (updated) {
            await Promise.all([refreshSales(), refreshCustomers(), refreshStats(), refreshTransactions()]);
        }
        return updated;
    }, [refreshSales, refreshCustomers, refreshStats, refreshTransactions]);

    // Expense operations
    const addExpense = useCallback(async (expense: Omit<Expense, 'id' | 'createdAt'>) => {
        const newExpense = await expenseService.addExpense(expense);
        // Transaction is derived automatically from the expense
        await Promise.all([refreshExpenses(), refreshStats(), refreshTransactions()]);
        return newExpense;
    }, [refreshExpenses, refreshStats, refreshTransactions]);

    const updateExpense = useCallback(async (id: string, updates: Partial<Expense>) => {
        const updated = await expenseService.updateExpense(id, updates);
        if (updated) await Promise.all([refreshExpenses(), refreshStats()]);
        return updated;
    }, [refreshExpenses, refreshStats]);

    const deleteExpense = useCallback(async (id: string) => {
        const success = await expenseService.deleteExpense(id);
        if (success) await Promise.all([refreshExpenses(), refreshStats()]);
        return success;
    }, [refreshExpenses, refreshStats]);

    // Get chart data by period
    const getChartDataByPeriod = useCallback(async (period: 'daily' | 'weekly' | 'monthly') => {
        return statsService.getChartDataByPeriod(period);
    }, []);

    const value: DataContextType = {
        isLoading,
        transactions,
        refreshTransactions,
        dismissedNotificationIds,
        dismissNotification,
        dismissAllNotifications,
        getVisibleNotifications,
        products,
        categories,
        lowStockProducts,
        addProduct,
        updateProduct,
        deleteProduct,
        refreshProducts,
        customers,
        addCustomer,
        updateCustomer,
        deleteCustomer,
        refreshCustomers,
        sales,
        todaySales,
        createSale,
        recordPayment,
        refreshSales,
        expenses,
        addExpense,
        updateExpense,
        deleteExpense,
        refreshExpenses,
        dashboardStats,
        weeklyData,
        categoryBreakdown,
        refreshStats,
        getChartDataByPeriod,
        refreshAll,
    };

    return (
        <DataContext.Provider value={value}>
            {children}
        </DataContext.Provider>
    );
}

// Hook for using data context
export function useData() {
    const context = useContext(DataContext);
    if (context === undefined) {
        throw new Error('useData must be used within a DataProvider');
    }
    return context;
}
