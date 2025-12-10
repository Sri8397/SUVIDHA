// ============================================
// Sale Service - Transaction Operations
// ============================================

import { Sale, SaleItem, STORAGE_KEYS } from '@/types';
import { getData, setData, generateId, getTimestamp } from './storage';
import { updateProductStock } from './productService';
import { updateCustomerOutstanding } from './customerService';

/**
 * Get all sales
 */
export async function getSales(): Promise<Sale[]> {
    const sales = await getData<Sale[]>(STORAGE_KEYS.SALES);
    return sales || [];
}

/**
 * Get sale by ID
 */
export async function getSaleById(id: string): Promise<Sale | null> {
    const sales = await getSales();
    return sales.find(s => s.id === id) || null;
}

/**
 * Create new sale
 */
export async function createSale(
    sale: Omit<Sale, 'id' | 'createdAt'>
): Promise<Sale> {
    const sales = await getSales();

    const newSale: Sale = {
        ...sale,
        id: generateId(),
        createdAt: getTimestamp(),
    };

    // Update product stock for each item (only for product sales)
    if (sale.saleType !== 'BAKAYA_PAYMENT') {
        for (const item of sale.items) {
            await updateProductStock(item.productId, -item.quantity);
        }

        // Update customer outstanding if applicable (only for product sales)
        // BAKAYA_PAYMENT handles outstanding update separately in ReceivePaymentForm
        if (sale.customerId && sale.paymentStatus !== 'paid') {
            const outstandingAmount = sale.total - sale.amountPaid;
            await updateCustomerOutstanding(sale.customerId, outstandingAmount);
        }
    }

    sales.push(newSale);
    await setData(STORAGE_KEYS.SALES, sales);
    return newSale;
}

/**
 * Get sales for a specific date (YYYY-MM-DD format)
 */
export async function getSalesByDate(date: string): Promise<Sale[]> {
    const sales = await getSales();
    return sales.filter(s => s.date.startsWith(date));
}

/**
 * Get sales for today
 */
export async function getTodaySales(): Promise<Sale[]> {
    const today = new Date().toISOString().split('T')[0];
    return getSalesByDate(today);
}

/**
 * Get sales by customer
 */
export async function getSalesByCustomer(customerId: string): Promise<Sale[]> {
    const sales = await getSales();
    return sales.filter(s => s.customerId === customerId);
}

/**
 * Get sales in date range
 */
export async function getSalesInRange(
    startDate: string,
    endDate: string
): Promise<Sale[]> {
    const sales = await getSales();
    return sales.filter(s => s.date >= startDate && s.date <= endDate);
}

/**
 * Record payment for a sale
 */
export async function recordPayment(
    saleId: string,
    amount: number
): Promise<Sale | null> {
    const sales = await getSales();
    const index = sales.findIndex(s => s.id === saleId);
    if (index === -1) return null;

    const sale = sales[index];
    const newAmountPaid = sale.amountPaid + amount;
    const remaining = sale.total - newAmountPaid;

    let paymentStatus: Sale['paymentStatus'] = 'pending';
    if (remaining <= 0) {
        paymentStatus = 'paid';
    } else if (newAmountPaid > 0) {
        paymentStatus = 'partial';
    }

    sales[index] = {
        ...sale,
        amountPaid: newAmountPaid,
        paymentStatus,
    };

    // Update customer outstanding if customer exists
    if (sale.customerId) {
        await updateCustomerOutstanding(sale.customerId, -amount);
    }

    await setData(STORAGE_KEYS.SALES, sales);
    return sales[index];
}

/**
 * Calculate total sales amount for given sales
 * Only counts PRODUCT_SALE, excludes BAKAYA_PAYMENT to avoid double counting
 */
export function calculateTotalSales(sales: Sale[]): number {
    return sales
        .filter(s => s.saleType !== 'BAKAYA_PAYMENT')
        .reduce((sum, s) => sum + s.total, 0);
}

/**
 * Calculate total paid amount for given sales
 */
export function calculateTotalPaid(sales: Sale[]): number {
    return sales.reduce((sum, s) => sum + s.amountPaid, 0);
}

/**
 * Get weekly sales data (last 7 days)
 */
export async function getWeeklySalesData(): Promise<{ dates: string[]; amounts: number[] }> {
    const dates: string[] = [];
    const amounts: number[] = [];

    const today = new Date();

    for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dateStr = date.toISOString().split('T')[0];
        const daySales = await getSalesByDate(dateStr);

        dates.push(dateStr);
        amounts.push(calculateTotalSales(daySales));
    }

    return { dates, amounts };
}
