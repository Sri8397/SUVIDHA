// ============================================
// Expense Service - Expense Tracking
// ============================================

import { Expense, ExpenseCategory } from '@/types';
import { getData, setData, generateId, getTimestamp } from './storage';
import { getKeysForCurrentMode } from './modeStorage';

/**
 * Get all expenses
 */
export async function getExpenses(): Promise<Expense[]> {
    const keys = getKeysForCurrentMode();
    const expenses = await getData<Expense[]>(keys.EXPENSES);
    return expenses || [];
}

/**
 * Get expense by ID
 */
export async function getExpenseById(id: string): Promise<Expense | null> {
    const expenses = await getExpenses();
    return expenses.find(e => e.id === id) || null;
}

/**
 * Add new expense
 */
export async function addExpense(
    expense: Omit<Expense, 'id' | 'createdAt'>
): Promise<Expense> {
    const keys = getKeysForCurrentMode();
    const expenses = await getExpenses();
    const newExpense: Expense = {
        ...expense,
        id: generateId(),
        createdAt: getTimestamp(),
    };
    expenses.push(newExpense);
    await setData(keys.EXPENSES, expenses);
    return newExpense;
}

/**
 * Update expense
 */
export async function updateExpense(
    id: string,
    updates: Partial<Omit<Expense, 'id' | 'createdAt'>>
): Promise<Expense | null> {
    const keys = getKeysForCurrentMode();
    const expenses = await getExpenses();
    const index = expenses.findIndex(e => e.id === id);
    if (index === -1) return null;

    expenses[index] = {
        ...expenses[index],
        ...updates,
    };
    await setData(keys.EXPENSES, expenses);
    return expenses[index];
}

/**
 * Delete expense
 */
export async function deleteExpense(id: string): Promise<boolean> {
    const keys = getKeysForCurrentMode();
    const expenses = await getExpenses();
    const filtered = expenses.filter(e => e.id !== id);
    if (filtered.length === expenses.length) return false;
    await setData(keys.EXPENSES, filtered);
    return true;
}

/**
 * Get expenses for a specific date
 */
export async function getExpensesByDate(date: string): Promise<Expense[]> {
    const expenses = await getExpenses();
    return expenses.filter(e => e.date.startsWith(date));
}

/**
 * Get expenses for today
 */
export async function getTodayExpenses(): Promise<Expense[]> {
    const today = new Date().toISOString().split('T')[0];
    return getExpensesByDate(today);
}

/**
 * Get expenses in date range
 */
export async function getExpensesInRange(
    startDate: string,
    endDate: string
): Promise<Expense[]> {
    const expenses = await getExpenses();
    return expenses.filter(e => e.date >= startDate && e.date <= endDate);
}

/**
 * Get expenses by category
 */
export async function getExpensesByCategory(category: ExpenseCategory): Promise<Expense[]> {
    const expenses = await getExpenses();
    return expenses.filter(e => e.category === category);
}

/**
 * Calculate total expenses
 */
export function calculateTotalExpenses(expenses: Expense[]): number {
    return expenses.reduce((sum, e) => sum + e.amount, 0);
}

/**
 * Get expense breakdown by category
 */
export async function getExpenseBreakdown(): Promise<Record<ExpenseCategory, number>> {
    const expenses = await getExpenses();
    const breakdown: Record<ExpenseCategory, number> = {
        rent: 0,
        electricity: 0,
        salary: 0,
        inventory: 0,
        transport: 0,
        maintenance: 0,
        stock_received: 0,
        stock_wastage: 0,
        other: 0,
    };

    for (const expense of expenses) {
        breakdown[expense.category] += expense.amount;
    }

    return breakdown;
}

/**
 * Get weekly expenses data (last 7 days)
 */
export async function getWeeklyExpensesData(): Promise<{ dates: string[]; amounts: number[] }> {
    const dates: string[] = [];
    const amounts: number[] = [];

    const today = new Date();

    for (let i = 6; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dateStr = date.toISOString().split('T')[0];
        const dayExpenses = await getExpensesByDate(dateStr);

        dates.push(dateStr);
        amounts.push(calculateTotalExpenses(dayExpenses));
    }

    return { dates, amounts };
}
