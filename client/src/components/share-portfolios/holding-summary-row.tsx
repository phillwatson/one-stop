import { PropsWithChildren } from 'react';
import Stack from '@mui/material/Stack';
import Container from '@mui/material/Container';

import { ShareHoldingSummary } from '../../model/share-portfolio.model';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import styles from './holding-summary.module.css';

interface Props extends PropsWithChildren {
  holding: ShareHoldingSummary;
  selected?: boolean;
  onClick?: (holding: ShareHoldingSummary) => void;
}

export default function HoldingSummaryRow({ holding, selected, onClick, children }: Props) {
  const [ formatMoney ] = useMonetaryContext();

  function selectRow(holding: ShareHoldingSummary) {
    if (onClick) {
      onClick(holding);
    }
  }

  return (
    <div className={`${ selected ? styles.selected : ''}`} >
      <Stack direction='row' onClick={ () => selectRow(holding) } className={`${styles.holdingrow}`} >
        <Container className={`${styles.holdingname}`}>{ holding.name }</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>{ formatMoney(holding.latestPrice, holding.currency, true) }</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>{ holding.quantity.toLocaleString() }</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>{ formatMoney(holding.averagePrice, holding.currency, true) }</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>{ formatMoney(holding.totalCost / 100, holding.currency) }</Container>
        <Container className={`${styles.holdingcell} ${ styles.money }`}>{ formatMoney(holding.currentValue / 100, holding.currency) }</Container>
        <Container className={`${styles.holdingcell} ${ styles.money } ${ holding.gainLoss >= 0 ? styles.gain : styles.loss }`}>
          {formatMoney(holding.gainLoss / 100, holding.currency)}
          <br/>({holding.gainLossPercent.toFixed(2)}%)
        </Container>
      </Stack>
      
      { children }
    </div>
  );
}
