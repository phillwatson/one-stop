import { useEffect, useState, useRef } from 'react';

import Grid from '@mui/material/Grid';
import Item from '@mui/material/Grid';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { debounce } from '@mui/material';

import dayjs, { Dayjs } from 'dayjs';
import 'dayjs/locale/en-gb';
import utc from 'dayjs/plugin/utc';
dayjs.extend(utc);

const now = dayjs.utc().startOf('day');
const tomorrow = now.add(1, 'day');

interface StaticRange {
  name: string;
  range: Dayjs[];
}
const StaticRanges: Array<StaticRange> = [
  { name: 'This Month',     range: [ now.date(1), tomorrow ] },
  { name: 'Last Month',     range: [ now.date(1).subtract(1, 'month'), now.date(1) ] },
  { name: 'This Year',      range: [ now.month(0).date(1), tomorrow ] },
  { name: 'Last Year',      range: [ now.month(0).date(1).subtract(1, 'year'), now.month(0).date(1) ] },
  { name: 'Past 30 Days',   range: [ now.subtract(30, 'day'), tomorrow ] },
  { name: 'Past 6 Months',  range: [ now.subtract(6, 'month'), tomorrow ] },
  { name: 'Past 12 Months', range: [ now.subtract(12, 'month'), tomorrow ] }
]

interface Props {
  rangeLabel?: string;
  fromDateLabel?: string;
  toDateLabel?: string;
  dateRange: Dayjs[];
  onSelect: (value: Dayjs[]) => void;
}

export default function DateRangeSelector(props: Props) {
  const prevSelectedRange = useRef<number>(-1);
  const [ selectedRange, setSelectedRange ] = useState<number>(0);
  useEffect(() => {
    if (selectedRange !== prevSelectedRange.current) {
      prevSelectedRange.current = selectedRange;
      props.onSelect(StaticRanges[selectedRange].range);
    }
  }, [ selectedRange, props ]);

  const debouncedSetDateRange = debounce((value: Dayjs[]) => { props.onSelect(value) }, 600);

  return (
    <LocalizationProvider dateAdapter={ AdapterDayjs } adapterLocale='en-gb'>
      <Grid container direction="row" spacing={ 3 } justifyContent="space-evenly">
        <Grid>
          <Item>
            <FormControl sx={{ width: { xs: 180, md: 220 }}}>
              <InputLabel id="range-label">{ props.rangeLabel || "Range" }</InputLabel>
              <Select labelId="select-range" id="select-range" label={ props.rangeLabel || "Range" }
                value={ selectedRange } onChange={ e => setSelectedRange(e.target.value as number) } >
                { StaticRanges.map((range, index) =>
                  <MenuItem key={ index } value={ index }>{ range.name }</MenuItem>
                )}
              </Select>
            </FormControl>
          </Item>
        </Grid>

        <Grid container direction="row" justifyContent="space-evenly">
          <Grid>
            <Item>
              <DatePicker disableFuture label={ props.fromDateLabel || "From Date (inclusive)" } value={ props.dateRange[0] }
                onChange={ (value: Dayjs | null, context: any) => {
                  if (value != null && context.validationError == null)
                    debouncedSetDateRange([ value, props.dateRange[1] ])
                  }}/>
            </Item>
          </Grid>
          <Grid>
            <Item>
              <DatePicker maxDate={ tomorrow } label={ props.toDateLabel || "To Date (exclusive)" } value={ props.dateRange[1] }
                onChange={ (value: Dayjs | null, context: any) => {
                  if (value != null && context.validationError == null)
                    debouncedSetDateRange([ props.dateRange[0], value ])
                  }}/>
            </Item>
          </Grid>
        </Grid>
      </Grid>
    </LocalizationProvider>
);
}