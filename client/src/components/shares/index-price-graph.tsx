import { useEffect, useMemo, useState } from "react";

import { LineChart } from "@mui/x-charts";
import Tooltip from "@mui/material/Tooltip";
import Avatar from "@mui/material/Avatar";
import Switch from "@mui/material/Switch";
import SwitchOnIcon from '@mui/icons-material/Percent';
import SwitchOffIcon from '@mui/icons-material/AttachMoney';
import { useMessageDispatch } from '../../contexts/messages/context';
import ShareService from "../../services/share.service";
import { ShareIndexResponse, HistoricalPriceResponse } from "../../model/share-indices.model";
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
  return value === null
    ? ''
    : percentFormat.format(value / 100);
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
      ShareService.getPrices(props.shareIndex!.id, props.fromDate, props.toDate)
        .then(response => setPrices(response))
        .catch(err => {
          showMessage(err);
        })
    } else {
      setPrices([]);
    }
  }, [ showMessage, props ]);

  const dataset = useMemo(() => {
    let resolution = calcResolution(props.fromDate, props.toDate);

    let next = startOfDay(props.fromDate);

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
      .map((price, index, array) => ({
        date: price.date,
        opening: price.open,
        closing: price.close,
        growth: index > 0 ? ((price.close - array[0].close) / array[0].close) * 100 : 0
      }));
  }, [ prices, props.fromDate, props.toDate ]);

  const [ showPercentage, setShowPercentage ] = useState<boolean>(false);

  return (
    <>
      <Tooltip title='toggle growth view' placement='bottom'>
        <Switch color='default' icon={ toggleIcon(false) } checkedIcon={ toggleIcon(true) }
          value={ showPercentage } onChange={ e => setShowPercentage(e.target.checked) }
        ></Switch>
      </Tooltip>

      <LineChart height={ 500 }
        dataset={ dataset }
        xAxis={[{
          id: 'dates', dataKey: 'date', scaleType: 'time', valueFormatter: (date) => formatDate(date),
          label: 'Date', height: 95, tickLabelStyle: { angle: -40, textAnchor: 'end' }
        }]}
        series=
        { showPercentage
          ? [ 
              { id: 'percentage', label: 'growth', dataKey: 'growth', curve: 'linear', showMark: false, valueFormatter: percentageFormatter }
            ]
          : [
              //{ id: 'opening', label: 'opening', dataKey: 'opening', curve: 'linear', showMark: false },
              { id: 'closing', label: 'closing price', dataKey: 'closing', curve: 'linear', showMark: false }
            ]
        }>
      </LineChart>
    </>
  );
}