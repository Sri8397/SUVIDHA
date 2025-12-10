import { InvoiceItem } from '@/types';

export const calculateInvoiceTotals = (items: InvoiceItem[]) => {
    const subtotal = items.reduce((acc, item) => acc + item.quantity * item.unitPrice, 0);

    const taxAmount = items.reduce((acc, item) => {
        return acc + (item.quantity * item.unitPrice * item.taxRate) / 100;
    }, 0);

    return {
        subtotal,
        taxAmount,
        total: subtotal + taxAmount
    };
};
