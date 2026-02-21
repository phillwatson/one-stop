import { useEffect, useState } from "react";
import { areaElementClasses, LineChart } from "@mui/x-charts";

import { useMessageDispatch } from '../../contexts/messages/context';
import PortfolioService from '../../services/portfolio.service';
import ShareService from '../../services/share.service';
import { ShareTradeSummary, ShareTrade } from '../../model/share-portfolio.model';
import { HistoricalPriceResponse } from "../../model/share-indices.model";
import { formatDate } from "../../util/date-util";


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

  constructor(holding: ShareTradeSummary, trades: Array<ShareTrade>) {
    this.holding = holding;
    this.trades = trades.sort((a, b) => a.dateExecuted.getTime() - b.dateExecuted.getTime());
  }

  setPrices(value: Array<HistoricalPriceResponse>) {
    this.prices = value;

    // var tradeIndex = 0;
    // var quantity = 0;
    // var nextTrade: ShareTrade | undefined = (this.trades.length > 0) ? this.trades[0] : undefined;

    // this.values = this.prices.map(price => {
    //   while ((nextTrade) && (nextTrade.dateExecuted <= price.date)) {
    //     quantity += nextTrade.quantity;
    //     nextTrade = (++tradeIndex < this.trades.length) ? this.trades[tradeIndex] : undefined;
    //   }
    //   return {
    //     date: price.date,
    //     price: (price.close * quantity) / 100.0
    //   };
    // })
  }

}

interface Props {
  /**
   * Array of share trade summaries to display
   */
  holdings: ShareTradeSummary[];
}

export default function HoldingsGraph({ holdings }: Props) {
  const showMessage = useMessageDispatch();
  const [ holdingData, setHoldingData ] = useState<Array<ShareHolding>>([]);

  useEffect(() => {
    // retrieve trade history for each holding
    const requests = holdings
      .map(holding => PortfolioService
        .fetchShareTrades(holding.portfolioId, holding.shareIndexId)
        .then(trades => new ShareHolding(holding, trades))
      );

    // wait for requests to complete
    Promise.all(requests)
      .then(holdings => setHoldingData(holdings))
      .catch(err => showMessage(err));
  }, [ showMessage, holdings ]);

  useEffect(() => {
    const toDate = new Date(Date.now());
    const fromDate = new Date(toDate.getTime() - ((30 * 24 * 60 * 60) * 1000));

    // Fetch the historical prices each share holding - filtered according to date range
    const requests = holdingData
      .map(holding => ShareService.getPrices(holding.holding.shareIndexId, fromDate, toDate)
        .then(response => response.filter(p => ((p.date >= fromDate) && (p.date <= toDate))))
        .then(response => holding.setPrices(response))
      );

    // wait for requests to complete
    Promise.all(requests)
      .catch(err => showMessage(err));
  }, [ showMessage, holdingData ]);

return (
  <>
    { holdingData.length > 0 && 
       <LineChart height={ 500 }
        dataset={ holdingData[0].values}
        grid={{ vertical: false, horizontal: true }}
        xAxis={[{
            id: 'dates', dataKey: 'date', scaleType: 'time', valueFormatter: (date) => formatDate(date),
            label: 'Date', height: 95, tickLabelStyle: { angle: -40, textAnchor: 'end' }
          }]}
        yAxis={[{ id: 'price', label: 'Price' }]
          }
        series=
        { [{
              id: 'single', label: 'closing price', dataKey: 'price',
              curve: 'linear', showMark: false,
              area: true, baseline: 'min', color: '#32323233'
            }]
        }
        
        sx={{
          [`& .${areaElementClasses.root}[data-series="single"]`]: {
            fill: "url('#percentage')",
            filter: 'none' // Remove the default filtering
          },
        }}>
        <defs>
          <linearGradient id="percentage" gradientTransform="rotate(90)" >
            <stop offset="0" stopColor={ "#12121212" } />
            <stop offset="100%" stopColor="#FFFFFF50" />
          </linearGradient>
        </defs>
       </LineChart>
    }
  </>
  );
}