export default interface PaginatedList<T> {
  total: number,
  totalPages: number,
  count: number,
  page: number,
  pageSize: number,
  links: {
    first: string,
    previous?: string,
    next?: string,
    last: string
  }
  items: Array<T>
}

export const EMPTY_PAGINATED_LIST = {
  total: 0,
  totalPages: 0,
  count: 0,
  page: 0,
  pageSize: 0,
  links: {
    first: '',
    last: ''
  },
  items: []
}