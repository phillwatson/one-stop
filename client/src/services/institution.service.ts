import http from './http-common';
import Institution from '../model/institution.model';
import PaginatedList from '../model/paginated-list.model';

class InstitutionService {
  getAll(countryCode: string = 'GB', page: number = 0, pageSize: number = 1000): Promise<PaginatedList<Institution>> {
    return http.get<PaginatedList<Institution>>('/rails/institutions', { params: { "country": countryCode, "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  get(id: string): Promise<Institution> {
    return http.get<Institution>(`/rails/institutions/${id}`)
      .then(response => response.data);
  }
}

const instance = new InstitutionService();
export default instance;
