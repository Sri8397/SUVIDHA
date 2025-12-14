// ============================================
// Stats Service - Dashboard & Report Statistics
// ============================================

import { DashboardStats, WeeklyData, CategoryBreakdown, Sale, StoreSettings, STORAGE_KEYS, SHARED_STORAGE_KEYS, ProfitCalculationMethod } from '@/types';
import { getTodaySales, calculateTotalSales, getSales } from './saleService';
import { getTodayExpenses, calculateTotalExpenses } from './expenseService';
import { getTotalOutstanding } from './customerService';
import { getProducts } from './productService';
import { getData } from './storage';
import i18n from '@/i18n';

/**
 * Get the profit calculation method from settings
 */
async function getProfitCalculationMethod(): Promise<ProfitCalculationMethod> {
    const appMode = await getData<string>(SHARED_STORAGE_KEYS.APP_MODE);
    if (appMode === 'personal') {
        return 'cash';
    }

    const settings = await getData<StoreSettings>(STORAGE_KEYS.STORE_SETTINGS);
    // Default to 'accrual' for shopkeeper if not set
    return settings?.profitCalculationMethod || 'accrual';
}

/**
 * Calculate sales amount based on profit calculation method
 * - 'accrual': uses total sale amount
 * - 'cash': uses only amount paid
 */
function calculateSalesAmount(sales: Sale[], method: ProfitCalculationMethod): number {
    if (method === 'cash') {
        return sales.reduce((sum, s) => sum + s.amountPaid, 0);
    }
    return sales.reduce((sum, s) => sum + s.total, 0);
}

/**
 * Calculate profit as Sales - Expenses
 * Profit = Total Sales Amount - Total Expenses (Spend)
 */

/**
 * Get dashboard statistics for today
 */
export async function getDashboardStats(): Promise<DashboardStats> {
    const todaySales = await getTodaySales();
    const todayExpenses = await getTodayExpenses();
    const pendingPayments = await getTotalOutstanding();
    const profitMethod = await getProfitCalculationMethod();

    // For accrual: filter out BAKAYA_PAYMENT (already counted in original sale total)
    // For cash: include BAKAYA_PAYMENT (it's actual money received)
    const relevantSales = profitMethod === 'cash'
        ? todaySales  // Include all sales including bakaya payments
        : todaySales.filter(s => s.saleType !== 'BAKAYA_PAYMENT');

    // Calculate Loan Given (Outflow) - specific to Personal Mode
    // Defined as BAKAYA_PAYMENT with 0 amountPaid (we gave money, so we recorded the transaction but 0 was "paid back" to us)
    const loanGiven = todaySales
        .filter(s => s.saleType === 'BAKAYA_PAYMENT' && s.amountPaid === 0)
        .reduce((sum, s) => sum + s.total, 0);

    // Use appropriate calculation based on setting
    const salesAmount = profitMethod === 'cash'
        ? relevantSales.reduce((sum, s) => sum + s.amountPaid, 0)
        : calculateTotalSales(relevantSales);
    const expensesAmount = calculateTotalExpenses(todayExpenses);

    // Profit = Sales - Expenses (Spend) - Loan Given
    const profit = salesAmount - expensesAmount - loanGiven;

    return {
        todaySales: salesAmount,
        pendingPayments,
        profit: profit,
        expenses: expensesAmount,
    };
}

/**
 * Get weekly sales and profit data for charts
 */
export async function getWeeklyChartData(): Promise<WeeklyData> {
    return getChartDataByPeriod('weekly');
}

/**
 * Get chart data for different time periods
 * Profit = Gross Profit (sales margin) - Expenses
 */
export async function getChartDataByPeriod(period: 'daily' | 'weekly' | 'monthly'): Promise<WeeklyData> {
    const allSales = await getSales();
    const { getExpenses } = await import('./expenseService');
    const allExpenses = await getExpenses();
    const profitMethod = await getProfitCalculationMethod();
    const today = new Date();

    let labels: string[] = [];
    let salesAmounts: number[] = [];
    let profits: number[] = [];

    if (period === 'daily') {
        // Last 7 days (Sun, Mon, Tue, ... Sat)
        const dayLabels = [i18n.t('charts.sun'), i18n.t('charts.mon'), i18n.t('charts.tue'), i18n.t('charts.wed'), i18n.t('charts.thu'), i18n.t('charts.fri'), i18n.t('charts.sat')];

        for (let i = 6; i >= 0; i--) {
            const date = new Date(today);
            date.setDate(date.getDate() - i);
            const dateStr = date.toISOString().split('T')[0];

            const dayOfWeek = date.getDay(); // 0 = Sunday, 1 = Monday, etc.
            labels.push(dayLabels[dayOfWeek]);

            // For accrual: exclude BAKAYA_PAYMENT (already counted in original sale total)
            // For cash: include BAKAYA_PAYMENT (actual money received)
            const salesOnDay = profitMethod === 'cash'
                ? allSales.filter(s => s.date === dateStr)
                : allSales.filter(s => s.date === dateStr && s.saleType !== 'BAKAYA_PAYMENT');
            const daySales = profitMethod === 'cash'
                ? salesOnDay.reduce((sum, s) => sum + s.amountPaid, 0)
                : salesOnDay.reduce((sum, s) => sum + s.total, 0);
            salesAmounts.push(daySales);

            // Subtract expenses for this day
            const expensesOnDay = allExpenses.filter(e => e.date === dateStr);
            const dayExpenses = expensesOnDay.reduce((sum, e) => sum + e.amount, 0);

            // Calculate Loan Given for this day
            const loanGivenOnDay = salesOnDay
                .filter(s => s.saleType === 'BAKAYA_PAYMENT' && s.amountPaid === 0)
                .reduce((sum, s) => sum + s.total, 0);

            // Profit = Sales - Expenses (Spend) - Loan Given
            const profit = daySales - dayExpenses - loanGivenOnDay;
            profits.push(profit);
        }

    } else if (period === 'weekly') {
        // Last 4 weeks (Week 1, Week 2, Week 3, Week 4)
        const weekLabels = [
            i18n.t('charts.week1'),
            i18n.t('charts.week2'),
            i18n.t('charts.week3'),
            i18n.t('charts.week4')
        ];

        for (let week = 3; week >= 0; week--) {
            labels.push(weekLabels[3 - week]);

            let weekSales = 0;
            let weekExpenses = 0;
            let weekLoanGiven = 0;

            // Each week is 7 days
            for (let day = 0; day < 7; day++) {
                const date = new Date(today);
                date.setDate(date.getDate() - (week * 7 + day));
                const dateStr = date.toISOString().split('T')[0];

                // For accrual: exclude BAKAYA_PAYMENT (already counted in original sale total)
                // For cash: include BAKAYA_PAYMENT (actual money received)
                const salesOnDay = profitMethod === 'cash'
                    ? allSales.filter(s => s.date === dateStr)
                    : allSales.filter(s => s.date === dateStr && s.saleType !== 'BAKAYA_PAYMENT');
                weekSales += profitMethod === 'cash'
                    ? salesOnDay.reduce((sum, s) => sum + s.amountPaid, 0)
                    : salesOnDay.reduce((sum, s) => sum + s.total, 0);

                // Add expenses for this day
                const expensesOnDay = allExpenses.filter(e => e.date === dateStr);
                weekExpenses += expensesOnDay.reduce((sum, e) => sum + e.amount, 0);

                // Calculate Loan Given for this day
                const loanGivenOnDay = salesOnDay
                    .filter(s => s.saleType === 'BAKAYA_PAYMENT' && s.amountPaid === 0)
                    .reduce((sum, s) => sum + s.total, 0);

                weekLoanGiven += loanGivenOnDay;
            }

            salesAmounts.push(weekSales);
            // Profit = Sales - Expenses (Spend) - Loan Given
            profits.push(weekSales - weekExpenses - weekLoanGiven);
        }

    } else if (period === 'monthly') {
        // Last 12 months including current month
        const monthLabels = [
            i18n.t('charts.jan'), i18n.t('charts.feb'), i18n.t('charts.mar'), i18n.t('charts.apr'),
            i18n.t('charts.may'), i18n.t('charts.jun'), i18n.t('charts.jul'), i18n.t('charts.aug'),
            i18n.t('charts.sep'), i18n.t('charts.oct'), i18n.t('charts.nov'), i18n.t('charts.dec')
        ];

        for (let monthOffset = 11; monthOffset >= 0; monthOffset--) {
            const date = new Date(today);
            date.setMonth(date.getMonth() - monthOffset);
            const monthIndex = date.getMonth();
            const year = date.getFullYear();

            labels.push(monthLabels[monthIndex]);

            // Get all sales in this month
            const monthStart = new Date(year, monthIndex, 1).toISOString().split('T')[0];
            const monthEnd = new Date(year, monthIndex + 1, 0).toISOString().split('T')[0];

            // For accrual: exclude BAKAYA_PAYMENT (already counted in original sale total)
            // For cash: include BAKAYA_PAYMENT (actual money received)
            const salesInMonth = profitMethod === 'cash'
                ? allSales.filter(s => s.date >= monthStart && s.date <= monthEnd)
                : allSales.filter(s => s.date >= monthStart && s.date <= monthEnd && s.saleType !== 'BAKAYA_PAYMENT');
            const monthSales = profitMethod === 'cash'
                ? salesInMonth.reduce((sum, s) => sum + s.amountPaid, 0)
                : salesInMonth.reduce((sum, s) => sum + s.total, 0);
            salesAmounts.push(monthSales);

            // Subtract expenses for this month
            const expensesInMonth = allExpenses.filter(e => e.date >= monthStart && e.date <= monthEnd);
            const monthExpenses = expensesInMonth.reduce((sum, e) => sum + e.amount, 0);

            // Calculate Loan Given for this month
            const loanGivenInMonth = salesInMonth
                .filter(s => s.saleType === 'BAKAYA_PAYMENT' && s.amountPaid === 0)
                .reduce((sum, s) => sum + s.total, 0);

            // Profit = Sales - Expenses (Spend) - Loan Given
            profits.push(monthSales - monthExpenses - loanGivenInMonth);
        }
    }

    return {
        sales: salesAmounts,
        profit: profits,
        labels,
    };
}

/**
 * Get category breakdown for pie chart
 */
export async function getCategoryBreakdown(): Promise<CategoryBreakdown[]> {
    const products = await getProducts();

    // Count products by category
    const categoryCount: Record<string, number> = {};
    let total = 0;

    for (const product of products) {
        const cat = product.category || 'other';
        categoryCount[cat] = (categoryCount[cat] || 0) + 1;
        total++;
    }

    if (total === 0) {
        return [];
    }

    // Category colors
    const colors: Record<string, string> = {
        grocery: '#1976D2',
        snacks: '#2E7D32',
        beverages: '#FBC02D',
        dairy: '#F57C00',
        personal: '#7B1FA2',
        household: '#00ACC1',
        other: '#757575',
    };

    // Category labels
    const labels: Record<string, string> = {
        grocery: i18n.t('categories.grocery', 'किराना'),
        snacks: i18n.t('categories.snacks', 'स्नैक्स'),
        beverages: i18n.t('categories.beverages', 'पेय'),
        dairy: i18n.t('categories.dairy', 'डेयरी'),
        personal: i18n.t('categories.personal', 'पर्सनल केयर'),
        household: i18n.t('categories.household', 'घरेलू सामान'),
        other: i18n.t('categories.other', 'अन्य'),
    };

    const breakdown: CategoryBreakdown[] = Object.entries(categoryCount).map(([cat, count]) => ({
        name: cat,
        percentage: Math.round((count / total) * 100),
        amount: count,
        color: colors[cat] || '#757575',
    }));

    // Sort by percentage descending
    breakdown.sort((a, b) => b.percentage - a.percentage);

    return breakdown;
}

/**
 * Get monthly summary
 */
export async function getMonthlySummary(): Promise<{
    totalSales: number;
    totalExpenses: number;
    netProfit: number;
    salesCount: number;
}> {
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
        .toISOString()
        .split('T')[0];
    const endOfMonth = now.toISOString().split('T')[0];

    const { getSalesInRange } = await import('./saleService');
    const { getExpensesInRange } = await import('./expenseService');

    const sales = await getSalesInRange(startOfMonth, endOfMonth);
    const expenses = await getExpensesInRange(startOfMonth, endOfMonth);

    // Filter only product sales for counting and totals
    const productSales = sales.filter(s => s.saleType !== 'BAKAYA_PAYMENT');

    const totalSales = calculateTotalSales(sales);
    const totalExpenses = calculateTotalExpenses(expenses);

    return {
        totalSales,
        totalExpenses,
        netProfit: totalSales - totalExpenses,
        salesCount: productSales.length,
    };
}
