import React from 'react';
import { View, Text, ScrollView, TouchableOpacity, Image, StatusBar, StyleSheet, Dimensions } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { LinearGradient } from 'expo-linear-gradient';
import { LineChart } from '@/components/ui/line-chart';
import { Header } from '@/components/ui/header';

const { width } = Dimensions.get('window');

// Mock data
const mockStats = {
  todaySales: 12500,
  pendingPayments: 3200,
  profit: 4800,
  expenses: 1500,
};

const mockWeeklySales = [8000, 10000, 7000, 12000, 9000, 11000, 15000];
const days = ['सोम', 'मंग', 'बुध', 'गुरु', 'शुक्र', 'शनि', 'रवि'];

const mockLowStock = [
  { id: '1', name: 'पारले-जी बिस्कुट', stock: 5, image: 'https://m.media-amazon.com/images/I/61WfCf0CEQL.jpg' },
  { id: '2', name: 'आशीर्वाद आटा', stock: 2, image: 'https://m.media-amazon.com/images/I/71xWPpJdpwL.jpg' },
];

export default function HomeScreen() {
  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

      {/* Header */}
      <Header title="Suvidha" showOnlineStatus={true} showLanguageToggle={true} />

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Welcome Card */}
        <View style={styles.welcomeCard}>
          <Text style={styles.welcomeTitle}>नमस्ते, रमेश!</Text>
          <Text style={styles.welcomeSubtitle}>आज के लिए आपका व्यवसाय सारांश यहाँ है।</Text>
        </View>

        {/* Stats Grid */}
        <View style={styles.statsGrid}>
          {/* Today's Sales */}
          <View style={styles.statCardWrapper}>
            <View style={styles.statCard}>
              <View style={styles.statHeader}>
                <View style={[styles.statIcon, { backgroundColor: '#E3F2FD' }]}>
                  <Text style={styles.rupeeIcon}>₹</Text>
                </View>
              </View>
              <Text style={styles.statLabel}>आज की बिक्री</Text>
              <Text style={styles.statValue}>₹ {mockStats.todaySales.toLocaleString('en-IN')}</Text>
            </View>
          </View>

          {/* Pending Payments */}
          <View style={styles.statCardWrapper}>
            <View style={styles.statCard}>
              <View style={styles.statHeader}>
                <View style={[styles.statIcon, { backgroundColor: '#FFEBEE' }]}>
                  <FontAwesome name="shopping-bag" size={16} color="#D32F2F" />
                </View>
              </View>
              <Text style={styles.statLabel}>लंबित भुगतान</Text>
              <Text style={styles.statValue}>₹ {mockStats.pendingPayments.toLocaleString('en-IN')}</Text>
            </View>
          </View>

          {/* Profit */}
          <View style={styles.statCardWrapper}>
            <View style={styles.statCard}>
              <View style={styles.statHeader}>
                <View style={[styles.statIcon, { backgroundColor: '#E8F5E9' }]}>
                  <MaterialIcons name="trending-up" size={18} color="#2E7D32" />
                </View>
              </View>
              <Text style={styles.statLabel}>लाभ</Text>
              <Text style={[styles.statValue, { color: '#2E7D32' }]}>₹ {mockStats.profit.toLocaleString('en-IN')}</Text>
            </View>
          </View>

          {/* Expenses */}
          <View style={styles.statCardWrapper}>
            <View style={styles.statCard}>
              <View style={styles.statHeader}>
                <View style={[styles.statIcon, { backgroundColor: '#FFF8E1' }]}>
                  <MaterialIcons name="trending-down" size={18} color="#F57C00" />
                </View>
              </View>
              <Text style={styles.statLabel}>खर्च</Text>
              <Text style={[styles.statValue, { color: '#F57C00' }]}>₹ {mockStats.expenses.toLocaleString('en-IN')}</Text>
            </View>
          </View>
        </View>

        {/* Weekly Sales Chart */}
        <View style={styles.chartCard}>
          <View style={styles.chartHeader}>
            <Text style={styles.chartTitle}>साप्ताहिक बिक्री</Text>
            <TouchableOpacity style={styles.viewReportBtn}>
              <Text style={styles.viewReportText}>View Report</Text>
              <FontAwesome name="arrow-right" size={12} color="#2E7D32" />
            </TouchableOpacity>
          </View>

          {/* Line Chart */}
          <LineChart
            data={mockWeeklySales}
            labels={days}
            color="#2E7D32"
            height={140}
            showDots={true}
            showValues={true}
          />
        </View>

        {/* Low Stock Alerts */}
        <View style={styles.lowStockSection}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>कम स्टॉक अलर्ट</Text>
            <TouchableOpacity>
              <Text style={styles.viewAllText}>View All</Text>
            </TouchableOpacity>
          </View>

          {mockLowStock.map((item) => (
            <View key={item.id} style={styles.lowStockCard}>
              <Image source={{ uri: item.image }} style={styles.productImage} />
              <View style={styles.productInfo}>
                <Text style={styles.productName}>{item.name}</Text>
                <Text style={styles.stockWarning}>केवल {item.stock} स्टॉक में बचे हैं</Text>
              </View>
              <TouchableOpacity style={styles.reorderButton}>
                <Text style={styles.reorderButtonText}>पुनः ऑर्डर करें</Text>
              </TouchableOpacity>
            </View>
          ))}
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
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#FFFFFF',
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  profileImage: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#E8F5E9',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  logo: {
    width: 40,
    height: 40,
  },
  storeName: {
    fontSize: 18,
    fontWeight: '700',
    color: '#212121',
  },
  onlineStatus: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  onlineDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#2E7D32',
  },
  onlineText: {
    fontSize: 12,
    color: '#757575',
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  langButton: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    backgroundColor: '#F5F5F5',
    borderRadius: 8,
  },
  langText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#212121',
  },
  bellButton: {
    position: 'relative',
  },
  notificationBadge: {
    position: 'absolute',
    top: -2,
    right: -2,
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#D32F2F',
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
    backgroundColor: '#E8F5E9',
  },
  welcomeTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#2E7D32',
    marginBottom: 8,
  },
  welcomeSubtitle: {
    fontSize: 14,
    color: '#2E7D32',
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
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  statHeader: {
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
    color: '#1976D2',
  },
  statLabel: {
    fontSize: 12,
    color: '#757575',
    marginBottom: 4,
  },
  statValue: {
    fontSize: 20,
    fontWeight: '700',
    color: '#212121',
  },
  chartCard: {
    backgroundColor: '#FFFFFF',
    marginHorizontal: 16,
    marginTop: 12,
    borderRadius: 16,
    padding: 16,
    shadowColor: '#000',
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
    paddingHorizontal: 8,
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
  chartBar: {
    width: 20,
    borderRadius: 4,
    minHeight: 8,
  },
  chartLabel: {
    fontSize: 10,
    color: '#757575',
    marginTop: 8,
  },
  chartAnnotation: {
    position: 'absolute',
    top: 40,
    right: 24,
    backgroundColor: '#2E7D32',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
  },
  chartAnnotationText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '600',
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
    color: '#212121',
  },
  viewAllText: {
    fontSize: 14,
    color: '#2E7D32',
    fontWeight: '500',
  },
  lowStockCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 12,
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  productImage: {
    width: 56,
    height: 56,
    borderRadius: 8,
    backgroundColor: '#F5F5F5',
    marginRight: 12,
  },
  productInfo: {
    flex: 1,
  },
  productName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#212121',
    marginBottom: 4,
  },
  stockWarning: {
    fontSize: 12,
    color: '#D32F2F',
  },
  reorderButton: {
    backgroundColor: '#D32F2F',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
  },
  reorderButtonText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '600',
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
