import AsyncStorage from '@react-native-async-storage/async-storage';
import { Invoice } from '../types';

const INVOICES_KEY = 'invoices_data';

export const InvoiceService = {
    getInvoices: async (): Promise<Invoice[]> => {
        try {
            const jsonValue = await AsyncStorage.getItem(INVOICES_KEY);
            return jsonValue != null ? JSON.parse(jsonValue) : [];
        } catch (e) {
            console.error('Failed to fetch invoices', e);
            return [];
        }
    },

    saveInvoice: async (invoice: Invoice): Promise<void> => {
        try {
            const invoices = await InvoiceService.getInvoices();
            const newInvoices = [invoice, ...invoices];
            await AsyncStorage.setItem(INVOICES_KEY, JSON.stringify(newInvoices));
        } catch (e) {
            console.error('Failed to save invoice', e);
            throw e;
        }
    },

    updateInvoiceStatus: async (id: string, status: 'Paid' | 'Pending'): Promise<void> => {
        try {
            const invoices = await InvoiceService.getInvoices();
            const updatedInvoices = invoices.map((inv) =>
                inv.id === id ? { ...inv, status } : inv
            );
            await AsyncStorage.setItem(INVOICES_KEY, JSON.stringify(updatedInvoices));
        } catch (e) {
            console.error('Failed to update invoice', e);
            throw e;
        }
    },

    clearAll: async () => {
        await AsyncStorage.removeItem(INVOICES_KEY);
    }
};
