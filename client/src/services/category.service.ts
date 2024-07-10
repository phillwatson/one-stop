import http from './http-common';
import PaginatedList from '../model/paginated-list.model';
import { Category, CategorySelector, CategoryStatistics } from '../model/category.model';

class CategoryService {
  getPage(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<Category>> {
    console.log(`Retrieving categories [page: ${page}, pageSize: ${pageSize}]`);
    return http.get<PaginatedList<Category>>('/rails/categories', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllCategories(): Promise<Array<Category>> {
    console.log('Retrieving ALL categories');
    var response = await this.getPage(0, 100);
    var categories = response.items as Array<Category>;
    while (response.links.next) {
      response = await this.getPage(response.page + 1, 100);
      categories = categories.concat(response.items);
    }
    return categories;
  }

  get(categoryId: string): Promise<Category> {
    console.log(`Retrieving category [id: ${categoryId}]`);
    return http.get<Category>(`/rails/categories/${categoryId}`)
      .then(response => response.data);
  }

  createCategory(category: Category): Promise<Category> {
    console.log(`Creating category [name: ${category.name}]`);
    return http.post<Category>('/rails/categories', category)
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
}

const instance = new CategoryService();
export default instance;
