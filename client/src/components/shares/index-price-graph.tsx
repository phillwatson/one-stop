import { useEffect, useMemo, useState } from "react";

import { LineChart } from "@mui/x-charts";

import { useMessageDispatch } from '../../contexts/messages/context';
import ShareService from "../../services/share.service";
import { ShareIndexResponse, HistoricalPriceResponse } from "../../model/share-indices.model";
import { toDate, formatDate } from "../../util/date-util";

interface Props {
  shareIndex?: ShareIndexResponse;
  fromDate: Date;
  toDate: Date;
}

export default function ShareIndexGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [ prices, setPrices ] = useState<Array<HistoricalPriceResponse>>([]);

  const dataset = useMemo(
    () => prices
    .map(p => ({
        date: toDate(p.date),
        opening: p.open,
        closing: p.close
    })),
    [ prices ]
  )

  useEffect(() => {
    if (props.shareIndex) {
      ShareService.getIndexPrices(props.shareIndex!.id, props.fromDate, props.toDate, 0, 1000)
        .then( response => setPrices(response.items) )
        .catch(err => {
          showMessage(err);
        })
    }
  }, [ showMessage, props ]);

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