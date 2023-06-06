import CurrencyAmount from "./currency-amount.model"

export default interface RailsBalance {
  balanceAmount: CurrencyAmount
  balanceType: string,
  referenceDate: Date
}