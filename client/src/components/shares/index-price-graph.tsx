import { useEffect, useMemo, useState } from "react";

import { areaElementClasses, LineChart } from "@mui/x-charts";
import Tooltip from "@mui/material/Tooltip";
import Avatar from "@mui/material/Avatar";
import Switch from "@mui/material/Switch";
import ShowPercentageIcon from '@mui/icons-material/Percent';
import ShowPriceIcon from '@mui/icons-material/AttachMoney';
import ShareService from "../../services/share.service";
import { ShareIndex, HistoricalPriceResponse } from "../../model/share-indices.model";
import { useMessageDispatch } from '../../contexts/messages/context';
import { formatDate, startOfDay } from "../../util/date-util";

/**
 * Provides a visual indication of the selection between price and percentage growth.
 * @param checked if true, indicates percentage growth is selected.
 * @returns the icon to display.
 */
function toggleViewIcon(checked: boolean) {
  return (
    <Avatar sx={{ width: 22, height: 22, bgcolor: '#ebfcf4', color: 'black' }}>
      { checked ? <ShowPercentageIcon /> : <ShowPriceIcon /> }
    </Avatar>
  )
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
 * Formats a number as a percentage string.
 * @param value the number to format.
 * @returns the formatted percentage string.
 */
function percentageFormatter(value: number | null): string {
  return value === null ? '' : percentFormat.format(value / 100);
}

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
  return colour
}

/**
 * A resolution level for filtering historical prices.
 */
enum Resolution {
  DAILY,
  WEEKLY,
  MONTHLY
}

/**
 * Calculates the resolution level for filtering historical prices.
 * @param fromDate the date from which historical prices begin.
 * @param toDate the date to which historical prices end.
 * @returns the resolution level appropriate for the date range.
 */
function calcResolution(fromDate: Date, toDate: Date): Resolution {
  // calculate difference in days
  const diff = (toDate.getTime() - fromDate.getTime()) / 86400000;

  if (diff < 64)  return Resolution.DAILY;
  if (diff < 367) return Resolution.WEEKLY;
  return Resolution.MONTHLY;
}

/**
 * Calculates the next date according to the given date resolution.
 * @param date the date from which the next date is calculated.
 * @param resolution the resolution level.
 * @returns the next date.
 */
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

/**
 * Encapsulates a share index and its associated historical prices for view and comparison.
 */
class Comparison {
  /**
   * The share index being viewed and compared.
   */
  shareIndex: ShareIndex;

  /**
   * The historical prices for the share index.
   */
  prices: Array<HistoricalPriceResponse>;

  /**
   * The colour assigned to the share index for graphing purposes.
   */
  colour: string;

  constructor(shareIndex: ShareIndex, prices: Array<HistoricalPriceResponse>) {
    this.shareIndex = shareIndex;
    this.prices = prices;
    this.colour = stringToColour(shareIndex.id)
  }
}

/**
 * Filters historical prices according to the given date range and resolution.
 * @param fromDate the date from which historical prices begin.
 * @param toDate the date to which historical prices end.
 * @param prices the historical prices to filter.
 * @returns the filtered historical prices.
 */
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

/**
 * The properties for the ShareIndexGraph component.
 */
interface Props {
  shareIndex?: ShareIndex;
  compareWith?: ShareIndex[];
  fromDate: Date;
  toDate: Date;
}

export default function ShareIndexGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [ prices, setPrices ] = useState<Array<Comparison>>([]);

  /**
   * Fetches and sets the historical prices for the selected share index and any
   * comparison share indices.
   */
  useEffect(() => {
    if (props.shareIndex) {
      const allSets = props.compareWith === undefined ? [ props.shareIndex ] : [ ...props.compareWith, props.shareIndex ];

      const requests = allSets
        // remove duplicate share indices
        .filter((value, index) => allSets.findIndex(item => item.id === value.id) === index)

        // request prices for date range - filtered according to resolution
        .map(shareIndex => ShareService.getPrices(shareIndex.id, props.fromDate, props.toDate)
          .then(response => filterPrices(props.fromDate, props.toDate, response))
          .then(response => new Comparison(shareIndex, response))
        );

      // wait for requests to complete
      Promise.all(requests)
        .then(responses => responses.sort((a, b) => a.shareIndex.name.localeCompare(b.shareIndex.name)))
        .then(response => setPrices(response))
        .catch(err => {
          showMessage(err);
        });
    } else {
      setPrices([]);
    }
  }, [ showMessage, props.shareIndex, props.compareWith, props.fromDate, props.toDate ]);

  /**
   * Creates a flattened dataset containing all the share prices for the line chart.
   */
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

    // create an array of objects in which each element contains the date and percentage growth
    // for each share index on that date. The property names are the share index names.
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

  const [ selectPercentage, setSelectPercentage ] = useState<boolean>(false);
  const percentageView = useMemo(() => selectPercentage || prices.length > 1, [ selectPercentage, prices.length ] )

  const axisColour = useMemo(() => {
    if (prices.length === 1) {
      const len = prices[0].prices.length;
      const growing = prices[0].prices[0].close < prices[0].prices[len - 1].close;
      return growing ? '#7AAF7A' : '#F08080';
    }

    return 'green';
  }, [ prices ])

  return (
    <>
      <Tooltip title='toggle growth view' placement='bottom'>
        <Switch color='default' icon={ toggleViewIcon(false) } checkedIcon={ toggleViewIcon(true) }
          checked={ percentageView } value={ percentageView }
          onChange={ e => setSelectPercentage(e.target.checked) }
        ></Switch>
      </Tooltip>

      <LineChart height={ 500 }
        dataset={ dataset }
        grid={{ vertical: false, horizontal: true }}
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
                id: 'single', label: 'growth', dataKey: 'growth',
                curve: 'linear', showMark: false, valueFormatter: percentageFormatter,
                area: true, baseline: 'min', color: axisColour
              }]
            : prices.map(comparison => ({
                id: comparison.shareIndex.id,
                label: comparison.shareIndex.name,
                dataKey: comparison.shareIndex.name,
                curve: 'linear', showMark: false, valueFormatter: percentageFormatter,
                color: comparison.colour
              }))
          : [{
              id: 'single', label: 'closing price', dataKey: 'closing',
              curve: 'linear', showMark: false,
              area: true, baseline: 'min', color: axisColour
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
            <stop offset="0" stopColor={ axisColour } />
            <stop offset="100%" stopColor="#FFFFFF50" />
          </linearGradient>
        </defs>
      </LineChart>
    </>
  );
}