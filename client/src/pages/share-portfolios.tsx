import Paper from '@mui/material/Paper';

import PageHeader from '../components/page-header/page-header';
import PortfolioList from '../components/share-portfolios/portfolio-list';

export default function SharePortfolios() {
  return (
    <PageHeader title='Your Portfolios'>
      <Paper sx={{ padding: 2 }} elevation={ 1 }>
        <PortfolioList />
      </Paper>
    </PageHeader>
  );
}