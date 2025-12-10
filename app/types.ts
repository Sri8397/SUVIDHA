export interface InvoiceItem {
    id: string;
    description: string;
    quantity: number;
    unitPrice: number;
    taxRate: number; // e.g., 18 for 18%
}

export interface ClientDetails {
    name: string;
    gstin: string;
    address: string;
}

export type TaxType = 'CGST_SGST' | 'IGST';

export interface Invoice {
    id: string;
    date: string;
    client: ClientDetails;
    items: InvoiceItem[];
    subtotal: number;
    taxType: TaxType;
    taxAmount: number;
    total: number;
    status: 'Paid' | 'Pending';
}

export interface CompanyDetails {
    name: string;
    gstin: string;
    address: string;
    phone: string;
}
