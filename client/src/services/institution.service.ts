import http from './http-common';
import Institution from '../model/institution.model';
import PaginatedList from '../model/paginated-list.model';

class InstitutionService {
  getAll(countryCode: string = 'GB', page: number = 0, pageSize: number = 1000): Promise<PaginatedList<Institution>> {
    console.log(`Retrieving banks for "${countryCode}"`);
    return http.get<PaginatedList<Institution>>('/rails/institutions', { params: { "country": countryCode, "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  get(id: string): Promise<Institution> {
    console.log(`Retrieving banks details for "${id}"`);
    return http.get<Institution>(`/rails/institutions/${id}`)
      .then(response => response.data);
  }
}

const instance = new InstitutionService();
export default instance;
