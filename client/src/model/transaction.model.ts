import AccountReference from "./account-reference.model";
import CurrencyAmount from "./currency-amount.model";

export default interface Transaction {
       id: string,
       transactionId: string,
       bookingDate: Date,
       bookingDateTime: Date,
       valueDate: Date,
       valueDateTime: Date,
       transactionAmount: number,
       transactionCurrency: string,
       additionalInformation: string,
       additionalInformationStructured: string,
       balanceAfterTransaction: string,
       bankTransactionCode: string,
       checkId: string,
       creditorAccount: AccountReference,
       creditorAgent: string,
       creditorId: string,
       creditorName: string,
       currencyExchange: string,
       debtorAccount: AccountReference,
       debtorAgent: string,
       debtorName: string,
       endToEndId: string,
       entryReference: string,
       internalTransactionId: string,
       mandateId: string,
       merchantCategoryCode: string,
       proprietaryBankTransactionCode: string,
       purposeCode: string,
       remittanceInformationStructured: string,
       remittanceInformationStructuredArray: string,
       remittanceInformationUnstructured: string,
       remittanceInformationUnstructuredArray: string,
       ultimateCreditor: string,
       ultimateDebtor: string,
}