import { Currency } from "./commons.model";
import Institution from "./institution.model"
import PaginatedList from "./paginated-list.model";

export interface TransactionDetail {
  id: string;
  accountId: string;
  transactionId: string; // the rail provider's identity of the transaction
  bookingDateTime: string;
  valueDateTime: string;
  amount: number;
  currency: Currency;
  reference: string;
  additionalInformation: string;
  creditorName: string;
  reconciled: boolean;
  notes?: string;
}

export interface AccountBalance {
  id: string;
  type: string;
  referenceDate: string;
  dateRecorded: string;
  amount: number;
  currency: Currency;
}

export interface AccountDetail {
  id: string;
  name: string;
  ownerName: string;
  currency: Currency;
  iban: string;
  balance: Array<AccountBalance>;
  institution: Institution;
}

export default interface Account {
  id: string;
  name: string;
  ownerName: string;
  currency: Currency;
  iban: string;
  institutionId: string;
  institutionName: string;
}

export type CurrencyTotal = {
  [currency: Currency | string]: number
};

export interface PaginatedTransactions extends PaginatedList<TransactionDetail> {
  currencyTotals?: CurrencyTotal;
}

export interface MovementEntry {
  count: number;
  amount: number;
  currency: Currency;
}

export interface TransactionMovement {
  fromDate: string;
  toDate: string;
  credits: MovementEntry;
  debits: MovementEntry;
}