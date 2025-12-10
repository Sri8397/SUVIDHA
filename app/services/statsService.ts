// ============================================
// Stats Service - Dashboard & Report Statistics
// ============================================

import { DashboardStats, WeeklyData, CategoryBreakdown, Sale } from '@/types';
import { getTodaySales, calculateTotalSales, getSales } from './saleService';
import { getTodayExpenses, calculateTotalExpenses } from './expenseService';
import { getTotalOutstanding } from './customerService';
import { getProducts } from './productService';

/**
 * Calculate profit from sales using cost prices
 */
function calculateProfitFromSales(sales: Sale[]): number {
    let totalProfit = 0;
    for (const sale of sales) {
        for (const item of sale.items) {
            // Profit = (Selling Price - Cost Price) × Quantity
            const itemProfit = (item.unitPrice - (item.costPrice || 0)) * item.quantity;
            totalProfit += itemProfit;
        }
    }
    return totalProfit;
}

/**
 * Get dashboard statistics for today
 */
export async function getDashboardStats(): Promise<DashboardStats> {
    const todaySales = await getTodaySales();
    const todayExpenses = await getTodayExpenses();
    const pendingPayments = await getTotalOutstanding();

    const salesAmount = calculateTotalSales(todaySales);
    const expensesAmount = calculateTotalExpenses(todayExpenses);

    // Calculate actual profit from cost prices
    const grossProfit = calculateProfitFromSales(todaySales);
    // Net profit after expenses
    const profit = grossProfit - expensesAmount;

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
    const today = new Date();

    let labels: string[] = [];
    let salesAmounts: number[] = [];
    let profits: number[] = [];

    if (period === 'daily') {
        // Last 7 days (Sun, Mon, Tue, ... Sat)
        const dayLabels = ['रवि', 'सोम', 'मंग', 'बुध', 'गुरु', 'शुक्र', 'शनि'];

        for (let i = 6; i >= 0; i--) {
            const date = new Date(today);
            date.setDate(date.getDate() - i);
            const dateStr = date.toISOString().split('T')[0];

            const dayOfWeek = date.getDay(); // 0 = Sunday, 1 = Monday, etc.
            labels.push(dayLabels[dayOfWeek]);

            // Filter only product sales, exclude bakaya payments
            const salesOnDay = allSales.filter(s => s.date === dateStr && s.saleType !== 'BAKAYA_PAYMENT');
            const daySales = salesOnDay.reduce((sum, s) => sum + s.total, 0);
            salesAmounts.push(daySales);

            // Calculate gross profit from sales
            let grossProfit = 0;
            for (const sale of salesOnDay) {
                for (const item of sale.items) {
                    grossProfit += (item.unitPrice - (item.costPrice || 0)) * item.quantity;
                }
            }

            // Subtract expenses for this day
            const expensesOnDay = allExpenses.filter(e => e.date === dateStr);
            const dayExpenses = expensesOnDay.reduce((sum, e) => sum + e.amount, 0);

            // Net profit = Gross Profit - Expenses
            const netProfit = grossProfit - dayExpenses;
            profits.push(netProfit);
        }

    } else if (period === 'weekly') {
        // Last 4 weeks (Week 1, Week 2, Week 3, Week 4)
        const weekLabels = ['सप्ताह 1', 'सप्ताह 2', 'सप्ताह 3', 'सप्ताह 4'];

        for (let week = 3; week >= 0; week--) {
            labels.push(weekLabels[3 - week]);

            let weekSales = 0;
            let weekGrossProfit = 0;
            let weekExpenses = 0;

            // Each week is 7 days
            for (let day = 0; day < 7; day++) {
                const date = new Date(today);
                date.setDate(date.getDate() - (week * 7 + day));
                const dateStr = date.toISOString().split('T')[0];

                // Filter only product sales, exclude bakaya payments
                const salesOnDay = allSales.filter(s => s.date === dateStr && s.saleType !== 'BAKAYA_PAYMENT');
                weekSales += salesOnDay.reduce((sum, s) => sum + s.total, 0);

                for (const sale of salesOnDay) {
                    for (const item of sale.items) {
                        weekGrossProfit += (item.unitPrice - (item.costPrice || 0)) * item.quantity;
                    }
                }

                // Add expenses for this day
                const expensesOnDay = allExpenses.filter(e => e.date === dateStr);
                weekExpenses += expensesOnDay.reduce((sum, e) => sum + e.amount, 0);
            }

            salesAmounts.push(weekSales);
            // Net profit = Gross Profit - Expenses
            profits.push(weekGrossProfit - weekExpenses);
        }

    } else if (period === 'monthly') {
        // Last 12 months including current month
        const monthLabels = ['जन', 'फर', 'मार्च', 'अप्रै', 'मई', 'जून', 'जुला', 'अग', 'सित', 'अक्टू', 'नव', 'दिस'];

        for (let monthOffset = 11; monthOffset >= 0; monthOffset--) {
            const date = new Date(today);
            date.setMonth(date.getMonth() - monthOffset);
            const monthIndex = date.getMonth();
            const year = date.getFullYear();

            labels.push(monthLabels[monthIndex]);

            // Get all sales in this month
            const monthStart = new Date(year, monthIndex, 1).toISOString().split('T')[0];
            const monthEnd = new Date(year, monthIndex + 1, 0).toISOString().split('T')[0];

            // Get all product sales in this month (exclude bakaya payments)
            const salesInMonth = allSales.filter(s => s.date >= monthStart && s.date <= monthEnd && s.saleType !== 'BAKAYA_PAYMENT');
            const monthSales = salesInMonth.reduce((sum, s) => sum + s.total, 0);
            salesAmounts.push(monthSales);

            // Calculate gross profit
            let monthGrossProfit = 0;
            for (const sale of salesInMonth) {
                for (const item of sale.items) {
                    monthGrossProfit += (item.unitPrice - (item.costPrice || 0)) * item.quantity;
                }
            }

            // Subtract expenses for this month
            const expensesInMonth = allExpenses.filter(e => e.date >= monthStart && e.date <= monthEnd);
            const monthExpenses = expensesInMonth.reduce((sum, e) => sum + e.amount, 0);

            // Net profit = Gross Profit - Expenses
            profits.push(monthGrossProfit - monthExpenses);
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

    // Category labels in Hindi
    const labels: Record<string, string> = {
        grocery: 'किराना',
        snacks: 'स्नैक्स',
        beverages: 'पेय',
        dairy: 'डेयरी',
        personal: 'पर्सनल केयर',
        household: 'घरेलू सामान',
        other: 'अन्य',
    };

    const breakdown: CategoryBreakdown[] = Object.entries(categoryCount).map(([cat, count]) => ({
        name: labels[cat] || cat,
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
