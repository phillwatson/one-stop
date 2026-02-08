import http from './http-common';

import PaginatedList from '../model/paginated-list.model';
import { HistoricalPriceResponse, ShareIndex } from '../model/share-indices.model';
import { minDate, startOfMonth } from "../util/date-util";

class ShareService {
  getIndices(page: number = 0, pageSize: number = 100): Promise<PaginatedList<ShareIndex>> {
    return http.get<PaginatedList<ShareIndex>>('/shares/indices', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllIndices(): Promise<Array<ShareIndex>> {
    var response = await this.getIndices(0, 100);
    var indices = response.items as Array<ShareIndex>;
    while (response.links.next) {
      response = await this.getIndices(response.page + 1, 100);
      indices = indices.concat(response.items);
    }
    return indices;
  }

  /**
   * Retrieves the prices for the identified share index over the given date
   * range.
   * 
   * @param shareIndexId the share index's identifier.
   * @param fromDate the start of the date range (inclusive).
   * @param toDate the end of the date range (exclusive).
   * @param page the zero-based page index.
   * @param pageSize the maximum number of entries per page.
   * @returns the selected page of prices, in ascending date order.
   */
  getIndexPrices(shareId: string, fromDate: Date, toDate: Date,
                 page: number = 0, pageSize: number = 100): Promise<PaginatedList<HistoricalPriceResponse>> {
    const fromDateStr = fromDate.toISOString().substring(0, 10);
    const toDateStr = toDate.toISOString().substring(0, 10);

    return http.get<PaginatedList<HistoricalPriceResponse>>(`/shares/indices/${shareId}/prices`,
      { params: { "from-date": fromDateStr, "to-date": toDateStr, "page": page, "page-size": pageSize }})
      .then(response => response.data)
      .then(response => {
        response.items.forEach(item => item.date = new Date(item.date));
        return response;
      });
  }

  /**
   * Retrieves the prices for the identified share index over the given date
   * range.
   * 
   * The prices will be retrieved in chunks of 1 month, and the results combined
   * and filtered to the given date range. This makes use of the browser's cache.
   * 
   * @param shareIndexId the share index's identifier.
   * @param fromDate the start of the date range (inclusive).
   * @param toDate the end of the date range (exclusive).
   * @returns the array of prices, in ascending date order.
   */
  getPrices(shareIndexId: string, fromDate: Date, toDate: Date): Promise<Array<HistoricalPriceResponse>> {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    // retrieve index prices in monthly chunks
    const requests = [];
    let startDate = startOfMonth(fromDate);
    const endDate = minDate(toDate, tomorrow);
    while (startDate < endDate) {
      const startNextMonth = startOfMonth(startDate, 1);
      const periodEnd = minDate(startNextMonth, tomorrow);

      // retrieve chunk and ensure it doesn't exceed overall range
      requests.push(
        this.getIndexPrices(shareIndexId, startDate, periodEnd, 0, 100)
          .then(response => response.items)
          .then(responses => responses.filter(p => p.date >= fromDate && p.date < toDate))
      );

      startDate = startNextMonth;
    }

    // join all results, in correct order
    return Promise.all(requests)
      .then(responses => responses.filter(r => r.length > 0))
      .then(responses => responses.toSorted((a, b) => a[0].date.getTime() - b[0].date.getTime()))
      .then(responses => responses.reduce((all, current) => all.concat(current)))
      .catch(err => {
        throw err;
      });
  }
}

const instance = new ShareService();
export default instance;
