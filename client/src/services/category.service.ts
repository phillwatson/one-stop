import http from './http-common';
import PaginatedList from '../model/paginated-list.model';
import { CategoryGroup, Category, CategorySelector, CategoryStatistics } from '../model/category.model';

class CategoryService {
  /**
   * Returns a page of the authenticate user's category groups. The
   * groups are ordered on their name, in ascending order.
   * @param page the zero-based index of the page to be returned.
   * @param pageSize the maximum number of category groups per page.
   * @returns the identified page of the user's category groups, in
   *  ascending name order.
   */
  getGroupsPage(page: number = 0, pageSize: number = 20): Promise<PaginatedList<CategoryGroup>> {
    return http.get<PaginatedList<CategoryGroup>>('/rails/category-groups', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  /**
   * Returns all the configured category groups for the authenticated user.
   * The groups are ordered on their name, in ascending order.
   * @returns the user's category groups, in ascending name order. 
   */
  async fetchAllGroups(): Promise<Array<CategoryGroup>> {
    var response = await this.getGroupsPage(0, 100);
    var groups = response.items as Array<CategoryGroup>;
    while (response.links.next) {
      response = await this.getGroupsPage(response.page + 1, 100);
      groups = groups.concat(response.items);
    }
    return groups;
  }

  /**
   * Returns the identified category group, but not its categories.
   * @param groupId the category group's unique identifier.
   * @returns the identified category group.
   */
  getGroup(groupId: string): Promise<CategoryGroup> {
    return http.get<CategoryGroup>(`/rails/category-groups/${groupId}`)
      .then(response => response.data);
  }

  /**
   * Creates a new category group for the authenticated user.
   * @param group the details of the new category group.
   * @returns the newly created category group.
   */
  createGroup(group: CategoryGroup): Promise<CategoryGroup> {
    return http.post<CategoryGroup>('/rails/category-groups', group)
      .then(response => response.data);
  }

  /**
   * Modifies the mutable properties of the identified group.
   * @param group the properties of the category group to be updated,
   * including its identifier.
   * @returns the modified category group.
   */
  updateGroup(group: CategoryGroup): Promise<CategoryGroup> {
    return http.put<CategoryGroup>(`/rails/category-groups/${group.id}`, group)
      .then(response => response.data);
  }

  /**
   * Deletes the authenticated user's identified category group. All categories
   * and category selectors within the group will also be deleted.
   * @param groupId the category group's unique identifier.
   * @returns an empty response.
   */
  deleteGroup(groupId: string): Promise<any> {
    return http.delete<void>(`/rails/category-groups/${groupId}`);
  }

  /**
   * Returns a page of the categories contained within the identified
   * category group. The categories are ordered on their name, in ascending
   * order.
   * @param groupId the group identifier.
   * @param page the zero-based index of the page to be returned.
   * @param pageSize the maximum number of category groups per page.
   * @returns the identified page of the identified group's categories.
   */
  getCategoriesPage(groupId: string, page: number = 0, pageSize: number = 20): Promise<PaginatedList<Category>> {
    return http.get<PaginatedList<Category>>(`/rails/category-groups/${groupId}/categories`, { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  /**
   * Returns all the categories contained within the identified category
   * group. The categories are ordered on their name, in ascending order.
   * @param groupId the group identifier.
   * @returns the identified group's categories, in ascending name order.
   */
  async fetchAllCategories(groupId: string): Promise<Array<Category>> {
    var response = await this.getCategoriesPage(groupId, 0, 100);
    var categories = response.items as Array<Category>;
    while (response.links.next) {
      response = await this.getCategoriesPage(groupId, response.page + 1, 100);
      categories = categories.concat(response.items);
    }
    return categories;
  }

  /**
   * Returns the identified category.
   * @param categoryId the category's unique identifier.
   * @returns the identified category.
   */
  getCategory(categoryId: string): Promise<Category> {
    return http.get<Category>(`/rails/categories/${categoryId}`)
      .then(response => response.data);
  }

  /**
   * Creates a new category within the identified category group, for the
   * authenticated user.
   * @param groupId the unique identifier of the category group.
   * @param category the details of the new category.
   * @returns the newly created category.
   */
  createCategory(groupId: string, category: Category): Promise<Category> {
    return http.post<Category>(`/rails/category-groups/${groupId}/categories`, category)
      .then(response => response.data);
  }

  /**
   * Modifies the mutable properties of the identified category.
   * @param category the properties of the category to be updated, including
   *  its identifier.
   * @returns the modified category.
   */
  updateCategory(category: Category): Promise<Category> {
    return http.put<Category>(`/rails/categories/${category.id}`, category)
      .then(response => response.data);
  }

  /**
   * Deletes the authenticated user's identified category. All the selectors of
   * the category will also be deleted.
   * @param categoryId the category's unique identifier.
   * @returns an empty response.
   */
  deleteCategory(categoryId: string): Promise<any> {
    return http.delete<void>(`/rails/categories/${categoryId}`);
  }

  /**
   * Returns the category selectors for the identified category, across all accounts.
   * @param categoryId the category's unique identifier.
   * @param page the zero-based index of the page to be returned.
   * @param pageSize the maximum number of selectors per page.
   * @returns those category selectors that belong to the category.
   */
  getCategorySelectors(categoryId: string,page: number = 0, pageSize: number = 20): Promise<PaginatedList<CategorySelector>> {
    return http.get<PaginatedList<CategorySelector>>(`/rails/categories/${categoryId}/selectors`, { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async getAllCategorySelectors(categoryId: string): Promise<Array<CategorySelector>> {
    var response = await this.getCategorySelectors(categoryId, 0, 100);
    var selectors = response.items as Array<CategorySelector>;
    while (response.links.next) {
      response = await this.getCategorySelectors(categoryId, response.page + 1, 100);
      selectors = selectors.concat(response.items);
    }
    return selectors;
  }

  /**
   * Moves the identified category selector to the identified destination category.
   * @param selector the category selector to be moved.
   * @param destCategoryId the identifier of the category to which the selector is to be moved.
   * @returns 
   */
  moveCategorySelector(selector: CategorySelector, destCategoryId: string): Promise<CategorySelector> {
    const destination = {
      categoryId: destCategoryId
    }
    return http.put<CategorySelector>(`/rails/categories/${selector.categoryId}/selectors/${selector.id!}`, destination)
      .then(response => response.data);
  }

  /**
   * Deletes the identified category selector.
   * @param selector the selector to be deleted.
   * @returns an empty response.
   */
  deleteCategorySelector(selector: CategorySelector): Promise<any> {
    return http.delete<void>(`/rails/categories/${selector.categoryId}/selectors/${selector.id!}`);
  }

  /**
   * Returns the category selectors for both the identified category and account.
   * @param categoryId the category's unique identifier.
   * @param accountId the account's unique identifier.
   * @param page the zero-based index of the page to be returned.
   * @param pageSize the maximum number of selectors per page.
   * @returns those category selectors that belong to both the category and account.
   */
  getAccountCategorySelectors(categoryId: string, accountId: string,
                              page: number = 0, pageSize: number = 20): Promise<PaginatedList<CategorySelector>> {
    return http.get<PaginatedList<CategorySelector>>(`/rails/categories/${categoryId}/account-selectors/${accountId}`, { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async getAllAccountCategorySelectors(categoryId: string, accountId: string): Promise<Array<CategorySelector>> {
    var response = await this.getAccountCategorySelectors(categoryId, accountId, 0, 100);
    var selectors = response.items as Array<CategorySelector>;
    while (response.links.next) {
      response = await this.getAccountCategorySelectors(categoryId, accountId, response.page + 1, 100);
      selectors = selectors.concat(response.items);
    }
    return selectors;
  }

  /**
   * Replaces the category selectors of the identified category and account with
   * those provided in the given array.
   * @param categoryId the category's unique identifier.
   * @param accountId the account's unique identifier.
   * @param selectors the collection of category selectors to be assigned.
   * @returns 
   */
  setAccountCategorySelectors(categoryId: string, accountId: string,
                              selectors: Array<CategorySelector>): Promise<Array<CategorySelector>> {
    return http.put<Array<CategorySelector>>(`/rails/categories/${categoryId}/account-selectors/${accountId}`, selectors)
      .then(response => response.data);
  }

  /**
   * Returns the statistics for the transactions covered by the given category
   * group over the given date range. The response will contain an entry for
   * each of the group's categories for which transactions exist within the
   * given date range, across all accounts.
   * @param groupId the category group's unique identifier.
   * @param fromDate the date, inclusive, from which the transaction should start.
   * @param toDate the date, exclusive, to which the transaction should end.
   * @returns an array of the category group's transaction statistics.
   */
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
