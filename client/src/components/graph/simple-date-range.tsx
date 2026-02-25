import dayjs, { Dayjs } from 'dayjs';
import utc from 'dayjs/plugin/utc';
import ButtonGroup from '@mui/material/ButtonGroup';
import Button from '@mui/material/Button';
import { useEffect, useState } from 'react';

dayjs.extend(utc);

interface Props {
  dateRange: Dayjs[];
  onSelect: (value: Dayjs[]) => void;
}

export enum Range {
  ONE_WEEK = '1W',
  ONE_MONTH = '1M',
  SIX_MONTHS = '6M',
  YTD = 'YTD',
  ONE_YEAR = '1Y',
  FIVE_YEARS = '5Y'
}

const ALL_RANGES: Range[] = Object.keys(Range).map((name: string) => Range[name as keyof typeof Range]);

export default function SimpleDateRange({ onSelect }: Props) {
  const [selectedRange, setSelectedRange] = useState<Range | Range.ONE_YEAR>();

  useEffect(() => setSelectedRange(Range.ONE_YEAR), []);

  useEffect(() => {
    if ((! onSelect) || (selectedRange === undefined)) {
      return;
    }

    let toDate = dayjs().startOf('day');
    let fromDate = toDate;
    switch (selectedRange) {
      case Range.ONE_WEEK:    fromDate = toDate.subtract(7, 'days'); break;
      case Range.ONE_MONTH:   fromDate = toDate.subtract(1, 'month'); break;
      case Range.SIX_MONTHS:  fromDate = toDate.subtract(6, 'month'); break;
      case Range.YTD:         fromDate = toDate.date(1).month(0); break;
      case Range.ONE_YEAR:    fromDate = toDate.subtract(1, 'year'); break;
      case Range.FIVE_YEARS:  fromDate = toDate.subtract(5, 'year'); break;
    }

    fromDate = fromDate.startOf('day');
    let dow = fromDate.day();
    while ((dow === 0) || (dow === 6)) {
      fromDate = fromDate.subtract(1, 'day');
      dow = fromDate.day();
    }

    onSelect([ fromDate, toDate ]);
  }, [ onSelect, selectedRange ]);

  return (
    <ButtonGroup variant='outlined'>
      { ALL_RANGES.map(range => 
        <Button
          variant={ selectedRange === range ? 'contained' : 'outlined' } 
          disabled={ selectedRange === range }
          onClick={() => setSelectedRange(range)}>{ range.toString() }</Button>
      )}
    </ButtonGroup>
  );
}