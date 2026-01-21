import http from './http-common';

import PaginatedList from '../model/paginated-list.model';
import { PortfolioRequest, PortfolioSummaryResponse, PortfolioResponse, TradeRequest, HoldingResponse } from '../model/share-portfolio.model';

class PortfolioService {
    createPortfolio(name: string): Promise<PortfolioSummaryResponse> {
        const request = { name: name } as PortfolioRequest;
        return http.post<PortfolioSummaryResponse>('/shares/portfolios', request)
            .then(response => response.data)
            .then(response => {
                response.dateCreated = new Date(response.dateCreated);
                return response;
            });
    }

    getPortfolios(page: number = 0, pageSize: number = 100): Promise<PaginatedList<PortfolioSummaryResponse>> {
        return http.get<PaginatedList<PortfolioSummaryResponse>>('/shares/portfolios', { params: { "page": page, "page-size": pageSize }})
            .then(response => response.data)
            .then(response => {
                if (response.items) {
                    response.items.forEach(item => item.dateCreated = new Date(item.dateCreated));
                }
                return response;
            });
    }

    getPortfolio(portfolioId: string): Promise<PortfolioResponse> {
        return http.get(`/shares/portfolios/${portfolioId}`)
            .then(response => response.data as PortfolioResponse)
            .then(response => {
                response.dateCreated = new Date(response.dateCreated);
                if (response.holdings) {
                    response.holdings.forEach(holding => {
                        holding.dealings.forEach(dealing => dealing.dateExecuted = new Date(dealing.dateExecuted));
                    });
                }
                return response;
            });
    }

    updatePortfolio(portfolioId: string, name: string): Promise<PortfolioSummaryResponse> {
        const request = { name: name } as PortfolioRequest;
        return http.put(`/shares/portfolios/${portfolioId}`, request)
            .then(response => response.data)
            .then(response => {
                response.dateCreated = new Date(response.dateCreated);
                return response;
            });
    }

    deletePortfolio(portfolioId: string): Promise<any> {
        return http.delete<void>(`/shares/portfolios/${portfolioId}`);
    }

    recordShareTrade(portfolioId: string, request: TradeRequest): Promise<HoldingResponse> {
        return http.post(`/shares/portfolios/${portfolioId}/holdings`, request)
            .then(response => response.data as HoldingResponse)
            .then(response => {
                if (response.dealings) {
                    response.dealings.forEach(dealing => dealing.dateExecuted = new Date(dealing.dateExecuted));
                }
                return response;
            });
    }
}

const instance = new PortfolioService();
export default instance;
