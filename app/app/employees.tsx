import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router, useFocusEffect } from 'expo-router';
import { Employee } from '@/types';
import * as employeeService from '@/services/employeeService';
import { AddEmployeeForm } from '@/components/forms/AddEmployeeForm';

export default function EmployeesScreen() {
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [showAddForm, setShowAddForm] = useState(false);
    const [editEmployee, setEditEmployee] = useState<Employee | null>(null);

    const loadEmployees = async () => {
        try {
            const data = await employeeService.getEmployees();
            setEmployees(data);
        } catch (error) {
            console.error('Error loading employees:', error);
        } finally {
            setIsLoading(false);
        }
    };

    useFocusEffect(
        useCallback(() => {
            loadEmployees();
        }, [])
    );

    const handleEditEmployee = (employee: Employee) => {
        setEditEmployee(employee);
        setShowAddForm(true);
    };

    const handleAddEmployee = () => {
        setEditEmployee(null);
        setShowAddForm(true);
    };

    const handleDeleteEmployee = (employee: Employee) => {
        Alert.alert(
            'कर्मचारी हटाएं',
            `क्या आप "${employee.name}" को हटाना चाहते हैं?`,
            [
                { text: 'रद्द करें', style: 'cancel' },
                {
                    text: 'हटाएं',
                    style: 'destructive',
                    onPress: async () => {
                        try {
                            await employeeService.deleteEmployee(employee.id);
                            loadEmployees();
                        } catch (error) {
                            Alert.alert('त्रुटि', 'कर्मचारी हटाने में समस्या हुई');
                        }
                    },
                },
            ]
        );
    };

    const handleFormClose = () => {
        setShowAddForm(false);
        setEditEmployee(null);
    };

    const handleFormSuccess = () => {
        loadEmployees();
    };

    if (isLoading) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#2E7D32" />
            </View>
        );
    }

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
                    <View style={styles.backButton} />
                </View>
            </SafeAreaView>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Add Employee Button */}
                <TouchableOpacity style={styles.addButton} onPress={handleAddEmployee}>
                    <MaterialIcons name="person-add" size={24} color="#FFFFFF" />
                    <Text style={styles.addButtonText}>नया कर्मचारी जोड़ें</Text>
                </TouchableOpacity>

                {/* Employee List */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>कर्मचारी सूची ({employees.length})</Text>

                    {employees.length === 0 ? (
                        <View style={styles.emptyState}>
                            <FontAwesome name="users" size={48} color="#BDBDBD" />
                            <Text style={styles.emptyStateText}>अभी कोई कर्मचारी नहीं है</Text>
                            <Text style={styles.emptyStateSubtext}>नया कर्मचारी जोड़ने के लिए ऊपर बटन दबाएं</Text>
                        </View>
                    ) : (
                        employees.map((employee) => (
                            <TouchableOpacity
                                key={employee.id}
                                style={styles.employeeCard}
                                onPress={() => handleEditEmployee(employee)}
                                onLongPress={() => handleDeleteEmployee(employee)}
                                activeOpacity={0.7}
                            >
                                <View style={styles.avatarContainer}>
                                    <FontAwesome name="user-circle" size={40} color="#2E7D32" />
                                </View>
                                <View style={styles.employeeInfo}>
                                    <Text style={styles.employeeName}>{employee.name}</Text>
                                    <Text style={styles.employeeRole}>{employee.role}</Text>
                                    {employee.phone && (
                                        <Text style={styles.employeePhone}>{employee.phone}</Text>
                                    )}
                                </View>
                                {employee.salary && (
                                    <View style={styles.salaryBadge}>
                                        <Text style={styles.salaryText}>₹{employee.salary.toLocaleString('hi-IN')}</Text>
                                    </View>
                                )}
                            </TouchableOpacity>
                        ))
                    )}
                </View>
            </ScrollView>

            {/* Add/Edit Employee Form */}
            <AddEmployeeForm
                visible={showAddForm}
                onClose={handleFormClose}
                onSuccess={handleFormSuccess}
                editEmployee={editEmployee || undefined}
            />
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
    emptyState: {
        alignItems: 'center',
        paddingVertical: 48,
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
    },
    emptyStateText: {
        fontSize: 16,
        fontWeight: '600',
        color: '#757575',
        marginTop: 12,
    },
    emptyStateSubtext: {
        fontSize: 14,
        color: '#9E9E9E',
        marginTop: 4,
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
        marginBottom: 2,
    },
    employeeRole: {
        fontSize: 14,
        color: '#2E7D32',
        fontWeight: '500',
    },
    employeePhone: {
        fontSize: 13,
        color: '#757575',
        marginTop: 2,
    },
    salaryBadge: {
        backgroundColor: '#E8F5E9',
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 8,
    },
    salaryText: {
        fontSize: 13,
        fontWeight: '600',
        color: '#2E7D32',
    },
});
