import { createContext, useContext, useState, PropsWithChildren } from "react";
import { Currency } from '../../model/commons.model';

/**
 * An interface to describe the state that we will pass in the provider.
*/
interface MonetaryContextValue {
  /**
   * A function that will format the given monetary value according to the rules
   * of the given currency. If the context has hidden monetary values, the result
   * will be a masked value; showing the currency symbol but no indication of the
   * true value.
   * 
   * @param amount the amount to be formatted.
   * @param currency the currency to be used in formatting the amount.
   * @param minorUnits whether the amount is given in minor units, and should be
   *  converted to major units and formatted with more fraction digits.
   * @returns a string representing the formatted monetary value.
   */
  format: (amount: number, currency: Currency, minorUnits?: boolean) => string;

  /**
   * Indicates whether monetary values are currently to be hidden.
   */
  hidden: boolean;

  /**
   * Allows the caller to toggle the hiding of monetary values.
   * @param value the value to be assigned to the 'hidden' property.
   */
  setHidden: (value: boolean) => void;
}

/**
 * Creates a context in which to store, and pass, the state of the monetary value
 * display; and a format function to allow the caller to format a monerary value.
 */
const MonetaryContext = createContext<MonetaryContextValue>({
  format: (amount: number, currency: Currency, minorUnits?: boolean) => { return "" },
  hidden: false,
  setHidden: (value: boolean) => {},
});

/**
 * The function that allows callers to query and update the context's state; and
 * a function to be used to format monetary values.
 * An example of it's use:
 * ```
 *  import useMonetaryContext from '../../contexts/monetary/monetary-context';
 *
 *  const [ formatMoney ] = useMonetaryContext();
 *  ...
 *  formatMoney(1234.55, 'GPB');
 * ```
 */
export default function useMonetaryContext(): [
  (amount: number, currency: Currency, minorUnits?: boolean) => string,
  boolean,
  (value: boolean) => void
] {
  const x = useContext(MonetaryContext);
  return [ x.format, x.hidden, x.setHidden ];
}

/**
 * A collection of monetary value formatters according to currency rules; keyed on the currency.
 */
const formatters = {
  'EUR': { formatter: new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }) },
  'GBP': { formatter: new Intl.NumberFormat('en-EN', { style: 'currency', currency: 'GBP' }) },
  'USD': { formatter: new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }) },
}

/**
 * A collection of monetary value formatters according to currency rules; keyed on the currency.
 * These are used to format values originally given in minor currency units, and a more precise
 * number of fraction digits.
 */
const formattersMinor = {
  'EUR': { formatter: new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR', minimumFractionDigits: 4 }) },
  'GBP': { formatter: new Intl.NumberFormat('en-EN', { style: 'currency', currency: 'GBP', minimumFractionDigits: 4 }) },
  'USD': { formatter: new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 4 }) },
}

/**
 * The provider that suppliers and maintains the monetary context.
 * @param props a collection of UI child nodes.
 */
export function MonetaryFormatProvider(props: PropsWithChildren) {
  const [ hidden, setHidden ] = useState<boolean>(false)

  function format(amount: number, currency: Currency, minorUnits?: boolean): string {
    const result = (minorUnits) 
      ? formattersMinor[currency].formatter.format( (hidden) ? 9999.9999 : amount / 100 )
      : formatters[currency].formatter.format( (hidden) ? 9999.99 : amount );

    return (hidden) ? result.replaceAll('9', '#') : result;
  }

  return (
    <MonetaryContext.Provider value={ { format, hidden, setHidden }}>
      { props.children }
    </MonetaryContext.Provider>
  );
}