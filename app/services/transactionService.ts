// ============================================
// Transaction Service - Derived from Sales + Expenses
// ============================================
// Transactions are NOT stored separately. They are computed
// on-the-fly from Sales and Expenses tables.

import { Transaction, Sale, Expense } from '@/types';
import { getSales } from './saleService';
import { getExpenses } from './expenseService';
import { getCustomers } from './customerService';
import i18n from '@/utils/i18n';

/**
 * Get all transactions - DERIVED from Sales and Expenses
 * This replaces the old stored transactions approach
 */
export const getTransactions = async (): Promise<Transaction[]> => {
    try {
        const [sales, expenses, customers] = await Promise.all([
            getSales(),
            getExpenses(),
            getCustomers()
        ]);

        // Create a customer lookup map for getting names
        const customerMap = new Map(customers.map(c => [c.id, c.name]));

        const transactions: Transaction[] = [];

        // Derive transactions from Sales
        for (const sale of sales) {
            const customerName = sale.customerId
                ? (customerMap.get(sale.customerId) || i18n.t('transactions.unknownCustomer', { defaultValue: 'Unknown Customer' }))
                : i18n.t('transactions.walkInCustomer', { defaultValue: 'Walk-in Customer' });

            if (sale.saleType === 'PRODUCT_SALE') {
                transactions.push({
                    id: `sale_${sale.id}`,
                    type: 'SALE',
                    amount: sale.amountPaid,
                    description: `${i18n.t('transactions.sale')}: ${customerName}`,
                    date: sale.date,
                    timestamp: new Date(sale.createdAt).getTime(),
                    referenceId: sale.id
                });
            } else if (sale.saleType === 'BAKAYA_PAYMENT') {
                transactions.push({
                    id: `payment_${sale.id}`,
                    type: 'PAYMENT_RECEIVED',
                    amount: sale.amountPaid,
                    description: `${i18n.t('transactions.paymentReceived')}: ${customerName}`,
                    date: sale.date,
                    timestamp: new Date(sale.createdAt).getTime(),
                    referenceId: sale.id
                });
            } else {
                // Fallback for old sales without saleType (backward compatibility)
                transactions.push({
                    id: `sale_${sale.id}`,
                    type: 'SALE',
                    amount: sale.amountPaid,
                    description: `${i18n.t('transactions.sale')}: ${customerName}`,
                    date: sale.date,
                    timestamp: new Date(sale.createdAt).getTime(),
                    referenceId: sale.id
                });
            }
        }

        // Derive transactions from Expenses
        for (const expense of expenses) {
            const category = expense.category;
            let type: Transaction['type'];
            let description: string;

            // Determine transaction type and description based on expense category
            if (category === 'inventory') {
                type = 'PRODUCT_ADD';
                description = `${i18n.t('transactions.stockPurchase')}: ${expense.description}`;
            } else if (category === 'stock_received') {
                type = 'PRODUCT_ADD';
                description = `${i18n.t('transactions.stockReceived')}: ${expense.description}`;
            } else if (category === 'stock_wastage') {
                type = 'EXPENSE';
                description = `${i18n.t('transactions.wastage')}: ${expense.description}`;
            } else {
                type = 'EXPENSE';
                description = `${i18n.t('transactions.expense')}: ${expense.description}`;
            }

            transactions.push({
                id: `expense_${expense.id}`,
                type,
                amount: expense.amount,
                description,
                date: expense.date,
                timestamp: new Date(expense.createdAt).getTime(),
                referenceId: expense.id
            });
        }

        // Sort by timestamp descending (newest first)
        return transactions.sort((a, b) => b.timestamp - a.timestamp);
    } catch (e) {
        console.error('Error deriving transactions', e);
        return [];
    }
};

// Note: addTransaction and clearTransactions are removed
// Transactions are now derived, not stored
