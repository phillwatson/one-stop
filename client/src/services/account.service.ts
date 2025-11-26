import http from './http-common';
import PaginatedList from '../model/paginated-list.model';
import { AccountDetail, TransactionDetail, PaginatedTransactions, TransactionMovement } from '../model/account.model';

class AccountService {
  getAll(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<AccountDetail>> {
    return http.get<PaginatedList<AccountDetail>>('/rails/accounts', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAll(): Promise<Array<AccountDetail>> {
    var response = await this.getAll(0, 100);
    var accounts = response.items as Array<AccountDetail>;
    while (response.links.next) {
      response = await this.getAll(response.page + 1, 100);
      accounts = accounts.concat(response.items);
    }
    return accounts;
  }

  get(accountId: string): Promise<AccountDetail> {
    return http.get<AccountDetail>(`/rails/accounts/${accountId}`)
      .then(response => response.data);
  }

  getTransactions(accountId: string, page: number = 0, pageSize = 25, filters: any = {}): Promise<PaginatedTransactions> {
    return http.get<PaginatedTransactions>('/rails/transactions',
     { params: { "account-id": accountId, "page": page, "page-size": pageSize, ...filters} })
     .then(response => response.data);
  }

  getTransactionsForDateRange(accountId: string, fromDate: Date, toDate: Date, page: number = 0, pageSize = 25): Promise<PaginatedTransactions> {
    const fromDateStr = fromDate.toISOString().substring(0, 10);
    const toDateStr = toDate.toISOString().substring(0, 10);

    return http.get<PaginatedTransactions>('/rails/transactions',
     { params: { "account-id": accountId, "from-date": fromDateStr, "to-date": toDateStr, "page": page, "page-size": pageSize }})
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

  getTransactionsByCategory(groupId: string, categoryId: string | undefined, fromDate: Date, toDate: Date): Promise<Array<TransactionDetail>> {
    const fromDateStr = fromDate.toISOString().substring(0, 10);
    const toDateStr = toDate.toISOString().substring(0, 10);

    return http.get<Array<TransactionDetail>>(`/rails/transactions/${groupId}/category`,
      { params: { "category-id": categoryId, "from-date": fromDateStr, "to-date": toDateStr }})
      .then(response => response.data);
  }

  getTransactionMovements(accountId: string, fromDate: Date, toDate: Date): Promise<Array<TransactionMovement>> {
    const fromDateStr = fromDate.toISOString().substring(0, 10);
    const toDateStr = toDate.toISOString().substring(0, 10);

    return http.get<Array<TransactionMovement>>('/rails/transactions/movements',
      { params: { "account-id": accountId, "from-date": fromDateStr, "to-date": toDateStr }})
      .then(response => response.data);
  }

  updateTransaction(transactionId: string, reconciled?: boolean, notes?: string): Promise<TransactionDetail> {
    return http.put<TransactionDetail>(`/rails/transactions/${transactionId}`, 
      {
        reconciled: reconciled,
        notes: notes
      })
      .then(response => response.data);
  }

  batchReconciliationUpdate(reconciliations: Map<string, boolean>): Promise<void> {
    const updates = Array.from(reconciliations.entries()).map(([transactionId, reconciled]) => ({
      transactionId: transactionId,
      reconciled: reconciled
    }));

    return http.put<void>('/rails/transactions/reconciliations', updates)
      .then(response => response.data);
  }
}

const instance = new AccountService();
export default instance;
