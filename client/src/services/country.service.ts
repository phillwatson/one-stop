import http from './http-common';
import Country from '../model/country.model';

class CountryService {
  getAll() {
    console.log("Retrieving country list")
    return http.get<Array<Country>>(`/rails/countries`);
  }

  get(id: string) {
    console.log(`Retrieving country details for "${id}"`)
    return http.get<Country>(`/rails/country/${id}`);
  }
}

const instance = new CountryService();
export default instance;
