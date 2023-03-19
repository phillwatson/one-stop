import CurrencyAmount from "./currency-amount.model"

export default interface AccountBalances {
  balanceAmount: CurrencyAmount
  balanceType: string,
  referenceDate: Date
}