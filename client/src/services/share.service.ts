import http from './http-common';

import PaginatedList from '../model/paginated-list.model';
import { HistoricalPriceResponse, ShareIndexResponse } from '../model/share-indices.model';


class ShareService {
  getIndices(page: number = 0, pageSize: number = 100): Promise<PaginatedList<ShareIndexResponse>> {
    return http.get<PaginatedList<ShareIndexResponse>>('/shares/indices', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllIndices(): Promise<Array<ShareIndexResponse>> {
    var response = await this.getIndices(0, 100);
    var indices = response.items as Array<ShareIndexResponse>;
    while (response.links.next) {
      response = await this.getIndices(response.page + 1, 100);
      indices = indices.concat(response.items);
    }
    return indices;
  }

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
}

const instance = new ShareService();
export default instance;
