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

// attempts to create a text identifier from the given selector
export function selectorName(selector: CategorySelector) {
  return selector.infoContains || selector.refContains || selector.creditorContains;
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
