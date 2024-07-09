export interface Category {
  id?: string;
  name: string;
  description?: string;
  colour?: string;
}

export interface CategorySelector {
  infoContains?: string;
  refContains?: string;
  creditorContains?: string;
}

export interface CategoryStatistics {
  category: string;
  categoryId?: string
  count: number;
  total: number;
}
