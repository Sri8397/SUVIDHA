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
import { TransliterationInput } from '../ui/TransliterationInput';
import * as employeeService from '@/services/employeeService';
import { Colors } from '@/constants/theme';

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
                        <FontAwesome name="times" size={20} color={Colors.light.textSecondary} />
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
                            <FontAwesome name="user" size={48} color={Colors.light.primary} />
                        </View>
                    </View>

                    {/* Employee Name */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>{t('forms.employee.name')} *</Text>
                        <TransliterationInput
                            style={styles.input}
                            value={name}
                            onChangeText={setName}
                            placeholder={t('forms.employee.namePlaceholder')}
                            placeholderTextColor={Colors.light.textTertiary}
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
                            <FontAwesome name="chevron-down" size={14} color={Colors.light.textSecondary} />
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
                            placeholderTextColor={Colors.light.textTertiary}
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
                            placeholderTextColor={Colors.light.textTertiary}
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
                            <ActivityIndicator color={Colors.light.white} />
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
                                <FontAwesome name="times" size={20} color={Colors.light.textSecondary} />
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
                                        <FontAwesome name="check" size={16} color={Colors.light.primary} />
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
        backgroundColor: Colors.light.white,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 16,
        borderBottomWidth: 1,
        borderBottomColor: Colors.light.border,
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
        color: Colors.light.text,
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
        backgroundColor: Colors.light.primaryLight,
        alignItems: 'center',
        justifyContent: 'center',
    },
    inputGroup: {
        marginBottom: 16,
    },
    label: {
        fontSize: 14,
        fontWeight: '600',
        color: Colors.light.text,
        marginBottom: 8,
    },
    input: {
        borderWidth: 1,
        borderColor: Colors.light.border,
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 14,
        fontSize: 16,
        color: Colors.light.text,
        backgroundColor: Colors.light.background,
    },
    selectButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderWidth: 1,
        borderColor: Colors.light.border,
        borderRadius: 12,
        paddingHorizontal: 16,
        paddingVertical: 14,
        backgroundColor: Colors.light.background,
    },
    selectButtonText: {
        fontSize: 16,
        color: Colors.light.text,
    },
    selectPlaceholder: {
        color: Colors.light.textTertiary,
    },
    submitButton: {
        backgroundColor: Colors.light.primary,
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
        color: Colors.light.white,
    },
    // Role Picker Modal Styles
    pickerOverlay: {
        flex: 1,
        backgroundColor: Colors.light.overlay,
        justifyContent: 'flex-end',
    },
    pickerContainer: {
        backgroundColor: Colors.light.white,
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
        borderBottomColor: Colors.light.border,
    },
    pickerTitle: {
        fontSize: 18,
        fontWeight: '700',
        color: Colors.light.text,
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
        borderBottomColor: Colors.light.background,
    },
    pickerOptionSelected: {
        backgroundColor: Colors.light.primaryLight,
    },
    pickerOptionText: {
        fontSize: 16,
        color: Colors.light.text,
    },
    pickerOptionTextSelected: {
        color: Colors.light.primary,
        fontWeight: '600',
    },
});
