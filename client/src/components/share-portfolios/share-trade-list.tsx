import { useEffect, useState } from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import { SxProps } from '@mui/material/styles';

import PortfolioService from '../../services/portfolio.service';
import { ShareTradeSummary, ShareTrade } from '../../model/share-portfolio.model';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { toLocaleDate } from '../../util/date-util';
import Box from '@mui/material/Box';

interface Props {
  holding: ShareTradeSummary;
}

const colhead: SxProps = {
  fontWeight: 'bold',
  backgroundColor: '#f5f5f5'
};

interface TradeSummary {
  // The trade
  trade: ShareTrade;
  
  // The current total value of the trade, in minor currency units.
  currentValue: number;

  // The total gain or loss for the trade, in minor currency units.
  gainLoss: number;

  // The total gain or loss for the trade, as a percentage of the currentValue.
  gainLossPercent: number;
}

function calcTradeSummary(holding: ShareTradeSummary, trade: ShareTrade): TradeSummary {
  const currentValue = holding.latestPrice * trade.quantity;
  const gainLoss = currentValue - trade.totalCost;
  return {
    trade: trade,
    currentValue: currentValue,
    gainLoss: gainLoss,
    gainLossPercent: trade.totalCost > 0 ? ((gainLoss / trade.totalCost) * 100) : 0
  } as TradeSummary;
}

export default function ShareTradeList({ holding }: Props) {
  const [ formatMoney ] = useMonetaryContext();

  const [ trades, setTrades ] = useState<ShareTrade[]>([]);

  useEffect(() => {
    PortfolioService.fetchShareTrades(holding.portfolioId, holding.shareIndexId)
      .then(response => setTrades(response)
    )
  }, [holding]);

  if (trades.length === 0) {
    return (
      <Box>No trades exist.</Box>
    )
  }

  return (
    <Table size="small" aria-label="trades">
      <TableHead>
        <TableRow>
          <TableCell sx={colhead}>Date</TableCell>
          <TableCell sx={colhead}>Type</TableCell>
          <TableCell sx={colhead} align="right">Quantity</TableCell>
          <TableCell sx={colhead} align="right">Price</TableCell>
          <TableCell sx={colhead} align="right">Total</TableCell>
          <TableCell sx={colhead} align="right">Gain/Loss</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        { trades.length > 0 && trades
          .map(trade => calcTradeSummary(holding, trade))
          .map(summary => (
          <TableRow key={summary.trade.id}>
            <TableCell>{toLocaleDate(summary.trade.dateExecuted)}</TableCell>
            <TableCell>{ summary.trade.quantity > 0 ? 'Buy' : 'Sell' }</TableCell>
            <TableCell align="right">{Math.abs(summary.trade.quantity).toLocaleString()}</TableCell>
            <TableCell align="right">{formatMoney(summary.trade.pricePerShare / 100, holding.currency)}</TableCell>
            <TableCell align="right">{formatMoney(summary.trade.totalCost / 100, holding.currency)}</TableCell>
            <TableCell align="right"
              sx={{
                color: summary.gainLoss >= 0 ? '#2e7d32' : '#d32f2f',
                fontWeight: 'bold'
              }}
            >
              {formatMoney(summary.gainLoss / 100, holding.currency)}
              {' '}{ summary.trade.quantity > 0 ? '(' + summary.gainLossPercent.toFixed(2) + '%)' : ''}
            </TableCell>
          </TableRow>
        )) }
      </TableBody>
    </Table>
  );
}
