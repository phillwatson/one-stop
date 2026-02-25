import { useEffect, useState, useMemo } from "react";
import { areaElementClasses, LineChart } from "@mui/x-charts";

import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { ShareHoldingSummary, ShareTrade } from '../../model/share-portfolio.model';
import { HistoricalPriceResponse } from "../../model/share-indices.model";
import { formatShortDate, startOfDay } from "../../util/date-util";
import { HoldingPrices } from "./holdings-editor";

interface Props {
  // Array of portfolio holdings and their prices over a date range
  holdingPrices: Array<HoldingPrices>;

  // A map of trades made against the above holdings - indexed by share-index-id
  holdingTrades: Map<string,Array<ShareTrade>>;
}

/**
 * Encapsulates a share index and its associated historical prices for view and comparison.
 */
class ShareHolding {
  // The share index being viewed and compared.
  holding: ShareHoldingSummary;

  // The historical prices for the share index.
  prices: Array<HistoricalPriceResponse> = [];

  // The historical trades made on the holding
  trades: Array<ShareTrade> = [];

  // The calculated historical value of the holding
  values: Array<any> = [];

  constructor(holdingPrices: HoldingPrices, trades?: Array<ShareTrade>) {
    this.holding = holdingPrices.holding;
    this.trades = trades ? trades.sort((a, b) => a.dateExecuted.getTime() - b.dateExecuted.getTime()) : [];
    this.prices = holdingPrices.prices || [];

    var tradeIndex = 0;
    var quantity = 0;
    var nextTrade: ShareTrade | undefined = (this.trades.length > 0) ? this.trades[0] : undefined;

    this.values = this.prices.map(price => {
      while ((nextTrade) && (nextTrade.dateExecuted <= price.date)) {
        quantity += nextTrade.quantity;
        nextTrade = (++tradeIndex < this.trades.length) ? this.trades[tradeIndex] : undefined;
      }
      return {
        date: startOfDay(price.date),
        value: (price.close * quantity) / 100.0
      };
    });
  }
}

export default function HoldingsGraph({ holdingPrices, holdingTrades }: Props) {
  const [ formatMoney ] = useMonetaryContext();
  const [ holdingData, setHoldingData ] = useState<Array<ShareHolding>>([]);

  const values = useMemo(() => {
    return holdingData.length === 0 ? [] : holdingData
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
    setHoldingData(holdingPrices
      // identify the trades for each holding
      .map(record => new ShareHolding(record, holdingTrades.get(record.holding.shareIndexId)))
      );
  }, [ holdingPrices, holdingTrades ]);

return (
  <>
    { holdingData.length > 0 && 
       <LineChart height={ 400 } margin={{ left: 0 }}
        grid={{ vertical: false, horizontal: true }} hideLegend
        xAxis={[{
            id: 'dates', dataKey: 'date', scaleType: 'time',
            valueFormatter: (date) => formatShortDate(date, true),
            height: 45, tickLabelStyle: { angle: 0, textAnchor: 'middle' },
          }]}
        yAxis={[{ width: 90, valueFormatter: (value: number) => formatMoney(value, 'GBP') }]}
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
