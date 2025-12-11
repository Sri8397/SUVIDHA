// ============================================
// Add Employee Form - Modal Form Component
// ============================================

import React, { useState, useEffect } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    Modal,
    ScrollView,
    StyleSheet,
    Alert,
    ActivityIndicator,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import * as employeeService from '@/services/employeeService';

// Predefined roles for employees
const getEmployeeRoles = (t: any) => [
    { id: 'salesman', label: t('forms.employee.roles.salesman') },
    { id: 'cashier', label: t('forms.employee.roles.cashier') },
    { id: 'stock_manager', label: t('forms.employee.roles.stock_manager') },
    { id: 'helper', label: t('forms.employee.roles.helper') },
    { id: 'delivery', label: t('forms.employee.roles.delivery') },
    { id: 'accountant', label: t('forms.employee.roles.accountant') },
    { id: 'manager', label: t('forms.employee.roles.manager') },
    { id: 'other', label: t('forms.employee.roles.other') },
];

interface AddEmployeeFormProps {
    visible: boolean;
    onClose: () => void;
    onSuccess?: () => void;
    editEmployee?: {
        id: string;
        name: string;
        role: string;
        phone: string;
        salary?: number;
    };
}

export function AddEmployeeForm({ visible, onClose, onSuccess, editEmployee }: AddEmployeeFormProps) {
    const { t } = useTranslation();
    const [isLoading, setIsLoading] = useState(false);

    // Get roles with translations
    const EMPLOYEE_ROLES = getEmployeeRoles(t);
    const [showRolePicker, setShowRolePicker] = useState(false);

    // Form state
    const [name, setName] = useState('');
    const [role, setRole] = useState('');
    const [phone, setPhone] = useState('');
    const [salary, setSalary] = useState('');

    // Sync form state when editEmployee changes or modal opens
    useEffect(() => {
        if (visible) {
            if (editEmployee) {
                setName(editEmployee.name || '');
                setRole(editEmployee.role || '');
                setPhone(editEmployee.phone || '');
                setSalary(editEmployee.salary?.toString() || '');
            } else {
                // Reset form for new employee
                resetForm();
            }
        }
    }, [visible, editEmployee]);

    const resetForm = () => {
        setName('');
        setRole('');
        setPhone('');
        setSalary('');
    };

    const handleRoleSelect = (selectedRole: string) => {
        setRole(selectedRole);
        setShowRolePicker(false);
    };

    const handleSubmit = async () => {
        // Validation
        if (!name.trim()) {
            Alert.alert(t('common.error'), t('forms.employee.enterName'));
            return;
        }
        if (!role.trim()) {
            Alert.alert(t('common.error'), t('forms.employee.selectRoleError'));
            return;
        }
        if (!phone.trim() || phone.length < 10) {
            Alert.alert(t('common.error'), t('forms.employee.enterPhone'));
            return;
        }

        setIsLoading(true);
        try {
            const employeeData = {
                name: name.trim(),
                role: role.trim(),
                phone: phone.trim(),
                salary: salary.trim() ? parseInt(salary.trim(), 10) : undefined,
            };

            if (editEmployee) {
                await employeeService.updateEmployee(editEmployee.id, employeeData);
                Alert.alert(t('common.success'), t('forms.employee.updated'));
            } else {
                await employeeService.addEmployee(employeeData);
                Alert.alert(t('common.success'), t('forms.employee.added'));
            }

            resetForm();
            onSuccess?.();
            onClose();
        } catch (error) {
            console.error('Save employee error:', error);
            Alert.alert(t('common.error'), t('forms.employee.saveError'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleClose = () => {
        if (!editEmployee) resetForm();
        onClose();
    };

    return (
        <Modal
            visible={visible}
            animationType="slide"
            presentationStyle="pageSheet"
            onRequestClose={handleClose}
        >
            <View style={styles.container}>
                {/* Header */}
                <View style={styles.header}>
                    <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
                        <FontAwesome name="times" size={20} color="#757575" />
                    </TouchableOpacity>
                    <Text style={styles.title}>
                        {editEmployee ? t('forms.employee.editTitle') : t('forms.employee.addTitle')}
                    </Text>
                    <View style={styles.closeButton} />
                </View>

                <ScrollView style={styles.form} showsVerticalScrollIndicator={false}>
                    {/* Employee Icon */}
                    <View style={styles.iconSection}>
                        <View style={styles.employeeIcon}>
                            <FontAwesome name="user" size={48} color="#2E7D32" />
                        </View>
                    </View>

                    {/* Employee Name */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.employee.name')} *</Text>
                        <TextInput
                            style={styles.input}
                            value={name}
                            onChangeText={setName}
                            placeholder={t('forms.employee.namePlaceholder')}
                            placeholderTextColor="#9E9E9E"
                        />
                    </View>

                    {/* Role - Select Option */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.employee.role')} *</Text>
                        <TouchableOpacity
                            style={styles.selectButton}
                            onPress={() => setShowRolePicker(true)}
                        >
                            <Text style={[styles.selectButtonText, !role && styles.selectPlaceholder]}>
                                {role || t('forms.employee.selectRole')}
                            </Text>
                            <FontAwesome name="chevron-down" size={14} color="#757575" />
                        </TouchableOpacity>
                    </View>

                    {/* Phone */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.employee.phone')} *</Text>
                        <TextInput
                            style={styles.input}
                            value={phone}
                            onChangeText={setPhone}
                            placeholder={t('forms.employee.phonePlaceholder')}
                            placeholderTextColor="#9E9E9E"
                            keyboardType="phone-pad"
                            maxLength={10}
                        />
                    </View>

                    {/* Salary */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.employee.salaryOptional')}</Text>
                        <TextInput
                            style={styles.input}
                            value={salary}
                            onChangeText={setSalary}
                            placeholder={t('forms.employee.salaryPlaceholder')}
                            placeholderTextColor="#9E9E9E"
                            keyboardType="numeric"
                        />
                    </View>

                    {/* Submit Button */}
                    <TouchableOpacity
                        style={[styles.submitButton, isLoading && styles.submitButtonDisabled]}
                        onPress={handleSubmit}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <Text style={styles.submitButtonText}>
                                {editEmployee ? t('common.update') : t('forms.employee.addTitle')}
                            </Text>
                        )}
                    </TouchableOpacity>
                </ScrollView>
            </View>

            {/* Role Picker Modal */}
            <Modal
                visible={showRolePicker}
                transparent
                animationType="fade"
                onRequestClose={() => setShowRolePicker(false)}
            >
                <View style={styles.pickerOverlay}>
                    <View style={styles.pickerContainer}>
                        <View style={styles.pickerHeader}>
                            <Text style={styles.pickerTitle}>{t('forms.employee.selectRole')}</Text>
                            <TouchableOpacity onPress={() => setShowRolePicker(false)}>
                                <FontAwesome name="times" size={20} color="#757575" />
                            </TouchableOpacity>
                        </View>
                        <ScrollView style={styles.pickerList}>
                            {EMPLOYEE_ROLES.map((roleItem) => (
                                <TouchableOpacity
                                    key={roleItem.id}
                                    style={[
                                        styles.pickerOption,
                                        role === roleItem.label && styles.pickerOptionSelected,
                                    ]}
                                    onPress={() => handleRoleSelect(roleItem.label)}
                                >
                                    <Text
                                        style={[
                                            styles.pickerOptionText,
                                            role === roleItem.label && styles.pickerOptionTextSelected,
                                        ]}
                                    >
                                        {roleItem.label}
                                    </Text>
                                    {role === roleItem.label && (
                                        <FontAwesome name="check" size={16} color="#2E7D32" />
                                    )}
                                </TouchableOpacity>
                            ))}
                        </ScrollView>
                    </View>
                </View>
            </Modal>
        </Modal>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    closeButton: {
        width: 40,
        height: 40,
        alignItems: 'center',
        justifyContent: 'center',
    },
    title: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
    },
    form: {
        flex: 1,
        padding: 16,
    },
    iconSection: {
        alignItems: 'center',
        marginBottom: 24,
    },
    employeeIcon: {
        width: 100,
        height: 100,
        borderRadius: 50,
        backgroundColor: '#E8F5E9',
        alignItems: 'center',
        justifyContent: 'center',
    },
    inputGroup: {
        marginBottom: 16,
    },
    label: {
        fontSize: 14,
        fontWeight: '600',
        color: '#424242',
        marginBottom: 8,
    },
    input: {
        borderWidth: 1,
        borderColor: '#E0E0E0',
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 14,
        fontSize: 16,
        color: '#212121',
        backgroundColor: '#FAFAFA',
    },
    selectButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderWidth: 1,
        borderColor: '#E0E0E0',
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 14,
        backgroundColor: '#FAFAFA',
    },
    selectButtonText: {
        fontSize: 16,
        color: '#212121',
    },
    selectPlaceholder: {
        color: '#9E9E9E',
    },
    submitButton: {
        backgroundColor: '#2E7D32',
        borderRadius: 12,
        paddingVertical: 16,
        alignItems: 'center',
        marginTop: 24,
        marginBottom: 32,
    },
    submitButtonDisabled: {
        opacity: 0.7,
    },
    submitButtonText: {
        fontSize: 16,
        fontWeight: '700',
        color: '#FFFFFF',
    },
    // Role Picker Modal Styles
    pickerOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'flex-end',
    },
    pickerContainer: {
        backgroundColor: '#FFFFFF',
        borderTopLeftRadius: 20,
        borderTopRightRadius: 20,
        maxHeight: '60%',
    },
    pickerHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#E0E0E0',
    },
    pickerTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: '#212121',
    },
    pickerList: {
        paddingVertical: 8,
    },
    pickerOption: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#F5F5F5',
    },
    pickerOptionSelected: {
        backgroundColor: '#E8F5E9',
    },
    pickerOptionText: {
        fontSize: 16,
        color: '#212121',
    },
    pickerOptionTextSelected: {
        color: '#2E7D32',
        fontWeight: '600',
    },
});
