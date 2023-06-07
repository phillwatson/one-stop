import http from './http-common';
import PaginatedList from '../model/paginated-list.model';
import { AccountDetail, TransactionSummary } from '../model/account.model';

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

  getTransactions(accountId: string, page: number = 0, pageSize = 25): Promise<PaginatedList<TransactionSummary>> {
    console.log(`Retrieving account transactions [id: ${accountId}, page: ${page}, pageSize: ${pageSize}]`);
    return http.get<PaginatedList<TransactionSummary>>('/rails/transactions', { params: { "account-id": accountId, "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }
}

const instance = new AccountService();
export default instance;
