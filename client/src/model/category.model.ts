export interface CategoryGroup {
  id?: string;
  name: string;
  description?: string;
}

export interface Category {
  id?: string;
  groupId?: string;
  name: string;
  description?: string;
  colour?: string;
}

export interface CategorySelector {
  id?: string;
  categoryId: string;
  accountId: string;
  infoContains?: string;
  refContains?: string;
  creditorContains?: string;
}

export interface CategoryStatistics {
  groupId: string;
  groupName: string;
  categoryId?: string;
  categoryName: string;
  description?: string;
  colour: string;
  count: number;
  total: number;
  credit: number;
  debit: number;
}
