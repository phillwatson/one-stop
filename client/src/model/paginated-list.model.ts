export default interface PaginatedList<T> {
  total: number,
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
