import http from './http-common';

import PaginatedList from '../model/paginated-list.model';
import { PortfolioRequest, PortfolioResponse, ShareTradeSummary, ShareTrade } from '../model/share-portfolio.model';
import { toISODate } from '../util/date-util';

class PortfolioService {
    createPortfolio(name: string): Promise<PortfolioResponse> {
        const request = { name: name } as PortfolioRequest;
        return http.post<PortfolioResponse>('/shares/portfolios', request)
            .then(response => response.data)
            .then(response => {
                response.dateCreated = new Date(response.dateCreated);
                return response;
            });
    }

    getPortfolios(page: number = 0, pageSize: number = 100): Promise<PaginatedList<PortfolioResponse>> {
        return http.get<PaginatedList<PortfolioResponse>>('/shares/portfolios', { params: { "page": page, "page-size": pageSize }})
            .then(response => response.data)
            .then(portfolios => {
                if (portfolios.items) {
                    portfolios.items.forEach(item => item.dateCreated = new Date(item.dateCreated));
                }
                return portfolios;
            });
    }

    getPortfolio(portfolioId: string): Promise<PortfolioResponse> {
        return http.get(`/shares/portfolios/${portfolioId}`)
            .then(response => response.data as PortfolioResponse)
            .then(portfolio => {
                portfolio.dateCreated = new Date(portfolio.dateCreated);
                return portfolio;
            });
    }

    updatePortfolio(portfolioId: string, name: string): Promise<PortfolioResponse> {
        const request = { name: name } as PortfolioRequest;
        return http.put(`/shares/portfolios/${portfolioId}`, request)
            .then(response => response.data)
            .then(portfolio => {
                portfolio.dateCreated = new Date(portfolio.dateCreated);
                return portfolio;
            });
    }

    deletePortfolio(portfolioId: string): Promise<any> {
        return http.delete<void>(`/shares/portfolios/${portfolioId}`);
    }

    recordShareTrade(portfolioId: string, shareIndexId: string, dateExecuted: Date, quantity: number, price: number): Promise<ShareTrade> {
        const request = {
            shareIndexId: shareIndexId,
            dateExecuted: toISODate(dateExecuted),
            quantity: quantity,
            pricePerShare: price
        };
        return http.post(`/shares/portfolios/${portfolioId}/trades`, request)
            .then(response => response.data as ShareTrade)
            .then(response => {
                response.dateExecuted = new Date(response.dateExecuted);
                return response;
            });
    }

    getPortfolioHoldings(portfolioId: string): Promise<ShareTradeSummary[]> {
        return http.get(`/shares/portfolios/${portfolioId}/trades`)
            .then(response => response.data as ShareTradeSummary[])
            .then(holdings => {
                holdings.forEach(holding => {
                    holding.averagePrice = holding.quantity > 0 ? holding.totalCost / holding.quantity : 0;
                    holding.currentValue = holding.latestPrice * holding.quantity;
                    holding.gainLoss = holding.currentValue - holding.totalCost;
                    holding.gainLossPercent = holding.totalCost > 0 ? ((holding.gainLoss / holding.totalCost) * 100) : 0;
                });
                return holdings;
            });
    }

    getShareTrades(portfolioId: string, shareIndexId: string, page: number = 0, pageSize: number = 100): Promise<PaginatedList<ShareTrade>> {
        return http.get<PaginatedList<ShareTrade>>(`/shares/portfolios/${portfolioId}/trades/${shareIndexId}`, { params: { "page": page, "page-size": pageSize }})
            .then(response => response.data)
            .then(trades => {
                if (trades.items) {
                    trades.items.forEach(trade => {
                      trade.dateExecuted = new Date(trade.dateExecuted);
                      trade.totalCost = trade.quantity * trade.pricePerShare;
                    });
                }
                return trades;
            });
    }


    async fetchShareTrades(portfolioId: string, shareIndexId: string): Promise<Array<ShareTrade>> {
        var response = await this.getShareTrades(portfolioId, shareIndexId, 0, 100);
        var trades = response.items as Array<ShareTrade>;
        while (response.links.next) {
        response = await this.getShareTrades(portfolioId, shareIndexId, response.page + 1, 100);
        trades = trades.concat(response.items);
        }
        return trades;
    }

    getShareTrade(tradeId: string): Promise<ShareTrade> {
        return http.get(`/shares/trades/${tradeId}`)
            .then(response => response.data as ShareTrade)
            .then(trade => {
                trade.dateExecuted = new Date(trade.dateExecuted);
                return trade;
            });
    }

    updateShareTrade(tradeId: string, shareIndexId: string, dateExecuted: Date, quantity: number, price: number): Promise<ShareTrade> {
        const request = {
            shareIndexId: shareIndexId,
            dateExecuted: toISODate(dateExecuted),
            quantity: quantity,
            pricePerShare: price
        };
        return http.put(`/shares/trades/${tradeId}`, request)
            .then(response => response.data as ShareTrade)
            .then(trade => {
                trade.dateExecuted = new Date(trade.dateExecuted);
                return trade;
            });
    }

    deleteShareTrade(tradeId: string): Promise<any> {
        return http.delete(`/shares/trades/${tradeId}`);
    }
}

const instance = new PortfolioService();
export default instance;
