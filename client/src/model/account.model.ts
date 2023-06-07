import Institution from "./institution.model"

export interface TransactionSummary {
  id: string;
  amount: number;
  currency: string;
  date: Date;
  description: string;
  accountId: string;
}

export interface AccountBalance {
  id: string;
  type: string;
  referenceDate: Date;
  dateRecorded: Date;
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