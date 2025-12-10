import { calculateInvoiceTotals } from './calculations';
import { InvoiceItem } from '../types';

describe('calculateInvoiceTotals', () => {
    it('should correctly calculate subtotal and tax for CGST_SGST (9+9)', () => {
        const items: InvoiceItem[] = [
            { id: '1', description: 'Item 1', quantity: 2, unitPrice: 100, taxRate: 18 }
        ];
        // Subtotal: 2 * 100 = 200
        // Tax: 200 * 18% = 36
        // Total: 236

        const result = calculateInvoiceTotals(items);
        expect(result.subtotal).toBe(200);
        expect(result.taxAmount).toBe(36);
        expect(result.total).toBe(236);
    });

    it('should correctly calculate totals for multiple items with different rates', () => {
        const items: InvoiceItem[] = [
            { id: '1', description: 'Item 1', quantity: 1, unitPrice: 100, taxRate: 18 },
            { id: '2', description: 'Item 2', quantity: 1, unitPrice: 200, taxRate: 5 }
        ];
        // Item 1: 100 + 18 = 118
        // Item 2: 200 + 10 = 210
        // Total Tax: 28
        // Subtotal: 300
        // Total: 328

        const result = calculateInvoiceTotals(items);
        expect(result.subtotal).toBe(300);
        expect(result.taxAmount).toBe(28);
        expect(result.total).toBe(328);
    });
});
