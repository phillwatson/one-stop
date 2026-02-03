import PortfoliosIcon from '@mui/icons-material/AssessmentOutlined';

import PageHeader from '../components/page-header/page-header';

export default function SharePortfolioDetail() {
  // const { portfolioId } = useParams();
  // const navigate = useNavigate();

  // const showMessage = useMessageDispatch();
  // const [ portfolio, setPortfolio ] = useState<PortfolioResponse|undefined>();

  // useEffect(() => {
  //   if (portfolioId) {
  //     PortfolioService.getPortfolio(portfolioId)
  //       .then(response => setPortfolio(response))
  //       .catch(err => showMessage(err));
  //   }
  // }, [ showMessage, portfolioId ]);

  // function close() {
  //   navigate('/shares/portfolios');
  // }

  // function handleSubmit(portfolio: PortfolioResponse) {
    // PortfolioService.updatePortfolio(portfolio)
    //   .then(() => close())
    //   .catch(err => showMessage(err));
    // }
  // }

  // function handleCancel() {
  //   close();
  // }

  return (
    <PageHeader title='Share Portfolio' icon={ <PortfoliosIcon /> }>
    </PageHeader>
  );
}