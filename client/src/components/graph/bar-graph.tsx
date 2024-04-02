import { useEffect, useState } from "react";

import { ChartsReferenceLine } from '@mui/x-charts/ChartsReferenceLine';
import { BarChart } from "@mui/x-charts/BarChart";

import { useMessageDispatch } from "../../contexts/messages/context";
import AccountService from '../../services/account.service';
import { AccountDetail } from "../../model/account.model";
import { formatDate } from "../../util/date-util";

interface Props {
  account: AccountDetail
  fromDate: Date;
  toDate: Date;
}

export default function BarGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [xAxis, setXAxis] = useState<Array<string>>([]);
  const [yAxis, setYAxis] = useState<Array<{
    credits: number;
    debits: number;
  }>>([]);

  useEffect(() => {
    // fetch transactions and group the amounts by date
    AccountService.fetchTransactions(props.account.id, props.fromDate, props.toDate)
      .then( transactions => {
        // create an initial range of dates with 0 values
        const range = new Map<string, any>();
        var date = new Date(props.fromDate);
        while (date <= props.toDate) {
          range.set(date.toLocaleDateString(), { credits: 0, debits: 0 });
          date.setDate(date.getDate() + 1);
        }

        transactions.forEach(transaction => {
          const date = formatDate(transaction.bookingDateTime);
          const entry = range.get(date);
          (transaction.amount < 0) ? entry.debits += transaction.amount : entry.credits += transaction.amount;
        })

        setXAxis(Array.from(range.keys()));
        setYAxis(Array.from(range.values()));
      })
      .catch(err => showMessage(err));
  }, [props, showMessage]);

  if ((xAxis.length === 0) || (yAxis.length === 0))
    return <>Loading...</>;

  return (
    <>
      <BarChart
          dataset={ yAxis }
          series={[
            { dataKey: 'credits', stack: 'a', color: '#00BF00', label: 'credits' },
            { dataKey: 'debits',  stack: 'a', color: '#BF0000', label: 'debits' }
          ]}
          yAxis={[{ id: 'amount', scaleType: 'linear' }]}
          xAxis={[{ id: 'dates', data: xAxis, scaleType: 'band', tickLabelInterval: (_, index) => index % 2 === 0 }]}
          height={ 450 }
          margin={{ top: 50, right: 10, bottom: 70, left: 120 }}
          bottomAxis={{
            axisId: 'dates',
            tickLabelStyle: {
              angle: -40,
              textAnchor: 'end',
              fontSize: 12,
            }
          }}
      >
        <ChartsReferenceLine
            y={ 0 }
            lineStyle={{ strokeDasharray: '6 10' }}
            labelAlign="start"
          />
      </BarChart>
    </>
  );
}