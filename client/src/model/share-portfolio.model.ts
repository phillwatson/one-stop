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
export interface PortfolioSummaryResponse {
    // The portfolio's internal Id
    id: string;

    // The user's chosen name of the portfolio
    name: string;

    // The date and time the portfolio was created
    dateCreated: Date;
}

/**
 * Describes a user's share portfolio and lists the share holding within it.
 */
export interface PortfolioResponse extends PortfolioSummaryResponse {
    // The list of the user's portfolios
    holdings: Array<HoldingResponse>;
}

/**
 * Summarizes a share holding within a user's portfolio.
 */
export interface HoldingResponse {
    // The unique ID of the holding record
    id: string;

    // The unique ID of the share index record
    shareIndexId: string;

    // The share stock identifier
    shareId: ShareId;

    // The name of the share
    name: string;

    // The number of shares held in the portfolio
    quantity: number;

    // The total price paid for the holding in minor currency units
    totalCost: string;

    // The latest market value of the holding in minor currency units
    latestValue: string;

    // The ISO-4217 currency code in which the shares are traded.
    currency: Currency;

    // The list of all dealings for the holding
    dealings: Array<DealingHistoryResponse>;
}

/**
 * Details the trade of a share holding within a user's portfolio
 */
export interface DealingHistoryResponse {
    // The dealing Id
    id: string;

    // The date and time the dealing was executed
    dateExecuted: Date;

    // The number of shares dealt. Negative for SELL, positive for BUY.
    quantity: number;
 
    // The price per share at which the dealing was executed, in minor currency units
    pricePerShare: number;
}

/**
 * Describes a request for a share trade to be recorded within a user's portfolio.
 * The request parameters will identify the user's portfolio.
 */
export interface TradeRequest {
    // The share index being traded.
    shareId: ShareId;

    // The date on which the trade occurred
    dateExecuted: Date;

    // The number of shares traded.
    quantity: number;

    // The price per share at which the trade was executed, in minor currency units
    pricePerShare: number;
}
