// ============================================
// Customer Service - CRUD Operations
// ============================================

import { Customer } from '@/types';
import { getData, setData, generateId, getTimestamp } from './storage';
import { getKeysForCurrentMode } from './modeStorage';

/**
 * Get all customers
 */
export async function getCustomers(): Promise<Customer[]> {
    const keys = getKeysForCurrentMode();
    const customers = await getData<Customer[]>(keys.CUSTOMERS);
    return customers || [];
}

/**
 * Get customer by ID
 */
export async function getCustomerById(id: string): Promise<Customer | null> {
    const customers = await getCustomers();
    return customers.find(c => c.id === id) || null;
}

/**
 * Add new customer
 */
export async function addCustomer(
    customer: Omit<Customer, 'id' | 'outstanding' | 'createdAt' | 'updatedAt'>
): Promise<Customer> {
    const keys = getKeysForCurrentMode();
    const customers = await getCustomers();
    const newCustomer: Customer = {
        ...customer,
        id: generateId(),
        outstanding: 0,
        createdAt: getTimestamp(),
        updatedAt: getTimestamp(),
    };
    customers.push(newCustomer);
    await setData(keys.CUSTOMERS, customers);
    return newCustomer;
}

/**
 * Update existing customer
 */
export async function updateCustomer(
    id: string,
    updates: Partial<Omit<Customer, 'id' | 'createdAt'>>
): Promise<Customer | null> {
    const keys = getKeysForCurrentMode();
    const customers = await getCustomers();
    const index = customers.findIndex(c => c.id === id);
    if (index === -1) return null;

    customers[index] = {
        ...customers[index],
        ...updates,
        updatedAt: getTimestamp(),
    };
    await setData(keys.CUSTOMERS, customers);
    return customers[index];
}

/**
 * Delete customer
 */
export async function deleteCustomer(id: string): Promise<boolean> {
    const keys = getKeysForCurrentMode();
    const customers = await getCustomers();
    const filtered = customers.filter(c => c.id !== id);
    if (filtered.length === customers.length) return false;
    await setData(keys.CUSTOMERS, filtered);
    return true;
}

/**
 * Update customer outstanding balance
 */
export async function updateCustomerOutstanding(
    id: string,
    amountChange: number
): Promise<Customer | null> {
    const customer = await getCustomerById(id);
    if (!customer) return null;

    // Can go negative (if they overpay, they have credit)
    const newOutstanding = customer.outstanding + amountChange;
    return updateCustomer(id, { outstanding: newOutstanding });
}

/**
 * Get customers with outstanding balances
 */
export async function getCustomersWithOutstanding(): Promise<Customer[]> {
    const customers = await getCustomers();
    return customers.filter(c => c.outstanding > 0);
}

/**
 * Get total outstanding amount
 */
export async function getTotalOutstanding(): Promise<number> {
    const customers = await getCustomers();
    return customers.reduce((sum, c) => sum + Math.max(0, c.outstanding), 0);
}

/**
 * Search customers by name or phone
 */
export async function searchCustomers(query: string): Promise<Customer[]> {
    const customers = await getCustomers();
    const lowerQuery = query.toLowerCase();
    return customers.filter(c =>
        c.name.toLowerCase().includes(lowerQuery) ||
        c.phone.includes(query)
    );
}

