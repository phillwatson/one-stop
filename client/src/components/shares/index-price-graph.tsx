import { useEffect, useMemo, useState } from "react";

import { LineChart } from "@mui/x-charts";

import { useMessageDispatch } from '../../contexts/messages/context';
import ShareService from "../../services/share.service";
import { ShareIndexResponse, HistoricalPriceResponse } from "../../model/share-indices.model";
import { formatDate, minDate, startOfMonth } from "../../util/date-util";

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
  const result = new Date(date.getTime());

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

interface Props {
  shareIndex?: ShareIndexResponse;
  fromDate: Date;
  toDate: Date;
}

export default function ShareIndexGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [ prices, setPrices ] = useState<Array<HistoricalPriceResponse>>([]);

  useEffect(() => {
    if (props.shareIndex) {
      // retrieve index prices in monthly chunks
      let startDate = startOfMonth(props.fromDate);
      const endDate = props.toDate;

      const requests = [];
      while (startDate < endDate) {
        let thisEnd = minDate(startOfMonth(startDate, 1), endDate);

        requests.push(
          ShareService.getIndexPrices(props.shareIndex!.id, startDate, thisEnd, 0, 100)
            .then(response => response.items)
        );

        startDate = thisEnd;
      }

      Promise.all(requests)
        .then(response => response.toSorted((a, b) => a[0].date.getTime() - b[0].date.getTime()))
        .then(responses => responses.reduce((all, current) => all.concat(current)))
        .then(result => setPrices(result) )
        .catch(err => {
          showMessage(err);
        })
    } else {
      setPrices([]);
    }
  }, [ showMessage, props ]);

  const dataset = useMemo(() => {
    let resolution = calcResolution(props.fromDate, props.toDate);
    let next = props.fromDate;
    return prices
      .filter(p => {
        // filter prices according to resolution
        let date = p.date;
        if ((date < next) || (date > props.toDate)) {
          return false;
        }

        next = nextDate(date, resolution);
        return true;
      })
      .map(p => ({
        date: p.date,
        opening: p.open,
        closing: p.close
      }))
  }, [ prices, props.fromDate, props.toDate ]);

  return (
    <LineChart height={ 500 }
      dataset={ dataset }
      xAxis={[{
        id: 'dates', dataKey: 'date', scaleType: 'time', valueFormatter: (date) => formatDate(date),
        label: 'Date', height: 95,
        tickLabelStyle: { angle: -40, textAnchor: 'end' }
       }]}
      series={[
        { id: 'opening', label: 'opening', dataKey: 'opening', curve: 'linear', showMark: false },
        { id: 'closing', label: 'closing', dataKey: 'closing', curve: 'linear', showMark: false }
      ]}>
    </LineChart>
  );
}