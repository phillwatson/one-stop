class CurrencyService {
  format(amount: number, currency: string) {
    const formatter = new Intl.NumberFormat('en-EN', {
      style: 'currency',
      currency: currency
    });
    return formatter.format(amount);
  }
}

export enum Currency {
  EUR,
  GBP,
  USD,
}

export type CurrencyStrings = keyof typeof Currency;

const instance = new CurrencyService();
export default instance;
