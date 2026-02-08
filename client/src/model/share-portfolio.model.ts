import { Currency } from "./commons.model";
import { ShareId } from "./share-indices.model";

/**
 * Provides the properties for a user's new portfolio.
 */
export interface PortfolioRequest {
  // The user's chosen name for the portfolio
  name: string;
}

/**
 * Describes a user's share portfolio
 */
export interface PortfolioResponse {
  // The portfolio's internal Id
  id: string;

  // The user's chosen name of the portfolio
  name: string;

  // The date and time the portfolio was created
  dateCreated: Date;
}

/**
 * A summary of a portfolio's holdings in a particular share.
 */
export interface ShareTradeSummary {
  // The portfolio's internal Id
  portfolioId: string;

  // The internal share index ID.
  shareindexId: string;

  // The share stock identifier
  shareId: ShareId;

  // The share's name
  name: string;

  // The share's trading currency
  currency: Currency;

  // The total number of shares held.
  quantity: number;

  // The total price paid for the holding in minor currency units.
  totalCost: number;

  // The latest closing price of the share, in minor currency units.
  latestPrice: number;

  averagePrice: number;
  currentValue: number;
  gainLoss: number;
  gainLossPercent: number;
}

/**
 * Summarizes a share holding within a user's portfolio.
 */
export interface ShareTradeResponse {
  // The unique ID of the holding record
  id: string;

  // The internal share index ID.
  shareindexId: string;

  // The date on which the trade occurred
  dateExecuted: Date;

  // The number of shares traded.
  quantity: number;

  // The price per share at which the trade was executed, in minor currency units
  pricePerShare: number;
}
