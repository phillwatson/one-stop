import Institution from "./institution.model"
import PaginatedList from "./paginated-list.model";

export interface TransactionDetail {
  id: string;
  accountId: string;
  transactionId: string;
  bookingDateTime: string;
  valueDateTime: string;
  amount: number;
  currency: string;
  reference: string;
  additionalInformation: string;
  creditorName: string;
}

export interface AccountBalance {
  id: string;
  type: string;
  referenceDate: string;
  dateRecorded: string;
  amount: number;
  currency: string;
}

export interface AccountDetail {
  id: string;
  name: string;
  ownerName: string;
  currency: string;
  iban: string;
  balance: Array<AccountBalance>;
  institution: Institution;
}

export default interface Account {
  id: string;
  name: string;
  ownerName: string;
  currency: string;
  iban: string;
  institutionId: string;
  institutionName: string;
}

export type CurrencyTotal = {
  [currency: string]: number
};

export interface PaginatedTransactions extends PaginatedList<TransactionDetail> {
  currencyTotals: CurrencyTotal;
}

export const EMPTY_PAGINATED_TRANSACTIONS: PaginatedTransactions = {
  total: 0,
  totalPages: 0,
  count: 0,
  page: 0,
  pageSize: 0,
  links: {
    first: '',
    last: ''
  },
  items: [],
  currencyTotals: {}
};
