import { useState } from 'react';
import dayjs, { Dayjs } from 'dayjs';

import DateRangeSelector from '../components/graph/date-range-selector';
import PageHeader from '../components/page-header/page-header';
import ShareIndexSelector from '../components/shares/share-index-selector';
import ShareIndexGraph from '../components/shares/index-price-graph';
import { ShareIndexResponse } from '../model/share-indices.model';
import Paper from '@mui/material/Paper';
import Grid from '@mui/material/Grid';

export default function SharePrices() {
  const [ dateRange, setDateRange ] = useState<Dayjs[]>([ dayjs(), dayjs() ]);
  const [ selectedIndex, setSelectedIndex ] = useState<ShareIndexResponse | undefined>(undefined);

  return (
    <PageHeader title='Share Prices'>
      <Paper sx={{ padding: 2 }} elevation={ 1 }>
        <Grid container direction="column" rowGap={ 3 } justifyContent="space-evenly">
          <Grid key={1}>
            <ShareIndexSelector onSelect={ setSelectedIndex } />
          </Grid>

          <Grid key={2}>
            <DateRangeSelector dateRange={ dateRange } onSelect={ setDateRange }/>
          </Grid>
        </Grid>
      </Paper>

      <Paper sx={{ marginTop: 2, padding: 2 }} elevation={ 1 }>
        <ShareIndexGraph shareIndex={ selectedIndex } fromDate={ dateRange[0].toDate() } toDate={ dateRange[1].toDate() }/>
      </Paper>
    </PageHeader>
  )
}