import { useEffect, useState, useMemo } from "react";
import { areaElementClasses, LineChart } from "@mui/x-charts";

import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { ShareHoldingSummary, ShareTrade } from '../../model/share-portfolio.model';
import { SharePrice } from "../../model/share-indices.model";
import { formatShortDate, formatDate, startOfDay } from "../../util/date-util";
import { HoldingPrices } from "./holdings-editor";
import { stringToColour } from "../../util/string-util";
import { percentageFormatter } from "../../util/number-util";

interface Props {
  showPercentages?: boolean;

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
  prices: Array<SharePrice> = [];

  // The historical trades made on the holding
  trades: Array<ShareTrade> = [];

  // The calculated historical value of the holding
  values: Array<any> = [];

  colour: string;

  constructor(holdingPrices: HoldingPrices, trades?: Array<ShareTrade>) {
    this.holding = holdingPrices.holding;
    this.trades = trades ? trades.sort((a, b) => a.dateExecuted.getTime() - b.dateExecuted.getTime()) : [];
    this.prices = holdingPrices.prices || [];
    this.colour = stringToColour(this.holding.shareIndexId);

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

export default function HoldingsGraph({ showPercentages, holdingPrices, holdingTrades }: Props) {
  const [ formatMoney ] = useMonetaryContext();
  const [ holdingData, setHoldingData ] = useState<Array<ShareHolding>>([]);

  const values = useMemo(() => {
    return holdingData.length === 0 ? [] : holdingData
      // simplify holdings into one large array
      .flatMap(holding =>
        holding.values.flatMap((value) => ({
          id: value.date.getTime(),
          date: value.date,
          value: value.value,
        }))
      )
      // sum all holding values of the same date to create single row
      .reduce((acc, obj) => {
        let entry: any = acc.find(item => item['id'] === obj['id']);
        if (entry === undefined) {
          entry = { id: obj.id, date: obj.date, value: 0 };
          acc.push(entry);
        }
        
        entry['value'] += obj.value;
        return acc;
      }, [] as any[])
      // calculate growth across all holdings
      .map((item, index, array) => {
        item.growth = index > 0 ? ((item.value - array[0].value) / array[0].value) * 100 : 0
        return item;
      });
  }, [ holdingData ]);

  const percentages = useMemo(() => {
    return holdingData.length === 0 ? [] : holdingData
      // simplify holdings into one large array
      .flatMap(holding =>
        holding.prices.flatMap((price, index, array) => ({
          id: price.date.getTime(),
          name: holding.holding.name,
          date: price.date,
          growth: index > 0 ? ((price.close - array[0].close) / array[0].close) * 100 : 0
        }))
      )
      .reduce((acc, obj) => {
        let entry: any = acc.find(item => item['id'] === obj['id']);
        if (entry === undefined) {
          entry = { id: obj.id, date: obj.date };
          acc.push(entry);
        }
        
        entry[obj.name] = obj.growth
        return acc;
      }, [] as any[]);
  }, [ holdingData ])

  useEffect(() => {
    // retrieve trade history for each holding
    setHoldingData(holdingPrices
      // identify the trades for each holding
      .map(record => new ShareHolding(record, holdingTrades.get(record.holding.shareIndexId)))
      );
  }, [ holdingPrices, holdingTrades ]);

  const axisColour = useMemo(() => {
    if (holdingData.length === 1) {
      const len = holdingData[0].values.length;
      const growing = holdingData[0].values[0].value < holdingData[0].values[len - 1].value;
      return growing ? '#7AAF7A' : '#F08080';
    }
    return '#478aee';
  }, [ holdingData ])

return (
  <>
    { holdingData.length > 0 && 
       <LineChart height={ 400 } margin={{ left: 0 }}
        dataset={ showPercentages ? percentages : values }
        grid={{ vertical: false, horizontal: true }} hideLegend
        xAxis={[{
            id: 'dates', dataKey: 'date', scaleType: 'time',
            valueFormatter: (date, context) => (context.location === 'tick') ? formatDate(date) : formatShortDate(date),
            height: 75, tickLabelStyle: { angle: -40, textAnchor: 'end' }
          }]}
        yAxis={ showPercentages
          ? [{ id: 'percentages', label: 'Growth (%)', valueFormatter: percentageFormatter }]
          : [{ width: 90, valueFormatter: (value: number) => formatMoney(value, 'GBP') }]
        }
        series={ showPercentages
          ? holdingData.map(holding => ({
                id: holding.holding.shareIndexId,
                label: holding.holding.name,
                dataKey: holding.holding.name,
                curve: 'linear', showMark: false, valueFormatter: percentageFormatter,
                color: holding.colour
            }))
          : [{
              id: 'values', dataKey: 'value',
              curve: 'linear', showMark: false, area: true,
              baseline: 'min', color: axisColour,
              valueFormatter: (value, context) => formatMoney(value, 'GBP') +
                  ' (' + percentageFormatter(values[context.dataIndex].growth) + ')'
            }]
          }
        
        sx={{
          [`& .${areaElementClasses.root}[data-series="values"]`]: {
            fill: "url('#gradient')",
            filter: 'none' // Remove the default filtering
          },
        }}>
        <defs>
          <linearGradient id="gradient" gradientTransform="rotate(90)" >
            <stop offset="0" stopColor={ axisColour } />
            <stop offset="100%" stopColor="#FFFFFF50" />
          </linearGradient>
        </defs>
       </LineChart>
    }
  </>
  );
}
