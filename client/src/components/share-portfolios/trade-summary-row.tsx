import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

import { ShareTradeSummary } from '../../model/share-portfolio.model';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import Collapse from '@mui/material/Collapse';
import ShareTradeList from './share-trade-list';

interface Props {
  holding: ShareTradeSummary;
  showTrades: boolean;
  selected?: boolean;
  onClick?: (holding: ShareTradeSummary) => void;
}

export default function ShareTradeSummaryRow({ holding, showTrades, selected, onClick }: Props) {
  const [ formatMoney ] = useMonetaryContext();

  function selectRow(holding: ShareTradeSummary) {
    if (onClick) {
      onClick(holding);
    }
  }

  return (
    <>
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

      <TableRow>
        <TableCell style={{ paddingBottom: 0, paddingTop: 0 }}></TableCell>
        <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
          <Collapse in={showTrades} timeout="auto" unmountOnExit>
            <Box>
              <Typography variant="h6" gutterBottom component="div">
                Trades
              </Typography>
              <ShareTradeList holding={ holding } />
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}
