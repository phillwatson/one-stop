import http from './http-common';
import Country from '../model/country.model';
import PaginatedList from '../model/paginated-list.model';

class CountryService {
  getAll(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<Country>> {
    console.log("Retrieving country list")
    return http.get<PaginatedList<Country>>('/rails/countries', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  get(id: string): Promise<Country> {
    console.log(`Retrieving country details for "${id}"`)
    return http.get<Country>(`/rails/country/${id}`)
      .then(response => response.data);
  }
}

const instance = new CountryService();
export default instance;
