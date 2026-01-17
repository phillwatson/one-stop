import { useEffect, useMemo, useState } from "react";

import { LineChart } from "@mui/x-charts";
import Tooltip from "@mui/material/Tooltip";
import Avatar from "@mui/material/Avatar";
import Switch from "@mui/material/Switch";
import SwitchOnIcon from '@mui/icons-material/Percent';
import SwitchOffIcon from '@mui/icons-material/AttachMoney';
import ShareService from "../../services/share.service";
import { ShareIndexResponse, HistoricalPriceResponse } from "../../model/share-indices.model";
import { useMessageDispatch } from '../../contexts/messages/context';
import { formatDate, startOfDay } from "../../util/date-util";

function toggleIcon(checked: boolean) {
  return (
    <Avatar sx={{ width: 22, height: 22, bgcolor: '#ebfcf4', color: 'black' }}>
      { checked ? <SwitchOnIcon /> : <SwitchOffIcon /> }
    </Avatar>
  )
}

const percentFormat = new Intl.NumberFormat(undefined, {
    style: 'percent',
    minimumSignificantDigits: 1,
    maximumSignificantDigits: 3,
  });

function percentageFormatter(value: number | null): string {
  return value === null ? '' : percentFormat.format(value / 100);
}

enum Resolution {
  DAILY,
  WEEKLY,
  MONTHLY
}

function calcResolution(fromDate: Date, toDate: Date): Resolution {
  // calculate difference in days
  const diff = (toDate.getTime() - fromDate.getTime()) / 86400000;

  if (diff < 64)  return Resolution.DAILY;
  if (diff < 367) return Resolution.WEEKLY;
  return Resolution.MONTHLY;
}

function nextDate(date: Date, resolution: Resolution): Date {
  const result = startOfDay(new Date(date.getTime()));

  switch (resolution) {
    case Resolution.WEEKLY:
      result.setDate(result.getDate() + 7);
      break;

      case Resolution.MONTHLY:
      result.setDate(1);
      result.setMonth(result.getMonth() + 1);
      break;

    case Resolution.DAILY:
    default:
      result.setDate(result.getDate() + 1);
      break;
  }

  return result;
}

class Comparison {
  shareIndex: ShareIndexResponse;
  prices: Array<HistoricalPriceResponse>;

  constructor(shareIndex: ShareIndexResponse,
              prices: Array<HistoricalPriceResponse>
  ) {
    this.shareIndex = shareIndex;
    this.prices = prices;
  }
}

function filterPrices(fromDate: Date, toDate: Date, prices: HistoricalPriceResponse[]): HistoricalPriceResponse[] {
  let resolution = calcResolution(fromDate, toDate);
  let next = startOfDay(fromDate);
  return prices
    .filter(p => {
      if ((p.date < next) || (p.date > toDate)) {
        return false;
      }

      next = nextDate(p.date, resolution);
      return true;
    });
}

interface Props {
  shareIndex?: ShareIndexResponse;
  compareIndices?: ShareIndexResponse[];
  fromDate: Date;
  toDate: Date;
}

export default function ShareIndexGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [ prices, setPrices ] = useState<Array<Comparison>>([]);

  useEffect(() => {
    if (props.shareIndex) {
      const allSets = props.compareIndices === undefined ? [ props.shareIndex ] : [ ...props.compareIndices, props.shareIndex];

      const requests = allSets
        // remove duplicates
        .filter((value, index) => allSets.findIndex(item => item.id === value.id) === index)

        // request prices for date range - filtered according to resolution
        .map(comparison => ShareService.getPrices(comparison.id, props.fromDate, props.toDate)
          .then(response => filterPrices(props.fromDate, props.toDate, response))
          .then(response => new Comparison(comparison, response))
        );

      // wait for requests to complete
      Promise.all(requests)
        .then(response => setPrices(response))
        .catch(err => {
          showMessage(err);
        });
    } else {
      setPrices([]);
    }
  }, [ showMessage, props.shareIndex, props.compareIndices, props.fromDate, props.toDate ]);

  const dataset = useMemo(() => {
    if (prices.length === 0) {
      return [];
    }

    // if no comparisions have been provided
    if (prices.length === 1) {
      return prices[0].prices
        .map((price, index, array) => ({
          date: price.date,
          opening: price.open,
          closing: price.close,
          growth: index > 0 ? ((price.close - array[0].close) / array[0].close) * 100 : 0
        }));
    }

    return prices
      .flatMap((comparison) =>
        comparison.prices.flatMap((price, index, array) => ({
          id: index,
          date: price.date,
          name: comparison.shareIndex.name,
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
  }, [ prices ]);

  const [ showPercentage, setShowPercentage ] = useState<boolean>(false);
  const percentageView = useMemo(() => showPercentage || prices.length > 1, [ showPercentage, prices.length ] )

  const colour = useMemo(() => {
    if (prices.length === 1) {
      const len = prices[0].prices.length;
      const growing = prices[0].prices[0].close < prices[0].prices[len - 1].close;
      return growing ? '#8FBC8F' : '#F08080';
    }

    return 'green';
  }, [ prices ])

  return (
    <>
      <Tooltip title='toggle growth view' placement='bottom'>
        <Switch color='default' icon={ toggleIcon(false) } checkedIcon={ toggleIcon(true) }
          checked={ percentageView } value={ percentageView }
          onChange={ e => setShowPercentage(e.target.checked) }
        ></Switch>
      </Tooltip>

      <LineChart height={ 500 }
        dataset={ dataset }
        xAxis={[{
            id: 'dates', dataKey: 'date', scaleType: 'time', valueFormatter: (date) => formatDate(date),
            label: 'Date', height: 95, tickLabelStyle: { angle: -40, textAnchor: 'end' }
          }]}
        yAxis={ percentageView
            ? [{ id: 'percentage', label: 'Growth (%)', valueFormatter: percentageFormatter }]
            : [{ id: 'price', label: 'Price' }]
          }
        series=
        { percentageView
          ? (prices.length === 1) 
            ? [{
                id: 'percentage', label: 'growth', dataKey: 'growth',
                curve: 'linear', showMark: false, valueFormatter: percentageFormatter,
                area: true, baseline: 'min', color: colour
              }]
            : prices.map(comparison => ({
                id: comparison.shareIndex.id,
                label: comparison.shareIndex.name,
                dataKey: comparison.shareIndex.name,
                curve: 'linear', showMark: false, valueFormatter: percentageFormatter
              }))
          : [{
              id: 'closing', label: 'closing price', dataKey: 'closing',
              curve: 'linear', showMark: false, area: true, baseline: 'min', color: colour
            }]
        }>
      </LineChart>
    </>
  );
}