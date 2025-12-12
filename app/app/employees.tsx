import React, { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { View, Text, ScrollView, StatusBar, TouchableOpacity, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router, useFocusEffect } from 'expo-router';
import { Employee } from '@/types';
import * as employeeService from '@/services/employeeService';
import { AddEmployeeForm } from '@/components/forms/AddEmployeeForm';
import { Colors } from '@/constants/theme';

export default function EmployeesScreen() {
    const { t } = useTranslation();
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
            t('employees.deleteEmployee'),
            t('employees.deleteEmployeeConfirm', { name: employee.name }),
            [
                { text: t('common.cancel'), style: 'cancel' },
                {
                    text: t('common.delete'),
                    style: 'destructive',
                    onPress: async () => {
                        try {
                            await employeeService.deleteEmployee(employee.id);
                            loadEmployees();
                        } catch {
                            Alert.alert(t('common.error'), t('employees.deleteError'));
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
                <ActivityIndicator size="large" color={Colors.light.primary} />
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <StatusBar barStyle="dark-content" backgroundColor={Colors.light.white} />

            {/* Header */}
            <SafeAreaView edges={['top']} style={styles.headerSafeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                        <FontAwesome name="arrow-left" size={20} color={Colors.light.text} />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>{t('employees.title')}</Text>
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
                    <MaterialIcons name="person-add" size={24} color={Colors.light.white} />
                    <Text style={styles.addButtonText}>{t('employees.addEmployee')}</Text>
                </TouchableOpacity>

                {/* Employee List */}
                <View style={styles.sectionContainer}>
                    <Text style={styles.sectionTitle}>{t('employees.employeeList')} ({employees.length})</Text>

                    {employees.length === 0 ? (
                        <View style={styles.emptyState}>
                            <FontAwesome name="users" size={48} color={Colors.light.textTertiary} />
                            <Text style={styles.emptyStateText}>{t('employees.noEmployees')}</Text>
                            <Text style={styles.emptyStateSubtext}>{t('employees.addEmployeeHint')}</Text>
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
                                    <FontAwesome name="user-circle" size={40} color={Colors.light.primary} />
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
        backgroundColor: Colors.light.background,
    },
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: Colors.light.background,
    },
    headerSafeArea: {
        backgroundColor: Colors.light.white,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        backgroundColor: Colors.light.white,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.border,
    },
    backButton: {
        padding: 8,
    },
    headerTitle: {
        fontSize: 20,
        fontWeight: '700',
        color: Colors.light.text,
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
        backgroundColor: Colors.light.primary,
        marginHorizontal: 16,
        marginTop: 16,
        paddingVertical: 14,
        borderRadius: 12,
        gap: 8,
    },
    addButtonText: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.white,
    },
    sectionContainer: {
        paddingHorizontal: 16,
        marginTop: 24,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
        marginBottom: 16,
    },
    emptyState: {
        alignItems: 'center',
        paddingVertical: 48,
        backgroundColor: Colors.light.white,
        borderRadius: 12,
    },
    emptyStateText: {
        fontSize: 16,
        fontWeight: '600',
        color: Colors.light.textSecondary,
        marginTop: 12,
    },
    emptyStateSubtext: {
        fontSize: 14,
        color: Colors.light.textTertiary,
        marginTop: 4,
    },
    employeeCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: Colors.light.white,
        padding: 16,
        marginBottom: 12,
        borderRadius: 12,
        shadowColor: Colors.light.shadow,
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
        color: Colors.light.text,
        marginBottom: 2,
    },
    employeeRole: {
        fontSize: 14,
        color: Colors.light.primary,
        fontWeight: '500',
    },
    employeePhone: {
        fontSize: 13,
        color: Colors.light.textSecondary,
        marginTop: 2,
    },
    salaryBadge: {
        backgroundColor: Colors.light.primaryLight,
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 8,
    },
    salaryText: {
        fontSize: 13,
        fontWeight: '600',
        color: Colors.light.primary,
    },
});
