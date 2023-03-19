import Transaction from "./transaction.model";

export default interface TransactionList {
  booked: Array<Transaction>,
  pending: Array<Transaction>
}
