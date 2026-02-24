import { useMemo, useState } from 'react';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Collapse from '@mui/material/Collapse';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Container from '@mui/material/Container';

import { ShareTrade, ShareHoldingSummary } from '../../model/share-portfolio.model';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import HoldingSummaryRow from './holding-summary-row';
import ShareTradeList from './share-trade-list';

import styles from './holding-summary.module.css';

interface Props {
  /**
   * Array of share trade summaries to display
   */
  holdings: ShareHoldingSummary[];
  onAddHolding?: (holding: ShareHoldingSummary) => void;
  onDeleteTrade?: (trade: ShareTrade) => void;
  onEditTrade?: (trade: ShareTrade) => void;
  onSelectHolding?: (holding?: ShareHoldingSummary) => void;
}

export default function HoldingsSummaryList({ holdings, onAddHolding, onDeleteTrade, onEditTrade, onSelectHolding }: Props) {
  const [ formatMoney ] = useMonetaryContext();

  const [ selectedHolding, setSelectingHolding ] = useState<ShareHoldingSummary | undefined>(undefined);

  const totalCost = useMemo(() => {
    return holdings.reduce((sum, holding) => sum + holding.totalCost, 0);
  }, [holdings]);

  const totalValue = useMemo(() => {
    return holdings.reduce((sum, holding) => sum + (holding.latestPrice * holding.quantity), 0);
  }, [holdings]);

  const gainLoss = useMemo(() => {
    return totalValue - totalCost;
  }, [totalValue, totalCost]);

  if (!holdings || holdings.length === 0) {
    return (
      <Box sx={{ padding: 2 }}>
        <Typography variant="body1" color="textSecondary">
          No share holdings found
        </Typography>
      </Box>
    );
  }

  function selectHolding(holding: ShareHoldingSummary) {
    const newSelection = (holding === selectedHolding) ? undefined : holding;
    setSelectingHolding(newSelection);
    if (onSelectHolding) {
      onSelectHolding(newSelection);
    }
  }

  return (
    <Paper style={{ width: '100%', fontSize: '14px' }}>
      <Stack key="header" direction='row' className={`${styles.holdingheader}`}>
        <Container className={`${styles.holdingname}`}>Name</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>Latest Price</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>Quantity</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>Avg. Price</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>Cost</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>Value</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>Gain/Loss</Container>
      </Stack>

      {
        holdings.map(holding =>
          <HoldingSummaryRow key={ holding.shareIndexId } holding={ holding }
            selected={ holding === selectedHolding } onClick={ selectHolding }
            children={
              <Collapse in={ holding === selectedHolding} timeout="auto" unmountOnExit>
                <Stack direction='row'>
                  <Container key={ "options_" + holding.shareIndexId } style={{ alignContent: 'center' }}>
                    { onAddHolding && 
                      <Button variant="text" onClick={ () => onAddHolding(holding) }>Record new trade</Button>
                    }
                  </Container>
                  <Container key={ "trades_" + holding.shareIndexId } style={{ paddingTop: '1px', paddingBottom: '12px' }}>
                      <Typography variant="h6">Trades</Typography>
                      <ShareTradeList holding={ holding } onDeleteTrade={ onDeleteTrade } onEditTrade={ onEditTrade }/>
                  </Container>
                </Stack>
              </Collapse>
            }/>
        )
      }

      <Stack key="totals" direction='row' className={`${styles.holdingtotal}`}>
        <Container className={`${styles.holdingname}`}>Total:</Container>
        <Container className={`${styles.holdingcell}`}></Container>
        <Container className={`${styles.holdingcell}`}></Container>
        <Container className={`${styles.holdingcell}`}></Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>{ formatMoney(totalCost / 100, holdings[0].currency) }</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>{ formatMoney(totalValue / 100, holdings[0].currency) }</Container>
        <Container className={`${styles.holdingcell} ${ styles.money } ${ gainLoss >= 0 ? styles.gain : styles.loss }`}>
          {formatMoney(gainLoss / 100, holdings[0].currency)}
          <br/>({totalCost > 0 ? ((gainLoss / totalCost) * 100).toFixed(2) : '0.00'}%)
        </Container>
      </Stack>
    </Paper>
  );
}
