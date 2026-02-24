import { useEffect, useState, useMemo } from "react";
import { areaElementClasses, LineChart } from "@mui/x-charts";

import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { useMessageDispatch } from '../../contexts/messages/context';
import PortfolioService from '../../services/portfolio.service';
import ShareService from '../../services/share.service';
import { ShareTradeSummary, ShareTrade } from '../../model/share-portfolio.model';
import { HistoricalPriceResponse } from "../../model/share-indices.model";
import { formatShortDate, startOfDay } from "../../util/date-util";

/**
 * Generates a colour code based on the input string.
 * @param str the string to generate a colour for.
 * @returns the colour code.
 */
const stringToColour = (str: string) => {
  let hash = 0;
  str.split('').forEach(char => { hash = char.charCodeAt(0) + ((hash << 5) - hash) })
  let colour = '#'
  for (let i = 0; i < 3; i++) {
    const value = (hash >> (i * 3)) & 0xff
    colour += value.toString(16).padStart(2, '0')
  }
  return colour;
}

/**
 * Encapsulates a share index and its associated historical prices for view and comparison.
 */
class ShareHolding {
  /**
   * The share index being viewed and compared.
   */
  holding: ShareTradeSummary;

  trades: Array<ShareTrade> = [];

  /**
   * The historical prices for the share index.
   */
  prices: Array<HistoricalPriceResponse> = [];

  values: Array<any> = [];

  colour: string;

  constructor(holding: ShareTradeSummary, trades: Array<ShareTrade>, prices: Array<HistoricalPriceResponse>) {
    this.holding = holding;
    this.trades = trades.sort((a, b) => a.dateExecuted.getTime() - b.dateExecuted.getTime());
    this.prices = prices;
    this.colour = stringToColour(holding.shareIndexId);

    var tradeIndex = 0;
    var quantity = 0;
    var nextTrade: ShareTrade | undefined = (this.trades.length > 0) ? this.trades[0] : undefined;

    this.values = this.prices.map((price, index) => {
      while ((nextTrade) && (nextTrade.dateExecuted <= price.date)) {
        quantity += nextTrade.quantity;
        nextTrade = (++tradeIndex < this.trades.length) ? this.trades[tradeIndex] : undefined;
      }
      return {
        id: index,
        date: startOfDay(price.date),
        value: (price.close * quantity) / 100.0
      };
    });
  }
}

interface Props {
  /**
   * Array of share trade summaries to display
   */
  holdings: ShareTradeSummary[];
}

export default function HoldingsGraph({ holdings }: Props) {
  const [ formatMoney ] = useMonetaryContext();
  const showMessage = useMessageDispatch();
  const [ holdingData, setHoldingData ] = useState<Array<ShareHolding>>([]);
  const [ fromDate, setFromDate ] = useState<Date>(new Date(Date.now() - ((360 * 24 * 60 * 60) * 1000)));
  const [ toDate, setToDate ] = useState<Date>(new Date(Date.now()));
  const values = useMemo(() => {
    return holdingData
      .flatMap(holding =>
        holding.values.flatMap((value) => ({
          id: value.date.getTime(),
          date: value.date,
          value: value.value
        }))
      )
      .reduce((acc, obj) => {
        let entry: any = acc.find(item => item['id'] === obj['id']);
        if (entry === undefined) {
          entry = { id: obj.id, date: obj.date, value: 0 };
          acc.push(entry);
        }
        
        entry['value'] += obj.value;
        return acc;
      }, [] as any[]);
  }, [ holdingData ]);

  useEffect(() => {
    // retrieve trade history for each holding
    const requests = holdings
      // fetch the trades for each holding
      .map(holding => PortfolioService.fetchShareTrades(holding.portfolioId, holding.shareIndexId)
        // fetch the historical prices each holding - filtered according to date range
        .then(trades => ShareService.getPrices(holding.shareIndexId, fromDate, toDate)
          .then(prices => new ShareHolding(holding, trades, prices))
        )
      );

    // wait for requests to complete
    Promise.all(requests)
      .then(holdings => setHoldingData(holdings))
      .catch(err => showMessage(err));
  }, [ showMessage, holdings, fromDate, toDate ]);

return (
  <>
    { holdingData.length > 0 && 
       <LineChart height={ 500 }
        grid={{ vertical: false, horizontal: true }}
        hideLegend
        xAxis={[{
            id: 'dates', dataKey: 'date', scaleType: 'time',
            valueFormatter: (date) => formatShortDate(date),
            height: 45, tickLabelStyle: { angle: 0, textAnchor: 'middle' },
          }]}
        yAxis={[{ width: 70 }]}
        dataset={ values }
        series={ (
          [{
            id: 'values', label: 'Value', dataKey: 'value',
            curve: 'linear', showMark: false, valueFormatter: (value) => formatMoney(value, 'GBP'),
            area: true, baseline: 'min', color: '#478aee'
          }])}
        
        sx={{
          [`& .${areaElementClasses.root}[data-series="values"]`]: {
            fill: "url('#percentage')",
            filter: 'none' // Remove the default filtering
          },
        }}>
        <defs>
          <linearGradient id="percentage" gradientTransform="rotate(90)" >
            <stop offset="0" stopColor={ "#478aee" } />
            <stop offset="100%" stopColor="#FFFFFF50" />
          </linearGradient>
        </defs>
       </LineChart>
    }
  </>
  );
}
