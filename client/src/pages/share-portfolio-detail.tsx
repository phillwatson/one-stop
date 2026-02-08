import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import PortfoliosIcon from '@mui/icons-material/AssessmentOutlined';

import { useMessageDispatch } from '../contexts/messages/context';
import PortfolioService from '../services/portfolio.service';
import { PortfolioResponse } from '../model/share-portfolio.model';
import PageHeader from '../components/page-header/page-header';
import ShareTradeEditor from '../components/share-portfolios/share-trade-editor';

export default function SharePortfolioDetail() {
  const { portfolioId } = useParams();

  const showMessage = useMessageDispatch();
  const [ portfolio, setPortfolio ] = useState<PortfolioResponse|undefined>();

  useEffect(() => {
    if (portfolioId) {
      PortfolioService.getPortfolio(portfolioId)
        .then(response => { setPortfolio(response); return response; })
        .then(portfolio => PortfolioService.getPortfolioHoldings(portfolio.id))
        .catch(err => showMessage(err));
    }
  }, [ showMessage, portfolioId ]);

  return (
    <PageHeader title={portfolio?.name || ''} icon={ <PortfoliosIcon /> }>
      <ShareTradeEditor portfolio={portfolio} />
    </PageHeader>
  );
}