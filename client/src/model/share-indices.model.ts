import { Currency } from "./commons.model";

export interface ShareId {
  // The unique International Securities Identification Number (e.g. GB00B80QG052)
  isin?: string;

  // The share's ticker symbol
  tickerSymbol?: string;
}

export interface ShareIndex {
  id: string;
  shareId: ShareId;
  name: string;
  currency: Currency;
  provider: string;
}

export const NULL_INDEX: ShareIndex = {
  id: '',
  shareId: { },
  name: '',
  currency: 'GBP',
  provider: ''
};

export interface SharePrice {
  date: Date;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

/**
 * Used to register a share index for future access to it's price history.
 * Either the share's ISIN or ticker symbol can be used.
 */
export interface RegisterShareIndexRequest {
  // The unique International Securities Identification Number (e.g. GB00B80QG052)
  isin?: string;

  // The share's ticker symbol
  tickerSymbol?: string;
}
