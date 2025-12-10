import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, TextInput, StyleSheet, ActivityIndicator } from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { DualLineChart } from '@/components/ui/line-chart';
import { Header } from '@/components/ui/header';
import { FAB } from '@/components/ui/fab';
import { useData } from '@/contexts/DataContext';
import { AddSaleForm } from '@/components/forms/AddSaleForm';
import { AddExpenseForm } from '@/components/forms/AddExpenseForm';
import { AddProductForm } from '@/components/forms/AddProductForm';
import { AddCustomerForm } from '@/components/forms/AddCustomerForm';
import { WeeklyData } from '@/types';

// Time periods
const timePeriods = [
    { id: 'daily', label: 'दैनिक' },
    { id: 'weekly', label: 'साप्ताहिक' },
    { id: 'monthly', label: 'मासिक' },
] as const;

type PeriodType = 'daily' | 'weekly' | 'monthly';

export default function ReportsScreen() {
    const { isLoading, dashboardStats, weeklyData, categoryBreakdown, getChartDataByPeriod } = useData();
    const [selectedPeriod, setSelectedPeriod] = useState<PeriodType>('weekly');
    const [searchQuery, setSearchQuery] = useState('');
    const [chartData, setChartData] = useState<WeeklyData>(weeklyData);
    const [isLoadingChart, setIsLoadingChart] = useState(false);

    // Form states
    const [showSaleForm, setShowSaleForm] = useState(false);
    const [showExpenseForm, setShowExpenseForm] = useState(false);
    const [showProductForm, setShowProductForm] = useState(false);
    const [showCustomerForm, setShowCustomerForm] = useState(false);

    // Category colors for pie chart
    const categoryColors = ['#1976D2', '#2E7D32', '#FBC02D', '#F57C00', '#7B1FA2', '#00ACC1'];

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
    }, [selectedPeriod, getChartDataByPeriod]);

    // Also update when weeklyData changes (initial load)
    useEffect(() => {
        if (selectedPeriod === 'weekly') {
            setChartData(weeklyData);
        }
    }, [weeklyData, selectedPeriod]);

    if (isLoading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#2E7D32" />
            </View>
        );
    }

    // Calculate totals from chart data
    const totalSales = chartData.sales.reduce((a: number, b: number) => a + b, 0);
    const totalProfit = chartData.profit.reduce((a: number, b: number) => a + b, 0);

    // Get period label for display - matches what's actually being summed
    const getPeriodLabel = () => {
        switch (selectedPeriod) {
            case 'daily': return '(7 दिन)';      // Chart shows 7 days
            case 'weekly': return '(4 सप्ताह)';  // Chart shows 4 weeks
            case 'monthly': return '(12 माह)';   // Chart shows 12 months
        }
    };

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            {/* Header */}
            <Header title="रिपोर्ट" showLanguageToggle={true} />

            {/* Search Bar */}
            <View style={styles.searchContainer}>
                <FontAwesome name="search" size={18} color="#9E9E9E" />
                <TextInput
                    style={styles.searchInput}
                    placeholder="खोजें..."
                    placeholderTextColor="#9E9E9E"
                    value={searchQuery}
                    onChangeText={setSearchQuery}
                />
            </View>

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
                                {period.label}
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
                        <Text style={styles.summaryLabel}>कुल बिक्री {getPeriodLabel()}</Text>
                        <Text style={styles.summaryValue}>₹ {totalSales.toLocaleString('en-IN')}</Text>
                    </View>
                    <View style={styles.summaryCard}>
                        <Text style={styles.summaryLabel}>कुल लाभ {getPeriodLabel()}</Text>
                        <Text style={[styles.summaryValue, { color: totalProfit >= 0 ? '#2E7D32' : '#D32F2F' }]}>₹ {totalProfit.toLocaleString('en-IN')}</Text>
                    </View>
                </View>

                {/* Today's Summary */}
                <View style={styles.todayCard}>
                    <Text style={styles.todayTitle}>आज का सारांश</Text>
                    <View style={styles.todayStats}>
                        <View style={styles.todayStat}>
                            <Text style={styles.todayStatLabel}>बिक्री</Text>
                            <Text style={styles.todayStatValue}>₹{dashboardStats.todaySales.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={styles.todayDivider} />
                        <View style={styles.todayStat}>
                            <Text style={styles.todayStatLabel}>खर्च</Text>
                            <Text style={[styles.todayStatValue, { color: '#F57C00' }]}>₹{dashboardStats.expenses.toLocaleString('en-IN')}</Text>
                        </View>
                        <View style={styles.todayDivider} />
                        <View style={styles.todayStat}>
                            <Text style={styles.todayStatLabel}>लाभ</Text>
                            <Text style={[styles.todayStatValue, { color: dashboardStats.profit >= 0 ? '#2E7D32' : '#D32F2F' }]}>₹{dashboardStats.profit.toLocaleString('en-IN')}</Text>
                        </View>
                    </View>
                </View>

                {/* Sales & Profit Chart */}
                <View style={styles.chartCard}>
                    <View style={styles.chartHeader}>
                        <Text style={styles.chartTitle}>बिक्री और लाभ {getPeriodLabel()}</Text>
                        <View style={styles.legendRow}>
                            <View style={styles.legendItem}>
                                <View style={[styles.legendDot, { backgroundColor: '#2E7D32' }]} />
                                <Text style={styles.legendText}>बिक्री</Text>
                            </View>
                            <View style={styles.legendItem}>
                                <View style={[styles.legendDot, { backgroundColor: '#F57C00' }]} />
                                <Text style={styles.legendText}>लाभ</Text>
                            </View>
                        </View>
                    </View>

                    {/* Dual Line Chart */}
                    {isLoadingChart ? (
                        <View style={{ height: 140, justifyContent: 'center', alignItems: 'center' }}>
                            <ActivityIndicator size="small" color="#2E7D32" />
                        </View>
                    ) : (
                        <DualLineChart
                            data1={chartData.sales}
                            data2={chartData.profit}
                            labels={chartData.labels}
                            color1="#2E7D32"
                            color2="#F57C00"
                            height={140}
                        />
                    )}
                </View>

                {/* Category Breakdown */}
                <View style={styles.pieChartCard}>
                    <Text style={styles.chartTitle}>श्रेणी वार रिपोर्ट</Text>

                    {categoryBreakdown.length === 0 ? (
                        <View style={styles.emptyCategory}>
                            <FontAwesome name="pie-chart" size={32} color="#BDBDBD" />
                            <Text style={styles.emptyCategoryText}>प्रोडक्ट्स जोड़ें श्रेणी रिपोर्ट देखने के लिए</Text>
                        </View>
                    ) : (
                        <View style={styles.pieChartContainer}>
                            {/* Simplified Pie Visualization */}
                            <View style={styles.pieChart}>
                                {categoryBreakdown.slice(0, 4).map((item, index) => (
                                    <View
                                        key={item.name}
                                        style={[
                                            styles.pieSegment,
                                            {
                                                backgroundColor: item.color,
                                                width: '50%',
                                                height: '50%',
                                                position: 'absolute',
                                                top: index < 2 ? 0 : undefined,
                                                bottom: index >= 2 ? 0 : undefined,
                                                left: index % 2 === 0 ? 0 : undefined,
                                                right: index % 2 !== 0 ? 0 : undefined,
                                            }
                                        ]}
                                    />
                                ))}
                            </View>

                            {/* Legend */}
                            <View style={styles.categoryLegendContainer}>
                                {categoryBreakdown.map((item, index) => (
                                    <View key={item.name} style={styles.categoryLegendItem}>
                                        <View style={[styles.categoryLegendDot, { backgroundColor: item.color }]} />
                                        <Text style={styles.categoryLegendText}>{item.name}</Text>
                                        <Text style={styles.categoryLegendPercent}>{item.percentage}%</Text>
                                    </View>
                                ))}
                            </View>
                        </View>
                    )}
                </View>
            </ScrollView>

            {/* FAB */}
            <FAB
                onNewSale={() => setShowSaleForm(true)}
                onNewExpense={() => setShowExpenseForm(true)}
                onAddProduct={() => setShowProductForm(true)}
                onAddCustomer={() => setShowCustomerForm(true)}
            />

            {/* Forms */}
            <AddSaleForm visible={showSaleForm} onClose={() => setShowSaleForm(false)} />
            <AddExpenseForm visible={showExpenseForm} onClose={() => setShowExpenseForm(false)} />
            <AddProductForm visible={showProductForm} onClose={() => setShowProductForm(false)} />
            <AddCustomerForm visible={showCustomerForm} onClose={() => setShowCustomerForm(false)} />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F5F5F5',
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
    },
    headerSafeArea: {
        backgroundColor: '#FFFFFF',
    },
    filterWrapper: {
        height: 48,
    },
    filterScrollView: {
        flexGrow: 0,
    },
    filtersContainer: {
        flexDirection: 'row',
        paddingHorizontal: 16,
        paddingVertical: 8,
        alignItems: 'center',
    },
    filterChip: {
        paddingHorizontal: 16,
        paddingVertical: 6,
        borderRadius: 16,
        borderWidth: 1,
        borderColor: '#E0E0E0',
        backgroundColor: '#FFFFFF',
        marginRight: 8,
    },
    filterChipActive: {
        backgroundColor: '#2E7D32',
        borderColor: '#2E7D32',
    },
    filterChipText: {
        fontSize: 14,
        fontWeight: '500',
        color: '#616161',
    },
    filterChipTextActive: {
        color: '#FFFFFF',
    },
    searchContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginVertical: 12,
        paddingHorizontal: 16,
        paddingVertical: 12,
        borderRadius: 12,
        gap: 12,
        borderWidth: 1,
        borderColor: '#E0E0E0',
    },
    searchInput: {
        flex: 1,
        fontSize: 16,
        color: '#212121',
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 100,
        paddingTop: 12,
    },
    summaryRow: {
        flexDirection: 'row',
        paddingHorizontal: 16,
        gap: 12,
    },
    summaryCard: {
        flex: 1,
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        padding: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    summaryLabel: {
        fontSize: 12,
        color: '#757575',
        marginBottom: 4,
    },
    summaryValue: {
        fontSize: 20,
        fontWeight: '700',
        color: '#2E7D32',
    },
    todayCard: {
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginTop: 12,
        borderRadius: 12,
        padding: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    todayTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 16,
    },
    todayStats: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    todayStat: {
        flex: 1,
        alignItems: 'center',
    },
    todayStatLabel: {
        fontSize: 12,
        color: '#757575',
        marginBottom: 4,
    },
    todayStatValue: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
    },
    todayDivider: {
        width: 1,
        height: 40,
        backgroundColor: '#E0E0E0',
    },
    chartCard: {
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginTop: 16,
        borderRadius: 16,
        padding: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    chartHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 16,
    },
    chartTitle: {
        fontSize: 16,
        fontWeight: '700',
        color: '#212121',
    },
    legendRow: {
        flexDirection: 'row',
        gap: 16,
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
        color: '#757575',
    },
    viewReportBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
    },
    viewReportText: {
        fontSize: 14,
        color: '#2E7D32',
        fontWeight: '500',
    },
    pieChartCard: {
        backgroundColor: '#FFFFFF',
        marginHorizontal: 16,
        marginTop: 16,
        borderRadius: 16,
        padding: 16,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    emptyCategory: {
        alignItems: 'center',
        paddingVertical: 32,
    },
    emptyCategoryText: {
        fontSize: 14,
        color: '#757575',
        marginTop: 12,
        textAlign: 'center',
    },
    pieChartContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: 16,
    },
    pieChart: {
        width: 120,
        height: 120,
        borderRadius: 60,
        overflow: 'hidden',
        marginRight: 24,
    },
    pieSegment: {
        position: 'absolute',
    },
    categoryLegendContainer: {
        flex: 1,
    },
    categoryLegendItem: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 8,
    },
    categoryLegendDot: {
        width: 12,
        height: 12,
        borderRadius: 6,
        marginRight: 8,
    },
    categoryLegendText: {
        flex: 1,
        fontSize: 12,
        color: '#616161',
    },
    categoryLegendPercent: {
        fontSize: 12,
        fontWeight: '600',
        color: '#212121',
    },
});
