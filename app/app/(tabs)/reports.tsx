import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, ActivityIndicator } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { DualLineChart } from '@/components/ui/line-chart';
import { Header } from '@/components/ui/header';
import { useData } from '@/contexts/DataContext';
import { useAppMode } from '@/contexts/ModeContext';
import { WeeklyData } from '@/types';
import { Colors } from '@/constants/theme';
import { FAB } from '@/components/ui/fab';
import { AddSaleForm } from '@/components/forms/AddSaleForm';
import { AddExpenseForm } from '@/components/forms/AddExpenseForm';
import { AddProductForm } from '@/components/forms/AddProductForm';
import { AddCustomerForm } from '@/components/forms/AddCustomerForm';
import { AddIncomeForm } from '@/components/forms/AddIncomeForm';
import { AddLoanForm } from '@/components/forms/AddLoanForm';

// Time periods
const timePeriods = [
    { id: 'daily' },
    { id: 'weekly' },
    { id: 'monthly' },
] as const;

type PeriodType = 'daily' | 'weekly' | 'monthly';

export default function ReportsScreen() {
    const { t, i18n } = useTranslation();
    const { period } = useLocalSearchParams<{ period?: string }>();
    const router = useRouter();
    const { isLoading, dashboardStats, weeklyData, categoryBreakdown, getChartDataByPeriod } = useData();
    const { appMode } = useAppMode();
    const isPersonalMode = appMode === 'personal';
    const [selectedPeriod, setSelectedPeriod] = useState<PeriodType>('weekly');
    const [chartData, setChartData] = useState<WeeklyData>(weeklyData);
    const [isLoadingChart, setIsLoadingChart] = useState(false);

    // Form visibility states
    const [showSaleForm, setShowSaleForm] = useState(false);
    const [showExpenseForm, setShowExpenseForm] = useState(false);
    const [showProductForm, setShowProductForm] = useState(false);
    const [showCustomerForm, setShowCustomerForm] = useState(false);
    const [showIncomeForm, setShowIncomeForm] = useState(false);
    const [showLoanForm, setShowLoanForm] = useState(false);

    // Set initial period based on URL params
    useEffect(() => {
        if (period === 'daily' || period === 'weekly' || period === 'monthly') {
            setSelectedPeriod(period);
        }
    }, [period]);

    // Load chart data when period changes
    useEffect(() => {
        const loadChartData = async () => {
            setIsLoadingChart(true);
            try {
                const data = await getChartDataByPeriod(selectedPeriod);
                setChartData(data);
            } catch (error) {
                console.error('Error loading chart data:', error);
            } finally {
                setIsLoadingChart(false);
            }
        };
        loadChartData();
    }, [selectedPeriod, getChartDataByPeriod, i18n.language]);

    // Also update when weeklyData changes (initial load)
    useEffect(() => {
        if (selectedPeriod === 'weekly') {
            setChartData(weeklyData);
        }
    }, [weeklyData, selectedPeriod]);

    if (isLoading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color={Colors.light.primary} />
            </View>
        );
    }

    // Calculate totals from chart data
    const totalSales = chartData.sales.reduce((a: number, b: number) => a + b, 0);
    const totalProfit = chartData.profit.reduce((a: number, b: number) => a + b, 0);

    // Get period label for display - matches what's actually being summed

    const getPeriodLabel = () => {
        switch (selectedPeriod) {
            case 'daily': return t('reports.periodDays');      // Chart shows 7 days
            case 'weekly': return t('reports.periodWeeks');  // Chart shows 4 weeks
            case 'monthly': return t('reports.periodMonths');   // Chart shows 12 months
        }
    };

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />

            {/* Header */}
            <Header title={t('reports.title')} showLanguageToggle={true} />

            {/* Time Period Filters */}
            <View style={styles.filterWrapper}>
                <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    contentContainerStyle={styles.filtersContainer}
                    style={styles.filterScrollView}
                >
                    {timePeriods.map((period) => (
                        <TouchableOpacity
                            key={period.id}
                            onPress={() => setSelectedPeriod(period.id)}
                            style={[
                                styles.filterChip,
                                selectedPeriod === period.id && styles.filterChipActive
                            ]}
                        >
                            <Text style={[
                                styles.filterChipText,
                                selectedPeriod === period.id && styles.filterChipTextActive
                            ]}>

                                {t(`reports.${period.id}`)}
                            </Text>
                        </TouchableOpacity>
                    ))}
                </ScrollView>
            </View>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Summary Cards */}
                <View style={styles.summaryRow}>
                    <View style={styles.summaryCard}>
                        <Text style={styles.summaryLabel}>
                            {isPersonalMode ? t('reports.totalIncome') : t('reports.totalSales')} {getPeriodLabel()}
                        </Text>
                        <Text style={[styles.summaryValue, { color: Colors.light.primary }]}>₹ {totalSales.toLocaleString('en-IN')}</Text>
                    </View>
                    <View style={styles.summaryCard}>
                        <Text style={styles.summaryLabel}>
                            {isPersonalMode ? t('reports.totalSavings') : t('reports.totalProfit')} {getPeriodLabel()}
                        </Text>
                        <Text style={[styles.summaryValue, { color: Colors.light.secondary }]}>₹ {totalProfit.toLocaleString('en-IN')}</Text>
                    </View>
                </View>

                {/* Today's Summary */}
                <View style={styles.todayCard}>
                    <Text style={styles.todayTitle}>{t('reports.todaysSummary')}</Text>
                    <View style={styles.todayStats}>
                        <View style={styles.todayStat}>
                            <Text style={styles.todayStatLabel}>{isPersonalMode ? t('reports.income') : t('reports.sales')}</Text>
                            <Text style={[styles.todayStatValue, { color: Colors.light.primary }]}>₹{dashboardStats.todaySales.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={styles.todayDivider} />
                        <View style={styles.todayStat}>
                            <Text style={styles.todayStatLabel}>{t('reports.expenses')}</Text>
                            <Text style={[styles.todayStatValue, { color: Colors.light.error }]}>₹{dashboardStats.expenses.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={styles.todayDivider} />
                        <View style={styles.todayStat}>
                            <Text style={styles.todayStatLabel}>{isPersonalMode ? t('personal.savings') : t('reports.profit')}</Text>
                            <Text style={[styles.todayStatValue, { color: Colors.light.secondary }]}>₹{dashboardStats.profit.toLocaleString('en-IN')}</Text>
                        </View>
                    </View>
                    <TouchableOpacity
                        style={styles.transactionsButton}
                        onPress={() => router.push('/transactions')}
                    >
                        <FontAwesome name="list" size={14} color={Colors.light.primary} />
                        <Text style={styles.transactionsButtonText}>{t('reports.viewTransactions')}</Text>
                        <FontAwesome name="arrow-right" size={12} color={Colors.light.primary} />
                    </TouchableOpacity>
                </View>

                {/* Sales & Profit Chart */}
                <View style={styles.chartCard}>
                    <View style={styles.chartHeader}>
                        <Text style={styles.chartTitle}>{t(isPersonalMode ? 'reports.incomeAndSavings' : 'reports.salesAndProfit')} {getPeriodLabel()}</Text>
                        <View style={styles.legendRow}>
                            <View style={styles.legendItem}>
                                <View style={[styles.legendDot, { backgroundColor: Colors.light.primary }]} />
                                <Text style={styles.legendText}>{t(isPersonalMode ? 'reports.income' : 'reports.sales')}</Text>
                            </View>
                            <View style={styles.legendItem}>
                                <View style={[styles.legendDot, { backgroundColor: Colors.light.secondary }]} />
                                <Text style={styles.legendText}>{t(isPersonalMode ? 'personal.savings' : 'reports.profit')}</Text>
                            </View>
                        </View>
                    </View>

                    {/* Dual Line Chart */}
                    {isLoadingChart ? (
                        <View style={styles.chartLoadingContainer}>
                            <ActivityIndicator size="small" color={Colors.light.primary} />
                        </View>
                    ) : (
                        <DualLineChart
                            data1={chartData.sales}
                            data2={chartData.profit}
                            labels={chartData.labels}
                            color1={Colors.light.primary}
                            color2={Colors.light.secondary}
                            height={140}
                            isPersonalMode={isPersonalMode}
                        />
                    )}
                </View>

                {/* Category Breakdown */}
                {!isPersonalMode && (
                    <View style={styles.pieChartCard}>
                        <Text style={styles.chartTitle}>{t('reports.categoryReport')}</Text>

                        {categoryBreakdown.length === 0 ? (
                            <View style={styles.emptyCategory}>
                                <FontAwesome name="pie-chart" size={32} color={Colors.light.textTertiary} />
                                <Text style={styles.emptyCategoryText}>{t('reports.emptyCategory')}</Text>
                            </View>
                        ) : (
                            <>
                                <View style={styles.pieChartContainer}>
                                    {/* Circular Pie Chart with proportional segments */}
                                    <View style={styles.pieChart}>
                                        {(() => {
                                            let cumulativeAngle = 0;
                                            return categoryBreakdown.map((item, index) => {
                                                const percentage = item.percentage || 0;
                                                const angle = (percentage / 100) * 360;
                                                const startAngle = cumulativeAngle;
                                                cumulativeAngle += angle;

                                                // Create pie slice using rotation and clipping
                                                const sliceRotation = startAngle - 90; // Start from top

                                                if (percentage <= 0) return null;

                                                // For segments <= 180 degrees
                                                if (angle <= 180) {
                                                    return (
                                                        <View
                                                            key={item.name}
                                                            style={[
                                                                styles.pieSlice,
                                                                {
                                                                    backgroundColor: item.color,
                                                                    transform: [{ rotate: `${sliceRotation}deg` }],
                                                                }
                                                            ]}
                                                        >
                                                            <View
                                                                style={[
                                                                    styles.pieSliceInner,
                                                                    {
                                                                        backgroundColor: item.color,
                                                                        transform: [{ rotate: `${angle}deg` }],
                                                                    }
                                                                ]}
                                                            />
                                                        </View>
                                                    );
                                                } else {
                                                    // For segments > 180 degrees, we need two halves
                                                    return (
                                                        <React.Fragment key={item.name}>
                                                            <View
                                                                style={[
                                                                    styles.pieSlice,
                                                                    {
                                                                        backgroundColor: item.color,
                                                                        transform: [{ rotate: `${sliceRotation}deg` }],
                                                                    }
                                                                ]}
                                                            >
                                                                <View
                                                                    style={[
                                                                        styles.pieSliceInner,
                                                                        {
                                                                            backgroundColor: item.color,
                                                                            transform: [{ rotate: '180deg' }],
                                                                        }
                                                                    ]}
                                                                />
                                                            </View>
                                                            <View
                                                                style={[
                                                                    styles.pieSlice,
                                                                    {
                                                                        backgroundColor: item.color,
                                                                        transform: [{ rotate: `${sliceRotation + 180}deg` }],
                                                                    }
                                                                ]}
                                                            >
                                                                <View
                                                                    style={[
                                                                        styles.pieSliceInner,
                                                                        {
                                                                            backgroundColor: item.color,
                                                                            transform: [{ rotate: `${angle - 180}deg` }],
                                                                        }
                                                                    ]}
                                                                />
                                                            </View>
                                                        </React.Fragment>
                                                    );
                                                }
                                            });
                                        })()}
                                    </View>
                                </View>

                                <View style={styles.categoryLegend}>
                                    {categoryBreakdown.map((item, index) => (
                                        <View key={index} style={styles.categoryLegendItem}>
                                            <View style={[styles.categoryDot, { backgroundColor: item.color }]} />
                                            <Text style={styles.categoryName} numberOfLines={1}>{item.name}</Text>
                                            <Text style={styles.categoryPercent}>{Math.round(item.percentage)}%</Text>
                                        </View>
                                    ))}
                                </View>
                            </>
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
                onAddIncome={() => setShowIncomeForm(true)}
                onAddLoan={() => setShowLoanForm(true)}
            />

            {/* Forms */}
            <AddSaleForm visible={showSaleForm} onClose={() => setShowSaleForm(false)} />
            <AddExpenseForm visible={showExpenseForm} onClose={() => setShowExpenseForm(false)} />
            <AddProductForm visible={showProductForm} onClose={() => setShowProductForm(false)} />
            <AddCustomerForm visible={showCustomerForm} onClose={() => setShowCustomerForm(false)} />
            <AddIncomeForm visible={showIncomeForm} onClose={() => setShowIncomeForm(false)} />
            <AddLoanForm
                visible={showLoanForm}
                onClose={() => setShowLoanForm(false)}
                onAddPerson={() => {
                    setShowLoanForm(false);
                    setTimeout(() => setShowCustomerForm(true), 500);
                }}
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
        backgroundColor: Colors.light.background,
    },
    filterWrapper: {
        backgroundColor: Colors.light.white,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.border,
        paddingVertical: 12,
    },
    filtersContainer: {
        paddingHorizontal: 16,
        gap: 8,
    },
    filterScrollView: {
        maxHeight: 40,
    },
    filterChip: {
        paddingHorizontal: 16,
        paddingVertical: 6,
        borderRadius: 20,
        backgroundColor: Colors.light.background,
        marginRight: 8,
        borderWidth: 1,
        borderColor: Colors.light.border,
    },
    filterChipActive: {
        backgroundColor: Colors.light.primary,
        borderColor: Colors.light.primary,
    },
    filterChipText: {
        fontSize: 13,
        fontWeight: '500',
        color: Colors.light.text,
    },
    filterChipTextActive: {
        color: Colors.light.white,
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        padding: 16,
        paddingBottom: 40,
        gap: 16,
    },
    summaryRow: {
        flexDirection: 'row',
        gap: 16,
    },
    summaryCard: {
        flex: 1,
        backgroundColor: Colors.light.white,
        padding: 16,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        elevation: 2,
    },
    summaryLabel: {
        fontSize: 12,
        color: Colors.light.textSecondary,
        marginBottom: 8,
    },
    summaryValue: {
        fontSize: 16,
        fontWeight: '700',
        color: Colors.light.text,
    },
    todayCard: {
        backgroundColor: Colors.light.white,
        padding: 16,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        elevation: 2,
    },
    todayTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.text,
        marginBottom: 16,
    },
    todayStats: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 16,
    },
    todayStat: {
        flex: 1,
        alignItems: 'center',
    },
    todayDivider: {
        width: 1,
        height: 30,
        backgroundColor: Colors.light.border,
    },
    todayStatLabel: {
        fontSize: 12,
        color: Colors.light.textSecondary,
        marginBottom: 4,
    },
    todayStatValue: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.text,
    },
    transactionsButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 10,
        backgroundColor: Colors.light.background,
        borderRadius: 8,
        gap: 8,
    },
    transactionsButtonText: {
        fontSize: 14,
        fontWeight: '500',
        color: Colors.light.primary,
    },
    chartCard: {
        backgroundColor: Colors.light.white,
        padding: 16,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        elevation: 2,
        minHeight: 200,
    },
    chartHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 16,
    },
    chartTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.text,
    },
    legendRow: {
        flexDirection: 'row',
        gap: 12,
    },
    legendItem: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
    },
    legendDot: {
        width: 8,
        height: 8,
        borderRadius: 4,
    },
    legendText: {
        fontSize: 12,
        color: Colors.light.textSecondary,
    },
    pieChartCard: {
        backgroundColor: Colors.light.white,
        padding: 16,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
        elevation: 2,
        alignItems: 'center',
    },
    pieChartContainer: {
        marginTop: 16,
        width: 200,
        height: 200,
        alignItems: 'center',
        justifyContent: 'center',
    },
    pieChart: {
        width: 160,
        height: 160,
        borderRadius: 80,
        position: 'relative',
        overflow: 'hidden',
    },
    pieSlice: {
        position: 'absolute',
        width: '100%',
        height: '100%',
        borderRadius: 100, // half of width/height/slice
        justifyContent: 'center',
        alignItems: 'center',
        // Note: react-native overflow:hidden on View with border radius clips context
    },
    pieSliceInner: {
        position: 'absolute',
        top: 0,
        left: '50%',
        width: '50%',
        height: '100%',
        backgroundColor: Colors.light.info, // overwritten by dynamic
        transformOrigin: 'left center', // This is web syntax, RN needs different handling usually, but let's stick to user's approach if it was working or intended. 
        // Actually, CSS pie charts are hard in RN. Usually standard library like react-native-chart-kit is better.
        // But user provided this logic.
        // Correct way for RN manual pie chart is tricky.
    },
    emptyCategory: {
        padding: 32,
        alignItems: 'center',
        gap: 12,
    },
    emptyCategoryText: {
        color: Colors.light.textTertiary,
        fontSize: 14,
    },
    chartLoadingContainer: {
        height: 140,
        justifyContent: 'center',
        alignItems: 'center',
    },
    categoryLegend: {
        width: '100%',
        marginTop: 24,
        gap: 12,
    },
    categoryLegendItem: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    categoryDot: {
        width: 12,
        height: 12,
        borderRadius: 6,
        marginRight: 8,
    },
    categoryName: {
        flex: 1,
        fontSize: 14,
        color: Colors.light.text,
    },
    categoryPercent: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.text,
    }
});
