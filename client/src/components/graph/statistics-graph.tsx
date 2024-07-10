import { useEffect, useState } from "react";

import { PieChart } from "@mui/x-charts/PieChart";
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import dayjs from 'dayjs';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { PieValueType } from "@mui/x-charts/models/seriesType/pie";
import Paper from "@mui/material/Paper/Paper";
import Grid from "@mui/material/Grid";

import { useMessageDispatch } from "../../contexts/messages/context";
import CategoryService from '../../services/category.service';

const DEFAULT_COLOUR = '#dee0da';
const DEFAULT_FROM_DATE = new Date();
DEFAULT_FROM_DATE.setDate(DEFAULT_FROM_DATE.getDate() - 30);

export default function StatisticsGraph() {
  const showMessage = useMessageDispatch();
  const [ data, setData ] = useState<Array<PieValueType>>([]);
  const [ fromDate, setFromDate ] = useState<Date>(DEFAULT_FROM_DATE);
  const [ toDate, setToDate ] = useState<Date>(new Date());

  useEffect(() => {
    if (fromDate < toDate) {
      CategoryService.getCategoryStatistics(fromDate, toDate)
        .then( statistics => {
          setData(statistics.map(stat => {
              const p: PieValueType = {
                id: stat.categoryId || "1",
                value: Math.abs(stat.total),
                label: stat.category,
                color: stat.colour || DEFAULT_COLOUR
              };
              return p;
            })
          )
        })
        .catch(err => showMessage(err));
    } else {
      showMessage({ type: 'add', level: 'info', text: 'The "from" date must be before the "to" date.' });
    }
  }, [ showMessage, fromDate, toDate ]);

  return (
    <>
      <LocalizationProvider dateAdapter={ AdapterDayjs }>
        <Paper elevation={3} sx={{ padding: 2 }}>
          <Grid container spacing={3}>
            <Grid item key={1} xs={6}>
              <DatePicker label="From Date" value={ dayjs(fromDate) } onChange={ (e) => { if (e) setFromDate(e.toDate()) }}/>
            </Grid>
            <Grid item key={2} xs={6}>
              <DatePicker label="To Date" value={ dayjs(toDate) } onChange={ (e) => { if (e) setToDate(e.toDate()) }}/>
            </Grid>
          </Grid>
        </Paper>
      </LocalizationProvider>
      <PieChart
        series={[
          {
            data: data,
            innerRadius: 20,
            outerRadius: 200,
            paddingAngle: 5,
            cornerRadius: 5,
            highlightScope: { faded: 'global', highlighted: 'item' },
            faded: { innerRadius: 15, additionalRadius: -10, color: 'gray', arcLabelRadius: 130 },
            cx: 150,
            cy: 200
          }
        ]}
        height={ 450 }
        margin={{ top: 50, right: 10, bottom: 70, left: 120 }}
      />
    </>
  );
}