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
      <TableCell align="right">{formatMoney(holding.latestPrice / 100, holding.currency)}</TableCell>
      <TableCell align="right">{holding.quantity.toLocaleString()}</TableCell>
      <TableCell align="right">{formatMoney(holding.averagePrice / 100, holding.currency)}</TableCell>
      <TableCell align="right">{formatMoney(holding.totalCost / 100, holding.currency)}</TableCell>
      <TableCell align="right">{formatMoney(holding.currentValue / 100, holding.currency)}</TableCell>
      <TableCell align="right"
        sx={{
          color: holding.gainLoss >= 0 ? '#2e7d32' : '#d32f2f',
          fontWeight: 'bold'
        }}
      >
        {formatMoney(holding.gainLoss / 100, holding.currency)}
        {' '}({holding.gainLossPercent.toFixed(2)}%)
      </TableCell>
    </TableRow>
  );
}
