import { useCallback, useEffect, useMemo, useState } from 'react';

import Paper from '@mui/material/Paper';
import { ChartsReferenceLine } from '@mui/x-charts/ChartsReferenceLine';
import { BarChart } from '@mui/x-charts/BarChart';
import { YAxis } from '@mui/x-charts/models/axis';

import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { useMessageDispatch } from '../../contexts/messages/context';
import AccountService from '../../services/account.service';
import { AccountDetail } from '../../model/account.model';
import DateRangeSelector from './date-range-selector';

import dayjs, { Dayjs, ManipulateType } from 'dayjs';
import utc from 'dayjs/plugin/utc';
dayjs.extend(utc);

interface Props {
  account: AccountDetail
  elevation?: number;
}

interface Data {
  credits: number;
  debits: number;
}

export default function BarGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [ formatMoney ] = useMonetaryContext();

  const formatValue = useCallback((value: number | null) => {
    return value === null ? "" : formatMoney(value, 'GBP');
  }, [ formatMoney ])

  const series = useMemo(() => {
    return [
      { dataKey: 'credits', stack: 'a', color: '#00BF00', label: 'credits', valueFormatter: formatValue },
      { dataKey: 'debits',  stack: 'a', color: '#BF0000', label: 'debits',  valueFormatter: formatValue }
    ];
  }, [ formatValue ]);

  const yAxisConfig = useMemo<YAxis[]>(() => {
    return [{
      id: 'amount',
      label: 'Amount',
      width: 100,
      scaleType: 'linear',
      valueFormatter: formatValue
    }]
  }, [ formatValue ]);

  const [xAxis, setXAxis] = useState<Array<number>>([]);
  const [yAxis, setYAxis] = useState<Array<any>>([]);

  const [ dateRange, setDateRange ] = useState<Dayjs[]>([  ]);

  useEffect(() => {
    if (dateRange.length > 0) {
    // fetch transactions and group the amounts by date
    const fromDate = dateRange[0].toDate();
    const toDate = dateRange[1].toDate()

    AccountService.getTransactionMovements(props.account.id, fromDate, toDate)
      .then( movements => {
          const units: ManipulateType = (dateRange[1].diff(dateRange[0], 'day') <= 65)
            ? 'day' : 'week';

          const range = new Map<number, Data>();
          let date = dateRange[0];
          while (date < dateRange[1]) {
            range.set(date.unix(), { credits: 0, debits: 0 });
            date = date.add(1, units);
          }

          const keys = Array.from(range.keys()).sort();

          movements.reverse().forEach(movement => {
            const from = dayjs.utc(movement.fromDate).unix();
            let key = keys.find(k => k >= from) || keys[keys.length - 1];
            let entry = range.get(key)!;
            entry.debits  += movement.debits.amount;
            entry.credits += movement.credits.amount;
          });

          setXAxis(keys);
          setYAxis(Array.from(range.values()));
        })
        .catch(err => showMessage(err));
      }
  }, [ props, showMessage, dateRange ]);

  return (
    <>
      <Paper sx={{ marginTop: 1, padding: 2 }} elevation={ props.elevation || 3 }>
        <DateRangeSelector dateRange={ dateRange } onSelect={ setDateRange } />
      </Paper>
    
      <Paper sx={{ marginTop: 1 }} elevation={ props.elevation || 3 }>
        <BarChart
            dataset={ yAxis }
            series={ series }
            yAxis={ yAxisConfig }
            xAxis={ [{
              id: 'dates', data: xAxis, scaleType: 'band', label: 'Date', height: 95,
              valueFormatter: (v: number) => dayjs.unix(v).format('DD/MM/YYYY'),
              tickLabelStyle: { angle: -40, textAnchor: 'end' },
              tickInterval: (_, index) => index % 2 === 0
            }] }
            height={ 450 }
            margin={{ top: 50, right: 10, bottom: 10, left: 22 }}
        >
          <ChartsReferenceLine
              y={ 0 }
              lineStyle={{ strokeDasharray: '6 10' }}
              labelAlign="start"
            />
        </BarChart>
      </Paper>
    </>
  );
}