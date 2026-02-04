import { useEffect, useMemo, useState } from 'react';
import dayjs, { Dayjs } from 'dayjs';
import Paper from '@mui/material/Paper';
import Grid from '@mui/material/Grid';
import SharePricesIcon from '@mui/icons-material/QueryStats';

import DateRangeSelector from '../components/graph/date-range-selector';
import PageHeader from '../components/page-header/page-header';
import ShareIndexSelector from '../components/shares/share-index-selector';
import ShareIndexGraph from '../components/shares/index-price-graph';
import ShareService from "../services/share.service";
import { ShareIndexResponse } from '../model/share-indices.model';
import MultiShareIndexSelector from '../components/shares/multi-share-index-selector';
import { useMessageDispatch } from '../contexts/messages/context';

const EMPTY_ARRAY = [] as ShareIndexResponse[];

export default function SharePrices() {
  const showMessage = useMessageDispatch();
  const [ shareIndices, setShareIndices ] = useState<ShareIndexResponse[]>([]);
  const [ dateRange, setDateRange ] = useState<Dayjs[]>([ dayjs(), dayjs() ]);
  const [ selectedIndex, setSelectedIndex ] = useState<ShareIndexResponse | undefined>(undefined);
  const [ comparisons, setComparisons ] = useState<ShareIndexResponse[]>([]);

  const exlusions = useMemo(() => {
    return (selectedIndex !== undefined) ? [ selectedIndex ] : EMPTY_ARRAY;
  }, [ selectedIndex ]);

  useEffect(() => {
    ShareService.fetchAllIndices()
      .then(response => setShareIndices(response))
      .catch(err => {
        showMessage(err);
      })
  }, [ showMessage ]);

  return (
    <PageHeader title='Share Prices' icon={ <SharePricesIcon />}>
      <Paper sx={{ padding: 2 }} elevation={ 1 }>
        <Grid container direction="column" rowGap={ 3 } justifyContent="space-evenly">
          <ShareIndexSelector shareIndices={ shareIndices } onSelect={ setSelectedIndex } />
          <DateRangeSelector dateRange={ dateRange } onSelect={ setDateRange }/>
        </Grid>
      </Paper>

      <Paper sx={{ marginTop: 2, padding: 2 }} elevation={ 1 }>
        <MultiShareIndexSelector label={ 'Select Comparisons' }
          shareIndices={ shareIndices }
          excludedIndices={ exlusions }
          onSelectIndices={ setComparisons } />
      </Paper>

      <Paper sx={{ marginTop: 2, padding: 2 }} elevation={ 1 }>
        <ShareIndexGraph
          shareIndex={ selectedIndex }
          compareWith={ comparisons }
          fromDate={ dateRange[0].toDate() }
          toDate={ dateRange[1].toDate() }/>
      </Paper>
    </PageHeader>
  )
}