import http from './http-common';
import Country from '../model/country.model';

class CountryService {
  getAll(): Promise<Array<Country>> {
    console.log("Retrieving country list")
    return http.get<Array<Country>>('/rails/countries')
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
