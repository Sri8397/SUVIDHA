import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, Image, StatusBar, StyleSheet, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { useTranslation } from 'react-i18next';
import { DualLineChart } from '@/components/ui/line-chart';
import { Header } from '@/components/ui/header';
import { FAB } from '@/components/ui/fab';
import { useData } from '@/contexts/DataContext';
import { useAppMode } from '@/contexts/ModeContext';
import { AddSaleForm } from '@/components/forms/AddSaleForm';
import { AddExpenseForm } from '@/components/forms/AddExpenseForm';
import { AddProductForm } from '@/components/forms/AddProductForm';
import { AddCustomerForm } from '@/components/forms/AddCustomerForm';
import { AddIncomeForm } from '@/components/forms/AddIncomeForm';
import { AddLoanForm } from '@/components/forms/AddLoanForm';
import { Colors } from '@/constants/theme';

export default function HomeScreen() {
  const { t } = useTranslation();
  const router = useRouter();
  const { isLoading, dashboardStats, weeklyData, lowStockProducts } = useData();
  const { appMode } = useAppMode();
  const isPersonalMode = appMode === 'personal';

  // Form visibility states
  const [showSaleForm, setShowSaleForm] = useState(false);
  const [showExpenseForm, setShowExpenseForm] = useState(false);
  const [showProductForm, setShowProductForm] = useState(false);
  const [showCustomerForm, setShowCustomerForm] = useState(false);
  const [showIncomeForm, setShowIncomeForm] = useState(false);
  const [showLoanForm, setShowLoanForm] = useState(false);

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={Colors.light.primary} />
        <Text style={styles.loadingText}>{t('common.loading')}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />

      {/* Header */}
      <Header title="Suvidha" showOnlineStatus={true} showLanguageToggle={true} />

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Welcome Card */}
        <View style={styles.welcomeCard}>
          <Text style={styles.welcomeTitle}>{isPersonalMode ? t('personal.dashboard.greeting') : t('dashboard.greeting')}</Text>
          <Text style={styles.welcomeSubtitle}>{isPersonalMode ? t('personal.dashboard.greetingSummary') : t('dashboard.greetingSummary')}</Text>
        </View>

        {/* Stats Grid */}
        <View style={styles.statsGrid}>
          {/* Today's Sales */}
          <View style={styles.statCardWrapper}>
            <TouchableOpacity
              style={styles.statCard}
              onPress={() => isPersonalMode ? setShowIncomeForm(true) : setShowSaleForm(true)}
              activeOpacity={0.7}
            >
              <View style={styles.statHeader}>
                <View style={[styles.statIcon, { backgroundColor: Colors.light.primaryLight }]}>
                  <Text style={[styles.rupeeIcon, { color: Colors.light.primary }]}>₹</Text>
                </View>
              </View>
              <Text style={styles.statLabel}>{isPersonalMode ? t('personal.todaysIncome') : t('dashboard.todaysSales')}</Text>
              <Text style={[styles.statValue, { color: Colors.light.primary }]}>₹ {dashboardStats.todaySales.toLocaleString('en-IN')}</Text>
            </TouchableOpacity>
          </View>

          {/* Pending Payments */}
          <View style={styles.statCardWrapper}>
            <TouchableOpacity
              style={styles.statCard}
              onPress={() => isPersonalMode && setShowLoanForm(true)}
              activeOpacity={0.7}
            >
              <View style={styles.statHeader}>
                <View style={[styles.statIcon, { backgroundColor: Colors.light.errorLight }]}>
                  <FontAwesome name="shopping-bag" size={16} color={Colors.light.error} />
                </View>
              </View>
              <Text style={styles.statLabel}>{isPersonalMode ? t('personal.loanGiven') : t('dashboard.pendingPayments')}</Text>
              <Text style={styles.statValue}>₹ {dashboardStats.pendingPayments.toLocaleString('en-IN')}</Text>
            </TouchableOpacity>
          </View>

          {/* Profit */}
          <View style={styles.statCardWrapper}>
            <View style={styles.statCard}>
              <View style={styles.statHeader}>
                <View style={[styles.statIcon, { backgroundColor: Colors.light.secondaryLight }]}>
                  <MaterialIcons name="trending-up" size={18} color={Colors.light.secondary} />
                </View>
              </View>
              <Text style={styles.statLabel}>{isPersonalMode ? t('personal.savings') : t('dashboard.profit')}</Text>
              <Text style={[styles.statValue, { color: Colors.light.secondary }]}>₹ {dashboardStats.profit.toLocaleString('en-IN')}</Text>
            </View>
          </View>

          {/* Expenses */}
          <View style={styles.statCardWrapper}>
            <TouchableOpacity
              style={styles.statCard}
              onPress={() => setShowExpenseForm(true)}
              activeOpacity={0.7}
            >
              <View style={styles.statHeader}>
                <View style={[styles.statIcon, { backgroundColor: Colors.light.errorLight }]}>
                  <MaterialIcons name="trending-down" size={18} color={Colors.light.error} />
                </View>
              </View>
              <Text style={styles.statLabel}>{t('dashboard.expenses')}</Text>
              <Text style={[styles.statValue, { color: Colors.light.error }]}>₹ {dashboardStats.expenses.toLocaleString('en-IN')}</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Weekly Sales Chart */}
        <View style={styles.chartCard}>
          <View style={styles.chartHeader}>
            <Text style={styles.chartTitle}>{isPersonalMode ? t('personal.weeklyIncomeSavings') : t('dashboard.weeklySalesProfit')}</Text>
            <TouchableOpacity
              style={styles.viewReportBtn}
              onPress={() => router.push('/(tabs)/reports?period=weekly')}
            >
              <Text style={styles.viewReportText}>{t('common.viewAll')}</Text>
              <FontAwesome name="arrow-right" size={12} color={Colors.light.primary} />
            </TouchableOpacity>
          </View>

          {/* Dual Line Chart */}
          <DualLineChart
            data1={weeklyData.sales}
            data2={weeklyData.profit}
            labels={weeklyData.labels}
            color1={Colors.light.primary}
            color2={Colors.light.secondary}
            height={140}
            isPersonalMode={isPersonalMode}
          />
        </View>

        {/* Low Stock Alerts - only show in shopkeeper mode */}
        {
          !isPersonalMode && (
            <View style={styles.lowStockSection}>
              <View style={styles.sectionHeader}>
                <Text style={styles.sectionTitle}>{t('dashboard.lowStockAlerts')}</Text>
                <TouchableOpacity onPress={() => router.push('/(tabs)/inventory?filter=low-stock')}>
                  <Text style={styles.viewAllText}>{t('common.viewAll')}</Text>
                </TouchableOpacity>
              </View>

              {lowStockProducts.length === 0 ? (
                <View style={styles.emptyLowStock}>
                  <FontAwesome name="check-circle" size={32} color={Colors.light.primary} />
                  <Text style={styles.emptyLowStockText}>{t('dashboard.allProductsInStock')}</Text>
                </View>
              ) : (
                lowStockProducts.slice(0, 3).map((item) => (
                  <View key={item.id} style={styles.lowStockCard}>
                    {item.image ? (
                      <Image source={{ uri: item.image }} style={styles.productImage} />
                    ) : (
                      <View style={[styles.productImage, styles.productImagePlaceholder]}>
                        <FontAwesome name="cube" size={24} color={Colors.light.textTertiary} />
                      </View>
                    )}
                    <View style={styles.productInfo}>
                      <Text style={styles.productName}>{item.name}</Text>
                      <Text style={styles.stockWarning}>{t('inventory.lowStock', { count: item.stock })}</Text>
                    </View>
                    <TouchableOpacity style={styles.reorderButton}>
                      <Text style={styles.reorderButtonText}>{t('inventory.reorder')}</Text>
                    </TouchableOpacity>
                  </View>
                ))
              )}
            </View>
          )
        }
      </ScrollView >

      {/* FAB */}
      < FAB
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
    </View >
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
    backgroundColor: Colors.light.white,
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: Colors.light.textSecondary,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 100,
  },
  welcomeCard: {
    marginHorizontal: 16,
    marginTop: 16,
    borderRadius: 16,
    padding: 24,
    backgroundColor: Colors.light.primaryLight,
  },
  welcomeTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: Colors.light.primary,
    marginBottom: 8,
  },
  welcomeSubtitle: {
    fontSize: 14,
    color: Colors.light.primary,
    opacity: 0.9,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 12,
    marginTop: 12,
  },
  statCardWrapper: {
    width: '50%',
    padding: 4,
  },
  statCard: {
    backgroundColor: Colors.light.white,
    borderRadius: 12,
    padding: 16,
    shadowColor: Colors.light.shadow,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  statHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  statIcon: {
    width: 36,
    height: 36,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  rupeeIcon: {
    fontSize: 18,
    fontWeight: '600',
    color: Colors.light.info,
  },
  statLabel: {
    fontSize: 12,
    color: Colors.light.textSecondary,
    marginBottom: 4,
  },
  statValue: {
    fontSize: 20,
    fontWeight: '700',
    color: Colors.light.text,
  },
  chartCard: {
    backgroundColor: Colors.light.white,
    marginHorizontal: 16,
    marginTop: 12,
    borderRadius: 16,
    padding: 16,
    shadowColor: Colors.light.shadow,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
    position: 'relative',
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
    color: Colors.light.text,
  },
  viewReportBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  viewReportText: {
    fontSize: 14,
    color: Colors.light.primary,
    fontWeight: '500',
  },
  lowStockSection: {
    marginHorizontal: 16,
    marginTop: 16,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: Colors.light.text,
  },
  viewAllText: {
    fontSize: 14,
    color: Colors.light.primary,
    fontWeight: '500',
  },
  emptyLowStock: {
    backgroundColor: Colors.light.white,
    borderRadius: 12,
    padding: 24,
    alignItems: 'center',
  },
  emptyLowStockText: {
    fontSize: 14,
    color: Colors.light.textSecondary,
    marginTop: 12,
  },
  lowStockCard: {
    backgroundColor: Colors.light.white,
    borderRadius: 12,
    padding: 12,
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
    shadowColor: Colors.light.shadow,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  productImage: {
    width: 56,
    height: 56,
    borderRadius: 8,
    backgroundColor: Colors.light.background,
    marginRight: 12,
  },
  productImagePlaceholder: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  productInfo: {
    flex: 1,
  },
  productName: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.light.text,
    marginBottom: 4,
  },
  stockWarning: {
    fontSize: 12,
    color: Colors.light.error,
  },
  reorderButton: {
    backgroundColor: Colors.light.error,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
  },
  reorderButtonText: {
    color: Colors.light.white,
    fontSize: 12,
    fontWeight: '600',
  },
});
