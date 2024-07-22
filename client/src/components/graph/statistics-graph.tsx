import { useEffect, useState } from 'react';

import Paper from '@mui/material/Paper/Paper';
import Grid from '@mui/material/Grid';
import { PieChart } from '@mui/x-charts/PieChart';
import { PieValueType } from '@mui/x-charts/models/seriesType/pie';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import dayjs, { Dayjs } from 'dayjs';
import 'dayjs/locale/en-gb';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';

import { useMessageDispatch } from '../../contexts/messages/context';
import CategoryService from '../../services/category.service';
import { CategoryStatistics } from '../../model/category.model';

var debounce = require('lodash/debounce');

interface Props {
  onCategorySelected: (category: CategoryStatistics, fromDate: Date, toDate: Date) => void;
}

export default function StatisticsGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [ statistics, setStatistics ] = useState<CategoryStatistics[]>([]);
  const [ data, setData ] = useState<Array<PieValueType>>([]);
  const [ dateRange, setDateRange ] = useState<Dayjs[]>([ dayjs().subtract(1, 'month'), dayjs().add(1, 'day') ]);
  const debouncedSetDateRange = debounce((value: Dayjs[]) => { setDateRange(value) }, 500);

  useEffect(() => {
    setData(statistics.map(stat => {
        const p: PieValueType = {
          id: stat.categoryId || "1",
          value: Math.abs(stat.total),
          label: stat.category,
          color: stat.colour
        };
        return p;
      }));
  }, [ statistics ]);

  useEffect(() => {
    if (dateRange[0] < dateRange[1]) {
      CategoryService.getCategoryStatistics(dateRange[0].toDate(), dateRange[1].toDate())
        .then( response => setStatistics(response) )
        .catch(err => showMessage(err));
    } else {
      showMessage({ type: 'add', level: 'info', text: 'The "from" date must be before the "to" date.' });
    }
  }, [ showMessage, dateRange ]);

  function selectCategory(selectedIndex: number) {
    props.onCategorySelected(statistics[selectedIndex], dateRange[0].toDate(), dateRange[1].toDate());
  }

  return (
    <>
      <LocalizationProvider dateAdapter={ AdapterDayjs } adapterLocale={ 'en-gb' }>
        <Paper sx={{ margin: 1, padding: 2 }} elevation={3}>
          <Grid container columnSpacing={7} rowSpacing={3} justifyContent={"center"}>
            <Grid key={1} item>
              <DatePicker disableFuture
                label="From Date (inclusive)"
                value={ dayjs(dateRange[0]) }
                onChange={ (value: any, context: any) => {
                  if (value != null && context.validationError == null)
                    debouncedSetDateRange([ value, dateRange[1]])
                  }}/>
            </Grid>

            <Grid key={2} item>
              <DatePicker maxDate={ dayjs().add(1, 'day') }
                label="To Date (exclusive)"
                value={ dayjs(dateRange[1]) }
                onChange={ (value: any, context: any) => {
                  if (value != null && context.validationError == null)
                    debouncedSetDateRange([dateRange[0], value])
                  }}/>
            </Grid>
          </Grid>
        </Paper>
        <Paper sx={{ margin: 1, padding: 2 }} elevation={3}>
          <PieChart
            series={[
              {
                data: data,
                innerRadius: 50,
                outerRadius: 200,
                paddingAngle: 5,
                cornerRadius: 5,
                highlightScope: { faded: 'global', highlighted: 'item' },
                faded: { innerRadius: 55, additionalRadius: -10, color: 'gray', arcLabelRadius: 130 },
                cx: 150,
                cy: 200
              }
            ]}
            height={ 500 }
            margin={{ top: 50, right: 10, bottom: 70, left: 120 }}
            onClick={(event: any, data: any) => { selectCategory(data.dataIndex) }}
          />
        </Paper>
      </LocalizationProvider>
    </>
  );
}