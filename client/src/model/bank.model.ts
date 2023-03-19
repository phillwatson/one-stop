export default interface Bank {
  id?: any | null,
  name: string,
  bic: string,
  countries: Array<string>,
  transaction_total_days: number,
  logo: string,
  paymentsEnabled: boolean
}
