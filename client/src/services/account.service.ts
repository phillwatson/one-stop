import http from './http-common';
import PaginatedList from '../model/paginated-list.model';
import { AccountDetail, TransactionDetail } from '../model/account.model';

class AccountService {
  getAll(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<AccountDetail>> {
    console.log(`Retrieving account [page: ${page}, pageSize: ${pageSize}]`);
    return http.get<PaginatedList<AccountDetail>>('/rails/accounts', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
    }

  get(accountId: string): Promise<AccountDetail> {
    console.log(`Retrieving account [id: ${accountId}]`);
    return http.get<AccountDetail>(`/rails/accounts/${accountId}`)
      .then(response => response.data);
  }

  getTransactions(accountId: string, page: number = 0, pageSize = 25, filters: any = {}): Promise<PaginatedList<TransactionDetail>> {
    console.log(`Retrieving account transactions [id: ${accountId}, page: ${page}, pageSize: ${pageSize}]`);
    return http.get<PaginatedList<TransactionDetail>>('/rails/transactions',
     { params: { "account-id": accountId, "page": page, "page-size": pageSize, ...filters} })
     .then(response => response.data);
  }

  getTransactionsForDateRange(accountId: string, fromDate: Date, toDate: Date, page: number = 0, pageSize = 25): Promise<PaginatedList<TransactionDetail>> {
    const from = fromDate.toISOString().substring(0, 10);
    const to = toDate.toISOString().substring(0, 10);
    console.log(`Retrieving account transactions [id: ${accountId}, from-date: ${from}, to-date: ${to}, page: ${page}, pageSize: ${pageSize}]`);

    return http.get<PaginatedList<TransactionDetail>>('/rails/transactions',
     { params: { "account-id": accountId, "from-date": from, "to-date": to, "page": page, "page-size": pageSize }})
     .then(response => response.data);
  }

  async fetchTransactions(accountId: string, fromDate: Date, toDate: Date): Promise<Array<TransactionDetail>> {
    var response = await this.getTransactionsForDateRange(accountId, fromDate, toDate, 0, 100);
    var transactions = response.items as Array<TransactionDetail>;
    while (response.links.next) {
      response = await this.getTransactionsForDateRange(accountId, fromDate, toDate, response.page + 1, 100);
      transactions = transactions.concat(response.items);
    }
    return transactions.reverse();
  }
  
}

const instance = new AccountService();
export default instance;
