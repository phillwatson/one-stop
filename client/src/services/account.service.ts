import http from './http-common';
import Account from '../model/account.model';
import TransactionList from '../model/transaction-list.model';
import AccountBalances from '../model/account-balances.model';

class AccountService {
  get(accountId: string) {
    console.log(`Retrieving account [id: ${accountId}]`);
    return http.get<Array<Account>>(`/rails/rails-accounts/${accountId}`);
  }

  getDetails(accountId: string) {
    console.log(`Retrieving account details [accountId: ${accountId}]`);
    return http.get<Map<string,any>>(`/rails/rails-accounts/${accountId}/details`);
  }

  getBalances(accountId:  string) {
    console.log(`Retrieving balances [accountId: ${accountId}]`);
    return http.get<Array<AccountBalances>>(`/rails/rails-accounts/${accountId}/balances`);
  }

  getTransactions(accountId: string, from: Date, to: Date) {
    console.log(`Retrieving transactions [accountId: ${accountId}, from: ${from}, to: ${to}]`);
    return http.get<TransactionList>(`/rails/rails-accounts/${accountId}/transactions?date_from=${from}&date_to=${to}`);
  }
}

const instance = new AccountService();
export default instance;
