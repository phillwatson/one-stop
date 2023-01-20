export default interface Bank {
  id?: any | null,
  name: string,
  bic: string,
  countries: Array<String>,
  transaction_total_days: number,
  logo: string
}
