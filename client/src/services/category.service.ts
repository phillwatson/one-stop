import http from './http-common';
import PaginatedList from '../model/paginated-list.model';
import { CategoryGroup, Category, CategorySelector, CategoryStatistics } from '../model/category.model';

class CategoryService {
  getGroupsPage(page: number = 0, pageSize: number = 20): Promise<PaginatedList<CategoryGroup>> {
    console.log(`Retrieving groups [page: ${page}, pageSize: ${pageSize}]`);
    return http.get<PaginatedList<CategoryGroup>>('/rails/category-groups', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllGroups(): Promise<Array<CategoryGroup>> {
    console.log('Retrieving ALL category groups');
    var response = await this.getGroupsPage(0, 100);
    var groups = response.items as Array<CategoryGroup>;
    while (response.links.next) {
      response = await this.getGroupsPage(response.page + 1, 100);
      groups = groups.concat(response.items);
    }
    return groups;
  }

  getGroup(groupId: string): Promise<CategoryGroup> {
    console.log(`Retrieving group [id: ${groupId}]`);
    return http.get<CategoryGroup>(`/rails/category-groups/${groupId}`)
      .then(response => response.data);
  }

  createGroup(group: CategoryGroup): Promise<CategoryGroup> {
    console.log(`Creating group [name: ${group.name}]`);
    return http.post<CategoryGroup>('/rails/category-groups', group)
      .then(response => response.data);
  }

  updateGroup(group: CategoryGroup): Promise<CategoryGroup> {
    console.log(`Updating group [id: ${group.id}, name: ${group.name}]`);
    return http.put<CategoryGroup>(`/rails/category-groups/${group.id}`, group)
      .then(response => response.data);
  }

  deleteGroup(groupId: string): Promise<any> {
    console.log(`Deleting group [id: ${groupId}]`);
    return http.delete<void>(`/rails/category-groups/${groupId}`);
  }

  getCategoriesPage(groupId: string, page: number = 0, pageSize: number = 20): Promise<PaginatedList<Category>> {
    console.log(`Retrieving categories [group: ${groupId}, page: ${page}, pageSize: ${pageSize}]`);
    return http.get<PaginatedList<Category>>(`/rails/category-groups/${groupId}/categories`, { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllCategories(groupId: string): Promise<Array<Category>> {
    console.log('Retrieving ALL categories');
    var response = await this.getCategoriesPage(groupId, 0, 100);
    var categories = response.items as Array<Category>;
    while (response.links.next) {
      response = await this.getCategoriesPage(groupId, response.page + 1, 100);
      categories = categories.concat(response.items);
    }
    return categories;
  }

  getCategory(categoryId: string): Promise<Category> {
    console.log(`Retrieving category [id: ${categoryId}]`);
    return http.get<Category>(`/rails/categories/${categoryId}`)
      .then(response => response.data);
  }

  createCategory(groupId: string, category: Category): Promise<Category> {
    console.log(`Creating category [groupId: ${groupId}, name: ${category.name}]`);
    return http.post<Category>(`/rails/category-groups/${groupId}/categories`, category)
      .then(response => response.data);
  }

  updateCategory(category: Category): Promise<Category> {
    console.log(`Updating category [id: ${category.id}, name: ${category.name}]`);
    return http.put<Category>(`/rails/categories/${category.id}`, category)
      .then(response => response.data);
  }

  deleteCategory(categoryId: string): Promise<any> {
    console.log(`Deleting category [id: ${categoryId}]`);
    return http.delete<void>(`/rails/categories/${categoryId}`);
  }

  getCategorySelectors(categoryId: string, accountId: string): Promise<Array<CategorySelector>> {
    console.log(`Retrieving category selectors [accountId: ${accountId}, categoryId: ${categoryId}]`);
    return http.get<Array<CategorySelector>>(`/rails/categories/${categoryId}/selectors/${accountId}`)
      .then(response => response.data);
  }

  setCategorySelectors(categoryId: string, accountId: string,
                       selectors: Array<CategorySelector>): Promise<Array<CategorySelector>> {
    console.log(`Setting category selectors [accountId: ${accountId}, categoryId: ${categoryId}]`);
    return http.put<Array<CategorySelector>>(`/rails/categories/${categoryId}/selectors/${accountId}`, selectors)
      .then(response => response.data);
  }

  getCategoryStatistics(groupId: string, fromDate: Date, toDate: Date): Promise<Array<CategoryStatistics>> {
    const fromDateStr = fromDate.toISOString().substring(0, 10);
    const toDateStr = toDate.toISOString().substring(0, 10);
    console.log(`Retrieving category statistics [group: ${groupId}, from: ${fromDateStr}, to: ${toDateStr}]`);
    return http.get<Array<CategoryStatistics>>(`/rails/category-groups/${groupId}/statistics`,
      { params: { "from-date": fromDateStr, "to-date": toDateStr }})
      .then(response => response.data);
  }
}

const instance = new CategoryService();
export default instance;
