// ============================================
// Employee Service - CRUD Operations
// ============================================

import { Employee, STORAGE_KEYS } from '@/types';
import { getData, setData, generateId, getTimestamp } from './storage';

/**
 * Get all employees
 */
export async function getEmployees(): Promise<Employee[]> {
    const employees = await getData<Employee[]>(STORAGE_KEYS.EMPLOYEES);
    return employees || [];
}

/**
 * Get employee by ID
 */
export async function getEmployeeById(id: string): Promise<Employee | null> {
    const employees = await getEmployees();
    return employees.find(e => e.id === id) || null;
}

/**
 * Add new employee
 */
export async function addEmployee(
    employee: Omit<Employee, 'id' | 'createdAt' | 'updatedAt'>
): Promise<Employee> {
    const employees = await getEmployees();
    const newEmployee: Employee = {
        ...employee,
        id: generateId(),
        createdAt: getTimestamp(),
        updatedAt: getTimestamp(),
    };
    employees.push(newEmployee);
    await setData(STORAGE_KEYS.EMPLOYEES, employees);
    return newEmployee;
}

/**
 * Update existing employee
 */
export async function updateEmployee(
    id: string,
    updates: Partial<Omit<Employee, 'id' | 'createdAt'>>
): Promise<Employee | null> {
    const employees = await getEmployees();
    const index = employees.findIndex(e => e.id === id);
    if (index === -1) return null;

    employees[index] = {
        ...employees[index],
        ...updates,
        updatedAt: getTimestamp(),
    };
    await setData(STORAGE_KEYS.EMPLOYEES, employees);
    return employees[index];
}

/**
 * Delete employee
 */
export async function deleteEmployee(id: string): Promise<boolean> {
    const employees = await getEmployees();
    const filtered = employees.filter(e => e.id !== id);
    if (filtered.length === employees.length) return false;
    await setData(STORAGE_KEYS.EMPLOYEES, filtered);
    return true;
}

/**
 * Search employees by name or role
 */
export async function searchEmployees(query: string): Promise<Employee[]> {
    const employees = await getEmployees();
    const lowerQuery = query.toLowerCase();
    return employees.filter(e =>
        e.name.toLowerCase().includes(lowerQuery) ||
        e.role.toLowerCase().includes(lowerQuery) ||
        e.phone.includes(query)
    );
}
