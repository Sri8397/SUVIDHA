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
import FontAwesome from '@expo/vector-icons/FontAwesome';
import * as employeeService from '@/services/employeeService';

// Predefined roles for employees
const EMPLOYEE_ROLES = [
    { id: 'salesman', label: 'सेल्समैन' },
    { id: 'cashier', label: 'कैशियर' },
    { id: 'stock_manager', label: 'स्टॉक मैनेजर' },
    { id: 'helper', label: 'हेल्पर' },
    { id: 'delivery', label: 'डिलीवरी' },
    { id: 'accountant', label: 'अकाउंटेंट' },
    { id: 'manager', label: 'मैनेजर' },
    { id: 'other', label: 'अन्य' },
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
    const [isLoading, setIsLoading] = useState(false);
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
            Alert.alert('त्रुटि', 'कर्मचारी का नाम दर्ज करें');
            return;
        }
        if (!role.trim()) {
            Alert.alert('त्रुटि', 'पद/भूमिका चुनें');
            return;
        }
        if (!phone.trim() || phone.length < 10) {
            Alert.alert('त्रुटि', 'सही फोन नंबर दर्ज करें (10 अंक)');
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
                Alert.alert('सफलता', 'कर्मचारी अपडेट हो गया');
            } else {
                await employeeService.addEmployee(employeeData);
                Alert.alert('सफलता', 'नया कर्मचारी जोड़ दिया गया');
            }

            resetForm();
            onSuccess?.();
            onClose();
        } catch (error) {
            console.error('Save employee error:', error);
            Alert.alert('त्रुटि', 'कर्मचारी सेव करने में समस्या हुई');
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
                        {editEmployee ? 'कर्मचारी एडिट करें' : 'नया कर्मचारी जोड़ें'}
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
                        <Text style={styles.label}>कर्मचारी का नाम *</Text>
                        <TextInput
                            style={styles.input}
                            value={name}
                            onChangeText={setName}
                            placeholder="जैसे: राजेश शर्मा"
                            placeholderTextColor="#9E9E9E"
                        />
                    </View>

                    {/* Role - Select Option */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>पद/भूमिका *</Text>
                        <TouchableOpacity
                            style={styles.selectButton}
                            onPress={() => setShowRolePicker(true)}
                        >
                            <Text style={[styles.selectButtonText, !role && styles.selectPlaceholder]}>
                                {role || 'भूमिका चुनें'}
                            </Text>
                            <FontAwesome name="chevron-down" size={14} color="#757575" />
                        </TouchableOpacity>
                    </View>

                    {/* Phone */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>फोन नंबर *</Text>
                        <TextInput
                            style={styles.input}
                            value={phone}
                            onChangeText={setPhone}
                            placeholder="10 अंकों का मोबाइल नंबर"
                            placeholderTextColor="#9E9E9E"
                            keyboardType="phone-pad"
                            maxLength={10}
                        />
                    </View>

                    {/* Salary */}
                    <View style={styles.inputGroup}>
                        <Text style={styles.label}>मासिक वेतन (वैकल्पिक)</Text>
                        <TextInput
                            style={styles.input}
                            value={salary}
                            onChangeText={setSalary}
                            placeholder="जैसे: 15000"
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
                                {editEmployee ? 'अपडेट करें' : 'कर्मचारी जोड़ें'}
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
                            <Text style={styles.pickerTitle}>भूमिका चुनें</Text>
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
