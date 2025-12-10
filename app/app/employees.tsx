import React from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router } from 'expo-router';

export default function EmployeesScreen() {
    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            {/* Header */}
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color="#212121" />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>कर्मचारी प्रबंधन</Text>
                    <TouchableOpacity style={styles.notificationButton}>
                        <FontAwesome name="bell" size={20} color="#212121" />
                    </TouchableOpacity>
                </View>
            </SafeAreaView>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Add Employee Button */}
                <TouchableOpacity style={styles.addButton}>
                    <MaterialIcons name="person-add" size={24} color="#FFFFFF" />
                    <Text style={styles.addButtonText}>नया कर्मचारी जोड़ें</Text>
                </TouchableOpacity>

                {/* Employee List */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>कर्मचारी सूची</Text>

                    {/* Employee Card 1 */}
                    <View style={styles.employeeCard}>
                        <View style={styles.avatarContainer}>
                            <FontAwesome name="user-circle" size={40} color="#2E7D32" />
                        </View>
                        <View style={styles.employeeInfo}>
                            <Text style={styles.employeeName}>राजेश शर्मा</Text>
                            <Text style={styles.employeeRole}>सेल्समैन</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Employee Card 2 */}
                    <View style={styles.employeeCard}>
                        <View style={styles.avatarContainer}>
                            <FontAwesome name="user-circle" size={40} color="#2E7D32" />
                        </View>
                        <View style={styles.employeeInfo}>
                            <Text style={styles.employeeName}>सुनीता देवी</Text>
                            <Text style={styles.employeeRole}>कैशियर</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>

                    {/* Employee Card 3 */}
                    <View style={styles.employeeCard}>
                        <View style={styles.avatarContainer}>
                            <FontAwesome name="user-circle" size={40} color="#2E7D32" />
                        </View>
                        <View style={styles.employeeInfo}>
                            <Text style={styles.employeeName}>अमित कुमार</Text>
                            <Text style={styles.employeeRole}>स्टॉक मैनेजर</Text>
                        </View>
                        <TouchableOpacity style={styles.editIcon}>
                            <FontAwesome name="pencil" size={18} color="#2E7D32" />
                        </TouchableOpacity>
                    </View>
                </View>
            </ScrollView>
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
        paddingVertical: 16,
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    backButton: {
        padding: 8,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: '#212121',
    },
    notificationButton: {
        padding: 8,
    },
    scrollView: {
        flex: 1,
    },
    scrollContent: {
        paddingBottom: 24,
    },
    addButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#2E7D32',
        marginHorizontal: 16,
        marginTop: 16,
        paddingVertical: 14,
        borderRadius: 12,
        gap: 8,
    },
    addButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#FFFFFF',
    },
    sectionContainer: {
        paddingHorizontal: 16,
        marginTop: 24,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
        marginBottom: 16,
    },
    employeeCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        padding: 16,
        marginBottom: 12,
        borderRadius: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.08,
        shadowRadius: 4,
        elevation: 2,
    },
    avatarContainer: {
        marginRight: 12,
    },
    employeeInfo: {
        flex: 1,
    },
    employeeName: {
        fontSize: 16,
        fontWeight: '600',
        color: '#212121',
        marginBottom: 4,
    },
    employeeRole: {
        fontSize: 14,
        color: '#757575',
    },
    editIcon: {
        padding: 8,
    },
});
