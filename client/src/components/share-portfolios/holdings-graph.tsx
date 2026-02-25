import { useEffect, useState, useMemo } from "react";
import { areaElementClasses, LineChart } from "@mui/x-charts";

import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { ShareHoldingSummary, ShareTrade } from '../../model/share-portfolio.model';
import { SharePrice } from "../../model/share-indices.model";
import { formatShortDate, startOfDay } from "../../util/date-util";
import { HoldingPrices } from "./holdings-editor";

interface Props {
  // Array of portfolio holdings and their prices over a date range
  holdingPrices: Array<HoldingPrices>;

  // A map of trades made against the above holdings - indexed by share-index-id
  holdingTrades: Map<string,Array<ShareTrade>>;
}


/**
 * A number formatter to format numbers as percentages.
 */
const percentFormat = new Intl.NumberFormat(undefined, {
    style: 'percent',
    minimumSignificantDigits: 1,
    maximumSignificantDigits: 3,
  });

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

  constructor(holdingPrices: HoldingPrices, trades?: Array<ShareTrade>) {
    this.holding = holdingPrices.holding;
    this.trades = trades ? trades.sort((a, b) => a.dateExecuted.getTime() - b.dateExecuted.getTime()) : [];
    this.prices = holdingPrices.prices || [];

    var tradeIndex = 0;
    var quantity = 0;
    var nextTrade: ShareTrade | undefined = (this.trades.length > 0) ? this.trades[0] : undefined;

    this.values = this.prices.map((price, index, array) => {
      while ((nextTrade) && (nextTrade.dateExecuted <= price.date)) {
        quantity += nextTrade.quantity;
        nextTrade = (++tradeIndex < this.trades.length) ? this.trades[tradeIndex] : undefined;
      }
      return {
        date: startOfDay(price.date),
        value: (price.close * quantity) / 100.0,
        growth: index > 0 ? ((price.close - array[0].close) / array[0].close) * 100 : 0
      };
    });
  }
}

export default function HoldingsGraph({ holdingPrices, holdingTrades }: Props) {
  const [ formatMoney ] = useMonetaryContext();
  const [ holdingData, setHoldingData ] = useState<Array<ShareHolding>>([]);

  const values = useMemo(() => {
    return holdingData.length === 0 ? [] : holdingData
      // simplify holdings into one large array
      .flatMap(holding =>
        holding.values.flatMap((value) => ({
          id: value.date.getTime(),
          date: value.date,
          value: value.value
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
        grid={{ vertical: false, horizontal: true }} hideLegend
        xAxis={[{
            id: 'dates', dataKey: 'date', scaleType: 'time',
            valueFormatter: (date, context) => formatShortDate(date, (context.location === 'tick')),
            height: 45, tickLabelStyle: { angle: 0, textAnchor: 'middle' },
          }]}
        yAxis={[{ width: 90, valueFormatter: (value: number) => formatMoney(value, 'GBP') }]}
        dataset={ values }
        series={ (
          [{
            id: 'values', label: 'Value', dataKey: 'value',
            curve: 'linear', showMark: false, area: true,
            baseline: 'min', color: axisColour,
            valueFormatter: (value, context) => {
              let result = formatMoney(value, 'GBP');
              const growth = (holdingData.length === 1)
                ? holdingData[0].values[context.dataIndex].growth
                : values[context.dataIndex].growth;

              result += ' (' + percentFormat.format(growth / 100) + ')';
              return result;
            }
          }])}
        
        sx={{
          [`& .${areaElementClasses.root}[data-series="values"]`]: {
            fill: "url('#percentage')",
            filter: 'none' // Remove the default filtering
          },
        }}>
        <defs>
          <linearGradient id="percentage" gradientTransform="rotate(90)" >
            <stop offset="0" stopColor={ axisColour } />
            <stop offset="100%" stopColor="#FFFFFF50" />
          </linearGradient>
        </defs>
       </LineChart>
    }
  </>
  );
}
