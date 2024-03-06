import Institution from "./institution.model"

export interface TransactionSummary {
  id: string;
  accountId: string;
  date: string;
  description: string;
  amount: number;
  currency: string;
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