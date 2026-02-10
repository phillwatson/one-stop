import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tooltip from '@mui/material/Tooltip';
import IconButton from '@mui/material/IconButton';
import DeleteIcon from '@mui/icons-material/DeleteOutlined';
import EditIcon from '@mui/icons-material/ModeEdit';
import { SxProps } from '@mui/material/styles';

import PortfolioService from '../../services/portfolio.service';
import { ShareTradeSummary, ShareTrade } from '../../model/share-portfolio.model';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { toLocaleDate } from '../../util/date-util';
import { Currency } from '../../model/commons.model';
import Stack from '@mui/material/Stack';

type TradeFunction = (trade: ShareTrade) => void;

const colhead: SxProps = {
  fontWeight: 'bold',
  backgroundColor: '#f5f5f5'
};

const gain: SxProps = {
  color: '#2e7d32',
  fontWeight: 'bold',
  position: 'relative'
};

const loss: SxProps = {
  color: '#d32f2f',
  fontWeight: 'bold',
  position: 'relative'
};

interface TradeSummary {
  // The trade
  trade: ShareTrade;

  currency: Currency;
  
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
    currency: holding.currency,
    currentValue: currentValue,
    gainLoss: gainLoss,
    gainLossPercent: trade.totalCost > 0 ? ((gainLoss / trade.totalCost) * 100) : 0
  } as TradeSummary;
}

interface RowProps {
  summary: TradeSummary;
  onDeleteTrade?: (trade: ShareTrade) => void;
  onEditTrade?: (trade: ShareTrade) => void;
}

function HoverOptions({ summary, onDeleteTrade, onEditTrade }: RowProps) {
  if ((!onDeleteTrade && !onEditTrade) || !summary.trade.id) {
    return null;
  }

  return (
    <Stack direction='row' spacing='3px' zIndex='1000' position='absolute' top='0' right='0'
           bgcolor='#f5fafc' borderRadius='3' padding='0' paddingLeft='25px' paddingRight='14px'>
      { onEditTrade &&
        <Tooltip title="Amend">
          <IconButton size="small" onClick={() => onEditTrade(summary.trade) }>
            <EditIcon fontSize="small"/>
          </IconButton>
        </Tooltip>
      }

      { onDeleteTrade &&
        <Tooltip title="Delete">
          <IconButton size="small" onClick={() => onDeleteTrade(summary.trade) }>
            <DeleteIcon fontSize="small"/>
          </IconButton>
        </Tooltip>
      }
    </Stack>
  );
}

function TradeRow({ summary, onDeleteTrade, onEditTrade }: RowProps) {
  const [ formatMoney ] = useMonetaryContext();
  const [ showOptions, setShowOptions ] = useState<boolean>(false);

  return (
    <TableRow key={summary.trade.id} hover
        onMouseEnter={ () => setShowOptions(true) }
        onMouseLeave={ () => setShowOptions(false) }
        sx={{ position: 'relative' }}
     >
      <TableCell>{ summary.trade.quantity > 0 ? 'Buy' : 'Sell' }</TableCell>
      <TableCell>{toLocaleDate(summary.trade.dateExecuted)}</TableCell>
      <TableCell align="right">{Math.abs(summary.trade.quantity).toLocaleString()}</TableCell>
      <TableCell align="right">{formatMoney(summary.trade.pricePerShare, summary.currency, true)}</TableCell>
      <TableCell align="right">{formatMoney(summary.trade.totalCost / 100, summary.currency)}</TableCell>
      <TableCell align="right" sx={ summary.gainLoss >= 0 ? gain : loss } >
        {formatMoney(summary.gainLoss / 100, summary.currency)}
        {' '}{ summary.trade.quantity > 0 ? '(' + summary.gainLossPercent.toFixed(2) + '%)' : ''}

        { showOptions &&
          <HoverOptions summary={ summary } onEditTrade={ onEditTrade } onDeleteTrade={ onDeleteTrade }/>
        }
      </TableCell>
    </TableRow>
  )
}


interface Props {
  holding: ShareTradeSummary;
  onDeleteTrade?: TradeFunction;
  onEditTrade?: TradeFunction;
}
export default function ShareTradeList({ holding, onDeleteTrade, onEditTrade }: Props) {
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
    <Table size="small" aria-label="trades" style={{ minWidth: '700px' }}>
      <TableHead>
        <TableRow>
          <TableCell sx={colhead}>Type</TableCell>
          <TableCell sx={colhead}>Date</TableCell>
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
            <TradeRow summary={ summary } onDeleteTrade={ onDeleteTrade } onEditTrade={ onEditTrade } />
        )) }
      </TableBody>
    </Table>
  );
}
