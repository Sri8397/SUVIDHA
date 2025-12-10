import { InvoiceService } from './invoiceService';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Invoice } from '../types';

// Mock AsyncStorage
jest.mock('@react-native-async-storage/async-storage', () => ({
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
}));

describe('InvoiceService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('getInvoices should return empty list if storage is null', async () => {
        (AsyncStorage.getItem as jest.Mock).mockResolvedValue(null);
        const invoices = await InvoiceService.getInvoices();
        expect(invoices).toEqual([]);
    });

    it('saveInvoice should add invoice to list', async () => {
        (AsyncStorage.getItem as jest.Mock).mockResolvedValue(JSON.stringify([]));

        const newInvoice = { id: '123', total: 100 } as Invoice;
        await InvoiceService.saveInvoice(newInvoice);

        expect(AsyncStorage.setItem).toHaveBeenCalledWith(
            'invoices_data',
            JSON.stringify([newInvoice])
        );
    });
});
