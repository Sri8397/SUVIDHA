import React, { useState } from 'react';
import { View, Text, ScrollView, Image, StatusBar, TouchableOpacity, TextInput, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { LinearGradient } from 'expo-linear-gradient';
import { DualLineChart } from '@/components/ui/line-chart';
import { Header } from '@/components/ui/header';

// Mock data
const timePeriods = [
    { id: 'daily', label: 'दैनिक' },
    { id: 'weekly', label: 'साप्ताहिक' },
    { id: 'monthly', label: 'मासिक' },
];

const mockStats = {
    totalSales: 75000,
    totalProfit: 18000,
};

const mockWeeklyData = {
    sales: [10000, 15000, 12000, 18000, 14000, 20000, 25000],
    profit: [3000, 5000, 4000, 6000, 4500, 7000, 8000],
};
const days = ['सोम', 'मंग', 'बुध', 'गुरु', 'शुक्र', 'शनि', 'रवि'];

const categoryData = [
    { name: 'किराना', percentage: 40, color: '#1976D2' },
    { name: 'स्नैक्स', percentage: 25, color: '#2E7D32' },
    { name: 'अपवस्त्र', percentage: 20, color: '#FBC02D' },
    { name: 'ड्रिंक', percentage: 15, color: '#F57C00' },
];

export default function ReportsScreen() {
    const [selectedPeriod, setSelectedPeriod] = useState('daily');
    const [searchQuery, setSearchQuery] = useState('');

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
                        <Text style={styles.summaryLabel}>कुल बिक्री</Text>
                        <Text style={styles.summaryValue}>₹ {mockStats.totalSales.toLocaleString('en-IN')}</Text>
                    </View>
                    <View style={styles.summaryCard}>
                        <Text style={styles.summaryLabel}>कुल लाभ</Text>
                        <Text style={styles.summaryValue}>₹ {mockStats.totalProfit.toLocaleString('en-IN')}</Text>
                    </View>
                </View>

                {/* Sales & Profit Chart */}
                <View style={styles.chartCard}>
                    <View style={styles.chartHeader}>
                        <Text style={styles.chartTitle}>बिक्री और लाभ</Text>
                        <TouchableOpacity style={styles.viewReportBtn}>
                            <Text style={styles.viewReportText}>View Report</Text>
                            <FontAwesome name="arrow-right" size={12} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Dual Line Chart */}
                    <DualLineChart
                        data1={mockWeeklyData.sales}
                        data2={mockWeeklyData.profit}
                        labels={days}
                        color1="#2E7D32"
                        color2="#F57C00"
                        height={140}
                    />
                </View>

                {/* Category Pie Chart */}
                <View style={styles.pieChartCard}>
                    <Text style={styles.chartTitle}>श्रेणी वार रिपोर्ट</Text>

                    <View style={styles.pieChartContainer}>
                        {/* Simplified Pie Visualization */}
                        <View style={styles.pieChart}>
                            <View style={[styles.pieSegment, { backgroundColor: '#1976D2', width: '50%', height: '50%', position: 'absolute', top: 0, left: 0 }]} />
                            <View style={[styles.pieSegment, { backgroundColor: '#2E7D32', width: '50%', height: '50%', position: 'absolute', top: 0, right: 0 }]} />
                            <View style={[styles.pieSegment, { backgroundColor: '#FBC02D', width: '50%', height: '50%', position: 'absolute', bottom: 0, left: 0 }]} />
                            <View style={[styles.pieSegment, { backgroundColor: '#F57C00', width: '50%', height: '50%', position: 'absolute', bottom: 0, right: 0 }]} />
                        </View>

                        {/* Legend */}
                        <View style={styles.legendContainer}>
                            {categoryData.map((item, index) => (
                                <View key={index} style={styles.legendItem}>
                                    <View style={[styles.legendDot, { backgroundColor: item.color }]} />
                                    <Text style={styles.legendText}>{item.name}</Text>
                                </View>
                            ))}
                        </View>
                    </View>
                </View>
            </ScrollView>

            {/* FAB */}
            <TouchableOpacity style={styles.fab}>
                <FontAwesome name="plus" size={24} color="#FFFFFF" />
            </TouchableOpacity>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F5F5F5',
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
    chartContainer: {
        flexDirection: 'row',
        alignItems: 'flex-end',
        justifyContent: 'space-between',
        height: 120,
        paddingHorizontal: 4,
    },
    chartBarWrapper: {
        flex: 1,
        alignItems: 'center',
    },
    chartBarContainer: {
        height: 100,
        justifyContent: 'flex-end',
        width: '100%',
        alignItems: 'center',
    },
    barGroup: {
        flexDirection: 'row',
        alignItems: 'flex-end',
        gap: 2,
        height: '100%',
    },
    chartBar: {
        width: 10,
        borderRadius: 2,
        minHeight: 4,
    },
    chartLabel: {
        fontSize: 10,
        color: '#757575',
        marginTop: 8,
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
    legendContainer: {
        flex: 1,
    },
    legendItem: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 8,
    },
    legendDot: {
        width: 12,
        height: 12,
        borderRadius: 6,
        marginRight: 8,
    },
    legendText: {
        fontSize: 12,
        color: '#616161',
    },
    fab: {
        position: 'absolute',
        bottom: 20,
        right: 20,
        width: 56,
        height: 56,
        borderRadius: 28,
        backgroundColor: '#2E7D32',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 6,
        elevation: 8,
    },
});
