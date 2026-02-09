import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';

import { ShareTradeSummary } from '../../model/share-portfolio.model';
import useMonetaryContext from '../../contexts/monetary/monetary-context';

interface Props {
  holding: ShareTradeSummary;
  selected?: boolean;
  onClick?: (holding: ShareTradeSummary) => void;
}

export default function ShareTradeSummaryRow({ holding, selected, onClick }: Props) {
  const [ formatMoney ] = useMonetaryContext();

  function selectRow(holding: ShareTradeSummary) {
    if (onClick) {
      onClick(holding);
    }
  }

  return (
    <TableRow key={holding.shareIndexId} hover selected={ selected }
      component="tr" onClick={ () => selectRow(holding) } >
      <TableCell>{holding.name}</TableCell>
      {/*
        <TableCell>{holding.shareId?.isin || '-'}</TableCell>
        <TableCell>{holding.shareId?.tickerSymbol || '-'}</TableCell>
      */}
      <TableCell align="right">{formatMoney(holding.latestPrice, holding.currency, true)}</TableCell>
      <TableCell align="right">{holding.quantity.toLocaleString()}</TableCell>
      <TableCell align="right">{formatMoney(holding.averagePrice, holding.currency, true)}</TableCell>
      <TableCell align="right">{formatMoney(holding.totalCost, holding.currency, true)}</TableCell>
      <TableCell align="right">{formatMoney(holding.currentValue, holding.currency, true)}</TableCell>
      <TableCell align="right"
        sx={{
          color: holding.gainLoss >= 0 ? '#2e7d32' : '#d32f2f',
          fontWeight: 'bold'
        }}
      >
        {formatMoney(holding.gainLoss, holding.currency, true)}
        {' '}({holding.gainLossPercent.toFixed(2)}%)
      </TableCell>
    </TableRow>
  );
}
