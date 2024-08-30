import http from './http-common';
import PaginatedList from '../model/paginated-list.model';
import { CategoryGroup, Category, CategorySelector, CategoryStatistics } from '../model/category.model';

class CategoryService {
  getGroupsPage(page: number = 0, pageSize: number = 20): Promise<PaginatedList<CategoryGroup>> {
    return http.get<PaginatedList<CategoryGroup>>('/rails/category-groups', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllGroups(): Promise<Array<CategoryGroup>> {
    var response = await this.getGroupsPage(0, 100);
    var groups = response.items as Array<CategoryGroup>;
    while (response.links.next) {
      response = await this.getGroupsPage(response.page + 1, 100);
      groups = groups.concat(response.items);
    }
    return groups;
  }

  getGroup(groupId: string): Promise<CategoryGroup> {
    return http.get<CategoryGroup>(`/rails/category-groups/${groupId}`)
      .then(response => response.data);
  }

  createGroup(group: CategoryGroup): Promise<CategoryGroup> {
    return http.post<CategoryGroup>('/rails/category-groups', group)
      .then(response => response.data);
  }

  updateGroup(group: CategoryGroup): Promise<CategoryGroup> {
    return http.put<CategoryGroup>(`/rails/category-groups/${group.id}`, group)
      .then(response => response.data);
  }

  deleteGroup(groupId: string): Promise<any> {
    return http.delete<void>(`/rails/category-groups/${groupId}`);
  }

  getCategoriesPage(groupId: string, page: number = 0, pageSize: number = 20): Promise<PaginatedList<Category>> {
    return http.get<PaginatedList<Category>>(`/rails/category-groups/${groupId}/categories`, { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllCategories(groupId: string): Promise<Array<Category>> {
    var response = await this.getCategoriesPage(groupId, 0, 100);
    var categories = response.items as Array<Category>;
    while (response.links.next) {
      response = await this.getCategoriesPage(groupId, response.page + 1, 100);
      categories = categories.concat(response.items);
    }
    return categories;
  }

  getCategory(categoryId: string): Promise<Category> {
    return http.get<Category>(`/rails/categories/${categoryId}`)
      .then(response => response.data);
  }

  createCategory(groupId: string, category: Category): Promise<Category> {
    return http.post<Category>(`/rails/category-groups/${groupId}/categories`, category)
      .then(response => response.data);
  }

  updateCategory(category: Category): Promise<Category> {
    return http.put<Category>(`/rails/categories/${category.id}`, category)
      .then(response => response.data);
  }

  deleteCategory(categoryId: string): Promise<any> {
    return http.delete<void>(`/rails/categories/${categoryId}`);
  }

  getCategorySelectors(categoryId: string, accountId: string): Promise<Array<CategorySelector>> {
    return http.get<Array<CategorySelector>>(`/rails/categories/${categoryId}/selectors/${accountId}`)
      .then(response => response.data);
  }

  setCategorySelectors(categoryId: string, accountId: string,
                       selectors: Array<CategorySelector>): Promise<Array<CategorySelector>> {
    return http.put<Array<CategorySelector>>(`/rails/categories/${categoryId}/selectors/${accountId}`, selectors)
      .then(response => response.data);
  }

  getCategoryStatistics(groupId: string, fromDate: Date, toDate: Date): Promise<Array<CategoryStatistics>> {
    const fromDateStr = fromDate.toISOString().substring(0, 10);
    const toDateStr = toDate.toISOString().substring(0, 10);
    return http.get<Array<CategoryStatistics>>(`/rails/category-groups/${groupId}/statistics`,
      { params: { "from-date": fromDateStr, "to-date": toDateStr }})
      .then(response => response.data);
  }
}

const instance = new CategoryService();
export default instance;
