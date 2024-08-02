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

  // seriesData is an array of two arrays, the first for credit totals and the second for debit.
  const seriesData = useMemo<Array<Array<PieValueType>>>(() =>
    statistics
      .filter(stat => stat.selected)
      .map(stat => toPieSlice(stat, stat.total))
      .reduce((acc, slice) => {
        const index = slice.value > 0 ? 0 : 1;
        slice.value = Math.abs(slice.value);
        acc[index].push(slice);

        return acc;
      }, Array.of([], []) as Array<Array<PieValueType>>
  ), [ statistics ]);

  function selectCategory(selectedSeries: number, selectedIndex: number) {
    var categoryId: string | undefined = seriesData[selectedSeries][selectedIndex].id as string;
    if (categoryId === '') categoryId = undefined; // uncategorised transactions

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

          <Grid container direction="row" columnGap={7} justifyContent={"center"}>
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
        <Grid container spacing={4} alignItems={"center"} justifyContent={"center"}>
          <Grid item>
            <PieChart height={ 500 } width={ 400 } margin={{ top: 50, right: 50, bottom: 50, left: 50 }}
              slotProps={{ legend: { hidden: true } }}
              series={[
                {
                  id: 0, data: seriesData[0],
                  innerRadius: 10, outerRadius: seriesData[1].length > 0 ? 100 : 200, cornerRadius: 5, paddingAngle: 5,
                  highlightScope: { faded: 'global', highlighted: 'item' },
                  faded: { innerRadius: 8, additionalRadius: -8, color: 'gray' }
                },
                {
                  id: 1, data: seriesData[1],
                  innerRadius: seriesData[0].length > 0 ? 110 : 50, outerRadius: 200, cornerRadius: 5, paddingAngle: 2,
                  highlightScope: { faded: 'global', highlighted: 'item' },
                  faded: { innerRadius: seriesData[0].length > 0 ? 108 : 48, additionalRadius: -8, color: 'gray' }
                }
              ]}
              onClick={(event: any, slice: any) => selectCategory(slice.seriesId, slice.dataIndex) }
            />
          </Grid>

          <Grid item>
            <Stack margin={ 2 }>
              { statistics.map(stat =>
                <FormControlLabel key={ stat.categoryName } label={ stat.categoryName }
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