import { useEffect, useMemo, useRef, useState } from 'react';

import Paper from '@mui/material/Paper/Paper';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
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

interface CategoryStatisticsUI extends CategoryStatistics {
  selected: boolean;
};

export default function StatisticsGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [ statistics, setStatistics ] = useState<CategoryStatisticsUI[]>([]);

  // allows the previous statistics to be accessed in the useEffect hook
  const statisticsRef = useRef<CategoryStatisticsUI[]>([]);
  statisticsRef.current = statistics;

  const [ dateRange, setDateRange ] = useState<Dayjs[]>([ dayjs().subtract(1, 'month'), dayjs().add(1, 'day') ]);
  const debouncedSetDateRange = debounce((value: Dayjs[]) => { setDateRange(value) }, 500);

  const debitData = useMemo<Array<PieValueType>>(() =>
    statistics
      .filter(stat => stat.selected)
      .filter(stat => stat.debit > 0)
      .map(stat => {
        const p: PieValueType = {
          id: stat.categoryId || '',
          value: stat.debit,
          label: stat.category,
          color: stat.colour
        };
        return p;
      }), [ statistics ]);

  const creditData = useMemo<Array<PieValueType>>(() =>
    statistics
      .filter(stat => stat.selected)
      .filter(stat => stat.credit > 0)
      .map(stat => {
        const p: PieValueType = {
          id: stat.categoryId || '',
          value: stat.credit,
          label: stat.category,
          color: stat.colour
        };
        return p;
      }), [ statistics ]);
    
  useEffect(() => {
    if (dateRange[0] < dateRange[1]) {
      CategoryService.getCategoryStatistics(dateRange[0].toDate(), dateRange[1].toDate())
        .then(response => {
          const newStats = response.map(s => {
            const oldStat = statisticsRef.current.find(stat => stat.categoryId === s.categoryId);
            const ui: CategoryStatisticsUI = {...s, selected: oldStat === undefined || oldStat.selected }
            return ui;
          });
          setStatistics(newStats);
        })
        .catch(err => showMessage(err));
    } else {
      showMessage({ type: 'add', level: 'info', text: 'The "from" date must be before the "to" date.' });
    }
  }, [ showMessage, dateRange ]);

  function selectCategory(selectedSeries: string, selectedIndex: number) {
    var categoryId: string | undefined = selectedSeries === 'credit'
      ? creditData[selectedIndex].id as string
      : debitData[selectedIndex].id as string;
    if (categoryId === '') categoryId = undefined;

    const stat = statistics.find(stat => stat.categoryId === categoryId);
    if (stat !== undefined) {
      props.onCategorySelected(stat, dateRange[0].toDate(), dateRange[1].toDate());
    }
  }

  function toggleCategory(categoryId: string | undefined, selected: boolean) {
    setStatistics(statistics.map(stat => {
      if (stat.categoryId === categoryId) {
        stat.selected = selected;
      }
      return stat;
    }));
  }

  return (
    <LocalizationProvider dateAdapter={ AdapterDayjs } adapterLocale={ 'en-gb' }>
      <Paper sx={{ margin: 1, padding: 2 }} elevation={3}>
        <Grid container spacing={7} alignItems={"center"} justifyContent={"center"}>
          <Grid key={1} item>
            <DatePicker disableFuture label="From Date (inclusive)" value={ dayjs(dateRange[0]) }
              onChange={ (value: any, context: any) => {
                if (value != null && context.validationError == null)
                  debouncedSetDateRange([ value, dateRange[1]])
                }}/>
          </Grid>

          <Grid key={2} item>
            <DatePicker maxDate={ dayjs().add(1, 'day') } label="To Date (exclusive)" value={ dayjs(dateRange[1]) }
              onChange={ (value: any, context: any) => {
                if (value != null && context.validationError == null)
                  debouncedSetDateRange([dateRange[0], value])
                }}/>
          </Grid>
        </Grid>
      </Paper>
      <Paper sx={{ margin: 1, padding: 2 }} elevation={3}>
        <Grid container spacing={4} alignItems={"center"} justifyContent={"center"}>
          <Grid item>
            <PieChart height={ 500 } width={ 400 } margin={{ top: 50, right: 50, bottom: 50, left: 50 }}
              slotProps={{ legend: { hidden: true } }}
              series={[
                {
                  id: 'credit',
                  data: creditData,
                  innerRadius: 10, outerRadius: 100, cornerRadius: 5, paddingAngle: 5,
                  highlightScope: { faded: 'global', highlighted: 'item' },
                  faded: { innerRadius: 8, additionalRadius: -8, color: 'gray' }
                },
                {
                  id: 'debit',
                  data: debitData,
                  innerRadius: 110, outerRadius: 200, cornerRadius: 5, paddingAngle: 2,
                  highlightScope: { faded: 'global', highlighted: 'item' },
                  faded: { innerRadius: 108, additionalRadius: -8, color: 'gray' }
                }
              ]}              
              onClick={(event: any, slice: any) => selectCategory(slice.seriesId, slice.dataIndex) }
            />
          </Grid>
          <Grid item>
            <Stack margin={ 2 }>
              { statistics.map(stat =>
                <FormControlLabel key={ stat.category } label={ stat.category }
                  control= {
                    <Switch key={ stat.category } name={ stat.category } checked={ stat.selected }
                      style={{ color: stat.selected ? stat.colour : undefined }}
                      onChange={ e => toggleCategory(stat.categoryId, e.target.checked) }/>
                  }
                />
                )
              }
            </Stack>
          </Grid>
        </Grid>
      </Paper>
    </LocalizationProvider>
  );
}