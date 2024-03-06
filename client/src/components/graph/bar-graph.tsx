import { useEffect, useState } from "react";

import { BarChart } from "@mui/x-charts/BarChart";

import { useNotificationDispatch } from "../../contexts/notification-context";
import AccountService from '../../services/account.service';
import ServiceErrorResponse from '../../model/service-error';
import { AccountDetail } from "../../model/account.model";

interface Props {
  account: AccountDetail
  fromDate: Date;
  toDate: Date;
}

export default function BarGraph(props: Props) {
  const showNotification = useNotificationDispatch();
  const [xAxis, setXAxis] = useState<Array<string>>([]);
  const [yAxis, setYAxis] = useState<Array<number>>([]);

  useEffect(() => {
    // create an initial range of dates with 0 values
    const range = new Map<string, number>();
    var date = new Date(props.fromDate);
    while (date <= props.toDate) {
      range.set(date.toLocaleDateString(), 0);
      date.setDate(date.getDate() + 1);
    }

    // fetch transactions and group the amounts by date
    AccountService.fetchTransactions(props.account.id, props.fromDate, props.toDate)
      .then( transactions => {
        const data = transactions.reduce((result, current) => {
          const date = new Date(current.date).toLocaleDateString();
          return result.set(date, (result.get(date) || 0) + current.amount);
        }, range)

        setXAxis(Array.from(data.keys()));
        setYAxis(Array.from(data.values()));
      })
      .catch(err => showNotification({ type: "add", level: "error", message: (err as ServiceErrorResponse).errors[0].message }));
  }, [props, showNotification]);

  if ((xAxis.length === 0) || (yAxis.length === 0)) return <div>Loading...</div>;
  return (
    <BarChart xAxis={[{
                        id: 'dates',
                        data: xAxis,
                        scaleType: 'band',
                      }]}
        series={[ { data: yAxis, } ]}
        height={ 300 }
        margin={{ top: 20, right: 20, bottom: 20, left: 100 }}
    />
  );
}