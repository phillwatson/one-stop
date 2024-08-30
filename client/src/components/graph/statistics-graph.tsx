import { useEffect, useMemo, useRef, useState } from 'react';

import Paper from '@mui/material/Paper/Paper';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import { PieChart } from '@mui/x-charts/PieChart';
import { PieValueType } from '@mui/x-charts/models/seriesType/pie';

import dayjs, { Dayjs } from 'dayjs';

import { useMessageDispatch } from '../../contexts/messages/context';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import CategoryService from '../../services/category.service';
import { CategoryGroup, CategoryStatistics } from '../../model/category.model';
import DateRangeSelector from './date-range-selector';

interface Props {
  elevation?: number;
  onCategorySelected: (category: CategoryStatistics, fromDate: Date, toDate: Date) => void;
}

interface CategoryStatisticsUI extends CategoryStatistics {
  selected: boolean;
};

// include statistic in each pie slice data
interface StatisticsPieValueType extends PieValueType {
  stat: CategoryStatistics
}

function toPieSlice(stat: CategoryStatistics, value: number): StatisticsPieValueType {
  const p: StatisticsPieValueType = {
    id: stat.categoryId || '',
    value: value,
    label: stat.categoryName,
    color: stat.colour,
    stat: stat
  };
  return p;
}

export default function StatisticsGraph(props: Props) {
  const showMessage = useMessageDispatch();
  const [ formatMoney ] = useMonetaryContext();

  const [ categoryGroups, setCategoryGroups ] = useState<CategoryGroup[]>([]);
  useEffect(() => {
    CategoryService.fetchAllGroups().then(response => setCategoryGroups(response))
  }, []);

  const [ selectedGroup, setSelectedGroup ] = useState<CategoryGroup>();
  const [ statistics, setStatistics ] = useState<CategoryStatisticsUI[]>([]);

  // allows the previous statistics to be accessed in the useEffect hook
  const statisticsRef = useRef<CategoryStatisticsUI[]>([]);
  statisticsRef.current = statistics;

  const [ dateRange, setDateRange ] = useState<Dayjs[]>([ dayjs(), dayjs() ]);

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

  /**
   * Construct pie slice data from the statistics. Recreate when money format changes.
   * @return an array of two arrays, the first for credit totals and the second for debit.
   */
  const pieSlices = useMemo<Array<Array<StatisticsPieValueType>>>(() => {
    return statistics
      .filter(stat => stat.selected)
      .map(stat => toPieSlice(stat, stat.total))
      .reduce((acc, slice) => {
        const index = slice.value > 0 ? 0 : 1;
        slice.value = Math.abs(slice.value);
        slice.label = `${slice.stat.categoryName} (${formatMoney(slice.stat.total, 'GBP')})`;
        acc[index].push(slice);

        return acc;
      }, Array.of([], []) as Array<Array<StatisticsPieValueType>>);
  }, [ statistics, formatMoney ])

  /**
   * Constructs the data for the pie series. Takes the current 'pieSlices' and calculates
   * the percentage of the chart that each series consumes.
   */
  const seriesData = useMemo<any>(() => {
    // find totals for both credit and debit
    const totalCredit = pieSlices[0].reduce((acc, slice) => acc + slice.value, 0);
    const totalDebit = pieSlices[1].reduce((acc, slice) => acc + slice.value, 0);

    var percentage = 0;
    var angle = 360;
    if (totalCredit > 0 && totalDebit > 0) {
      // find percentage difference between the two totals
      percentage = (totalCredit - totalDebit) * 100 / ((totalDebit > totalCredit) ? totalDebit : totalCredit);

      // find percentage of 360 degrees
      angle = 360 - (Math.abs(percentage) * 3.6);
    }

    return [
      {
        id: 0, data: pieSlices[0], highlightScope: { faded: 'global', highlighted: 'item' },
        outerRadius: pieSlices[1].length > 0 ? 100 : 200, innerRadius: 10, cornerRadius: 5, paddingAngle: 5,
        endAngle: (percentage < 0) ? angle : 360,
        faded: { innerRadius: 8, additionalRadius: -8, color: 'gray' },
        valueFormatter: (slice: StatisticsPieValueType) => ''
      },
      {
        id: 1, data: pieSlices[1], highlightScope: { faded: 'global', highlighted: 'item' },
        innerRadius: pieSlices[0].length > 0 ? 110 : 50, outerRadius: 200, cornerRadius: 5, paddingAngle: 2,
        endAngle: (percentage > 0) ? angle : 360,
        faded: { innerRadius: pieSlices[0].length > 0 ? 108 : 48, additionalRadius: -8, color: 'gray' },
        valueFormatter: (slice: StatisticsPieValueType) => ''
      }
    ]
  }, [ pieSlices ]);

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
    const data = seriesData[selectedSeries].data[selectedIndex];

    const stat = data.stat;
    if (stat !== selectedStat.current) {
      selectedStat.current = stat;
      if (stat !== undefined) {
        props.onCategorySelected(stat, dateRange[0].toDate(), dateRange[1].toDate());
      }
    }
  }

  return (
    <>
      <Paper sx={{ padding: 2 }} elevation={ props.elevation || 3 }>
        <Grid container direction="column" rowGap={ 3 } justifyContent="space-evenly">
          <Grid key={1} item>
            <FormControl fullWidth>
              <InputLabel id="select-group">Category Group</InputLabel>
              <Select labelId="select-group" label="Category Group" value={ selectedGroup?.id || "" } onChange={ selectGroup }>
                { categoryGroups.map(group =>
                  <MenuItem key={ group.id } value={ group.id }>{ group.name }</MenuItem>
                )}
              </Select>
            </FormControl>
          </Grid>

          <Grid key={2} item>
            <DateRangeSelector dateRange={ dateRange } onSelect={ setDateRange } />
          </Grid>
        </Grid>
      </Paper>

      <Paper sx={{ marginTop: 1, padding: 2 }} elevation={ props.elevation || 3 }>
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
                  label={ `${stat.categoryName} (${ formatMoney(stat.total, 'GBP')})` }
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
    </>
  );
}