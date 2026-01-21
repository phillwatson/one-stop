import PortfoliosIcon from '@mui/icons-material/AssessmentOutlined';

import PageHeader from '../components/page-header/page-header';
import PortfolioList from '../components/share-portfolios/portfolio-list';

export default function SharePortfolios() {
  return (
    <PageHeader title='Your Portfolios' icon={ <PortfoliosIcon /> }>
      <PortfolioList />
    </PageHeader>
  );
}