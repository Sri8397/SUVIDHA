import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Invoice, InvoiceItem, ClientDetails, TaxType, CompanyDetails } from '../types';

const COMPANY_KEY = 'company_details';

const getCompanyDetails = async (): Promise<CompanyDetails> => {
    try {
        const jsonValue = await AsyncStorage.getItem(COMPANY_KEY);
        if (jsonValue != null) return JSON.parse(jsonValue);
    } catch (e) { }

    return {
        name: 'Smart Billing App',
        gstin: '',
        address: '',
        phone: ''
    };
}

const generateHtml = (
    company: CompanyDetails,
    client: ClientDetails,
    items: InvoiceItem[],
    subtotal: number,
    taxAmount: number,
    total: number,
    taxType: TaxType
) => {
    const taxLabel = taxType === 'IGST' ? 'IGST (18%)' : 'CGST (9%) + SGST (9%)';

    const itemsRows = items.map((item, index) => `
    <tr>
      <td>${index + 1}</td>
      <td>${item.description}</td>
      <td>${item.quantity}</td>
      <td>₹${item.unitPrice.toFixed(2)}</td>
      <td>${item.taxRate}%</td>
      <td>₹${(item.quantity * item.unitPrice).toFixed(2)}</td>
    </tr>
  `).join('');

    return `
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no" />
        <style>
          body { font-family: 'Helvetica', sans-serif; padding: 20px; color: #333; }
          .header-container { display: flex; justify-content: space-between; margin-bottom: 40px; border-bottom: 2px solid #eee; padding-bottom: 20px;}
          .company-info { text-align: left; }
          .company-name { font-size: 20px; font-weight: bold; color: #0a7ea4; margin-bottom: 5px; }
          .invoice-title { font-size: 32px; font-weight: bold; color: #333; text-align: right; }
          .subtitle { font-size: 14px; color: #666; margin-bottom: 2px; }
          .section { margin-bottom: 20px; }
          .label { font-weight: bold; font-size: 12px; color: #666; margin-bottom: 4px; }
          .value { font-size: 14px; }
          
          table { width: 100%; border-collapse: collapse; margin-top: 20px; }
          th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
          th { background-color: #f8fafc; font-size: 12px; font-weight: bold; color: #475569; }
          td { font-size: 13px; }
          
          .totals { margin-top: 20px; text-align: right; }
          .total-row { display: flex; justify-content: flex-end; margin-bottom: 8px; }
          .total-label { width: 150px; text-align: right; margin-right: 15px; font-weight: bold; color: #64748b; }
          .total-value { width: 120px; text-align: right; font-weight: 500; }
          .grand-total { font-size: 20px; font-weight: bold; color: #0a7ea4; margin-top: 15px; border-top: 1px solid #ddd; padding-top: 10px; }
        </style>
      </head>
      <body>
        <div class="header-container">
            <div class="company-info">
                <div class="company-name">${company.name}</div>
                <div class="subtitle">${company.address}</div>
                <div class="subtitle">GSTIN: ${company.gstin}</div>
                 <div class="subtitle">${company.phone}</div>
            </div>
            <div>
                 <div class="invoice-title">INVOICE</div>
                 <div class="subtitle" style="text-align: right;">Date: ${new Date().toLocaleDateString()}</div>
            </div>
        </div>

        <div class="section">
          <div class="label">BILL TO:</div>
          <div class="value" style="font-weight: bold; font-size: 16px;">${client.name}</div>
          <div class="value">${client.address || ''}</div>
          <div class="value">GSTIN: ${client.gstin || 'N/A'}</div>
        </div>

        <table>
          <thead>
            <tr>
              <th style="width: 30px">#</th>
              <th>Description</th>
              <th style="width: 60px">Qty</th>
              <th style="width: 80px">Price</th>
              <th style="width: 60px">Tax %</th>
              <th style="width: 100px">Total</th>
            </tr>
          </thead>
          <tbody>
            ${itemsRows}
          </tbody>
        </table>

        <div class="totals">
          <div class="total-row">
            <div class="total-label">Subtotal</div>
            <div class="total-value">₹${subtotal.toFixed(2)}</div>
          </div>
          <div class="total-row">
            <div class="total-label">${taxLabel}</div>
            <div class="total-value">₹${taxAmount.toFixed(2)}</div>
          </div>
           <div class="total-row grand-total">
            <div class="total-label">Total Amount</div>
            <div class="total-value">₹${total.toFixed(2)}</div>
          </div>
        </div>
      </body>
    </html>
  `;
};

export const generateAndSharePdf = async (
    client: ClientDetails,
    items: InvoiceItem[],
    subtotal: number,
    taxAmount: number,
    total: number,
    taxType: TaxType
) => {
    try {
        const company = await getCompanyDetails();
        const html = generateHtml(company, client, items, subtotal, taxAmount, total, taxType);
        const { uri } = await Print.printToFileAsync({ html });
        await Sharing.shareAsync(uri, { UTI: '.pdf', mimeType: 'application/pdf' });
    } catch (error) {
        console.error('Error generating PDF:', error);
        throw error;
    }
};
