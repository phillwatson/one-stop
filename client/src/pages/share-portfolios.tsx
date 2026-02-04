import { useNavigate } from 'react-router-dom';
import PortfoliosIcon from '@mui/icons-material/AssessmentOutlined';

import PageHeader from '../components/page-header/page-header';
import PortfolioList from '../components/share-portfolios/portfolio-list';
import { PortfolioSummaryResponse } from '../model/share-portfolio.model';

export default function SharePortfolios() {
  const navigate = useNavigate();

  function showPortfolio(portfolio?: PortfolioSummaryResponse) {
    if (portfolio) {
      navigate(`/shares/portfolios/${portfolio?.id}`);
    }
  }
  return (
    <PageHeader title='Your Portfolios' icon={ <PortfoliosIcon /> }>
      <PortfolioList onSelectPortfolio={ showPortfolio }/>
    </PageHeader>
  );
}