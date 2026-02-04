import { Currency } from "./commons.model";

export interface ShareId {
  // The unique International Securities Identification Number (GB00B80QG052)
  isin: string;

  // The share's ticker symbol
  tickerSymbol: string;
}

export interface ShareIndexResponse {
  id: string;
  shareId: string;
  name: string;
  currency: Currency;
  provider: string;
}

export const NULL_INDEX: ShareIndexResponse = {
  id: '',
  shareId: '',
  name: '',
  currency: 'GBP',
  provider: ''
};

export interface HistoricalPriceResponse {
  date: Date;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}
