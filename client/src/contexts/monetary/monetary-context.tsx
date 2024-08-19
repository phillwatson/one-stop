import { createContext, useContext, useState, PropsWithChildren } from "react";
import { Currency } from '../../model/account.model';

/**
 * An interface to describe the state that we will pass in the provider.
*/
interface MonetaryContextValue {
  format: (amount: number, currency: Currency) => string;
  hidden: boolean;
  setHidden: (value: boolean) => void;
}

const MonetaryContext = createContext<MonetaryContextValue>({
  format: (amount: number, currency: Currency) => { return "" },
  hidden: false,
  setHidden: (value: boolean) => {}
});

export default function useMonetaryContext(): [ (amount: number, currency: Currency) => string, boolean, (value: boolean) => void ] {
  const x = useContext(MonetaryContext);
  return [ x.format, x.hidden, x.setHidden ];
}

const formatters = {
  'EUR': { formatter: new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }) },
  'GBP': { formatter: new Intl.NumberFormat('en-EN', { style: 'currency', currency: 'GBP' }) },
  'USD': { formatter: new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }) },
}

export function MonetaryFormatProvider(props: PropsWithChildren) {
  const [ hidden, setHidden ] = useState<boolean>(false)

  function format(amount: number, currency: Currency): string {
    const result = formatters[currency].formatter.format( (hidden) ? 999.99 : amount);
    return (hidden) ? result.replaceAll('9', '#') : result;
  }

  return (
    <MonetaryContext.Provider value={ { format, hidden, setHidden }}>
      { props.children }
    </MonetaryContext.Provider>
  );
}