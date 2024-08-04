import { useEffect, useMemo, useRef, useState } from 'react';

import Paper from '@mui/material/Paper/Paper';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import { FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { PieChart } from '@mui/x-charts/PieChart';
import { PieValueType } from '@mui/x-charts/models/seriesType/pie';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { Dayjs } from 'dayjs';
import 'dayjs/locale/en-gb';

import { useMessageDispatch } from '../../contexts/messages/context';
import CurrencyService from '../../services/currency.service';
import CategoryService from '../../services/category.service';
import { CategoryGroup, CategoryStatistics } from '../../model/category.model';

var debounce = require('lodash/debounce');

interface Props {
  onCategorySelected: (category: CategoryStatistics, fromDate: Date, toDate: Date) => void;
}

interface CategoryStatisticsUI extends CategoryStatistics {
  selected: boolean;
};

function toPieSlice(stat: CategoryStatistics, value: number): PieValueType {
  const p: PieValueType = {
    id: stat.categoryId || '',
    value: value,
    label: stat.categoryName,
    color: stat.colour
  };
  return p;
}

export default function StatisticsGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [ categoryGroups, setCategoryGroups ] = useState<CategoryGroup[]>([]);
  const [ selectedGroup, setSelectedGroup ] = useState<CategoryGroup>();
  const [ statistics, setStatistics ] = useState<CategoryStatisticsUI[]>([]);

  // allows the previous statistics to be accessed in the useEffect hook
  const statisticsRef = useRef<CategoryStatisticsUI[]>([]);
  statisticsRef.current = statistics;

  const [ dateRange, setDateRange ] = useState<Dayjs[]>([ dayjs().subtract(1, 'month'), dayjs().add(1, 'day') ]);
  const debouncedSetDateRange = debounce((value: Dayjs[]) => { setDateRange(value) }, 600);

  useEffect(() => {
    CategoryService.fetchAllGroups().then(response => setCategoryGroups(response))
  }, []);

  useEffect(() => {
    if (selectedGroup === undefined) {
      return;
    }
    
    if (dateRange[0] < dateRange[1]) {
      CategoryService.getCategoryStatistics(selectedGroup.id!!, dateRange[0].toDate(), dateRange[1].toDate())
        .then(response => {
          const newStats = response.map(s => {
            // reflect the previous selection state in the new statistics
            const oldStat = statisticsRef.current.find(stat => stat.categoryId === s.categoryId);
            const ui: CategoryStatisticsUI = {...s, selected: oldStat === undefined || oldStat.selected }
            return ui;
          });
          setStatistics(newStats);
        })
        .catch(err => 
          showMessage(err)
        );
    } else {
      showMessage({ type: 'add', level: 'info', text: 'The "from" date must be before the "to" date.' });
    }
  }, [ showMessage, selectedGroup, dateRange ]);

  function selectGroup(event: any) {
    const groupId = event.target.value;
    setSelectedGroup(categoryGroups.find(group => group.id === groupId));
  }

  const seriesData = useMemo<any>(() => {
    // an array of two arrays, the first for credit totals and the second for debit.
    const series = statistics
      .filter(stat => stat.selected)
      .map(stat => toPieSlice(stat, stat.total))
      .reduce((acc, slice) => {
        const index = slice.value > 0 ? 0 : 1;
        slice.value = Math.abs(slice.value);
        acc[index].push(slice);

        return acc;
      }, Array.of([], []) as Array<Array<PieValueType>>);

      // find totals for both credit and debit
      const totalCredit = series[0].reduce((acc, slice) => acc + slice.value, 0);
      const totalDebit = series[1].reduce((acc, slice) => acc + slice.value, 0);

      var percentage = 0;
      var angle = 360;
      if (totalCredit > 0 && totalDebit > 0) {
        // find percentage difference between the two totals
        percentage = (totalCredit - totalDebit) * 100 / ((totalDebit > totalCredit) ? totalDebit : totalCredit);

        // find percentage of 360 degrees
        angle = 360 - (Math.abs(percentage) * 3.6);
        //console.log(`debit: ${totalDebit}, credit: ${totalCredit}, percentage: ${percentage}, angle: ${angle}`);
      }

      return [
        {
          id: 0, data: series[0], highlightScope: { faded: 'global', highlighted: 'item' },
          outerRadius: series[1].length > 0 ? 100 : 200, innerRadius: 10, cornerRadius: 5, paddingAngle: 5,
          endAngle: (percentage < 0) ? angle : 360,
          faded: { innerRadius: 8, additionalRadius: -8, color: 'gray' }
        },
        {
          id: 1, data: series[1], highlightScope: { faded: 'global', highlighted: 'item' },
          innerRadius: series[0].length > 0 ? 110 : 50, outerRadius: 200, cornerRadius: 5, paddingAngle: 2,
          endAngle: (percentage > 0) ? angle : 360,
          faded: { innerRadius: series[0].length > 0 ? 108 : 48, additionalRadius: -8, color: 'gray' }
        }
      ]
  }, [ statistics ]);

  function toggleCategory(categoryId: string | undefined, selected: boolean) {
    setStatistics(statistics.map(stat => {
      if (stat.categoryId === categoryId) {
        stat.selected = selected;
      }
      return stat;
    }));
  }

  // record the selected category so we can tell when the selection changes
  const selectedStat = useRef<CategoryStatisticsUI | undefined>();

  function selectCategory(selectedSeries: number, selectedIndex: number) {
    var categoryId: string | undefined = seriesData[selectedSeries].data[selectedIndex].id as string;
    if (categoryId === '') categoryId = undefined; // uncategorised transactions

    const stat = statistics.find(stat => stat.categoryId === categoryId);
    if (stat !== selectedStat.current) {
      selectedStat.current = stat;
      if (stat !== undefined) {
        props.onCategorySelected(stat, dateRange[0].toDate(), dateRange[1].toDate());
      }
    }
  }

  return (
    <LocalizationProvider dateAdapter={ AdapterDayjs } adapterLocale={ 'en-gb' }>
      <Paper sx={{ margin: 1, padding: 2 }} elevation={3}>
        <Grid container direction="column" rowGap={3} alignItems={"stretch"}>
          <Grid container direction="row" justifyContent={"center"}>
            <FormControl fullWidth>
              <InputLabel id="select-group">Category Group</InputLabel>
              <Select labelId="select-group" label="Category Group" value={ selectedGroup?.id || "" } onChange={ selectGroup }>
                { categoryGroups.map(group =>
                  <MenuItem key={ group.id } value={ group.id }>{ group.name }</MenuItem>
                )}
              </Select>
            </FormControl>
          </Grid>

          <Grid container direction="row" columnGap={7} rowGap={3} justifyContent={"center"}>
            <Grid key={2} item>
              <DatePicker disableFuture label="From Date (inclusive)" value={ dayjs(dateRange[0]) }
                onChange={ (value: any, context: any) => {
                  if (value != null && context.validationError == null)
                    debouncedSetDateRange([ value, dateRange[1] ])
                  }}/>
            </Grid>
            <Grid key={3} item>
              <DatePicker maxDate={ dayjs().add(1, 'day') } label="To Date (exclusive)" value={ dayjs(dateRange[1]) }
                onChange={ (value: any, context: any) => {
                  if (value != null && context.validationError == null)
                    debouncedSetDateRange([ dateRange[0], value ])
                  }}/>
            </Grid>
          </Grid>
        </Grid>
      </Paper>

      <Paper sx={{ margin: 1, padding: 2 }} elevation={3}>
        <Grid container spacing={2} alignItems={"center"} justifyContent={"center"}>
          <Grid item>
            <PieChart height={ 450 } width={ 400 } margin={{ top: 0, right: 8, bottom: 0, left: 8 }}
              slotProps={{ legend: { hidden: true } }}
              series={ seriesData }
              onItemClick={(event: any, slice: any) => selectCategory(slice.seriesId, slice.dataIndex) }
            />
          </Grid>

          <Grid item>
            <Stack margin={ 0 } marginLeft={ 2 } marginTop={ 0 }>
              { statistics.map(stat =>
                <FormControlLabel key={ stat.categoryName }
                  label={ `${stat.categoryName} (${ CurrencyService.format(stat.total, 'GBP')})` }
                  style={{ padding: 0, margin: 0 }}
                  control= {
                    <Switch key={ stat.categoryName } name={ stat.categoryName } checked={ stat.selected }
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