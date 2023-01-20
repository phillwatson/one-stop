import http from './http-common';
import Bank from '../model/bank.model';

class BankService {
  getAll(countryCode: string = 'GB') {
    console.log(`Retrieving banks for "${countryCode}"`)
    return http.get<Array<Bank>>(`/banks/?country=${countryCode}&payments_enabled=false`);
  }

  get(id: string) {
    console.log(`Retrieving banks details for "${id}"`)
    return http.get<Bank>(`/banks/${id}`);
  }
}

var instance = new BankService();
export default instance;
