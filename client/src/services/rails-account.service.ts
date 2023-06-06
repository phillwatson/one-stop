import http from './http-common';
import RailsTransactionList from '../model/rails-transaction.model';
import RailsBalance from '../model/rails-balances.model';
import RailsAccount from '../model/rails-account.model';

class RailsAccountService {
  get(accountId: string) {
    console.log(`Retrieving account [id: ${accountId}]`);
    return http.get<Array<RailsAccount>>(`/rails/rails-accounts/${accountId}`);
  }

  getDetails(accountId: string) {
    console.log(`Retrieving account details [accountId: ${accountId}]`);
    return http.get<Map<string,any>>(`/rails/rails-accounts/${accountId}/details`);
  }

  getBalances(accountId:  string) {
    console.log(`Retrieving balances [accountId: ${accountId}]`);
    return http.get<Array<RailsBalance>>(`/rails/rails-accounts/${accountId}/balances`);
  }

  getTransactions(accountId: string, from: Date, to: Date) {
    console.log(`Retrieving transactions [accountId: ${accountId}, from: ${from}, to: ${to}]`);
    return http.get<RailsTransactionList>(`/rails/rails-accounts/${accountId}/transactions?date_from=${from}&date_to=${to}`);
  }
}

const instance = new RailsAccountService();
export default instance;
