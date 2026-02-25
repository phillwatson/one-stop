import { useState } from 'react';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Stack from '@mui/material/Stack';
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

import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { ShareHoldingSummary, ShareTrade } from '../../model/share-portfolio.model';
import { formatShortDate } from '../../util/date-util';
import { Currency } from '../../model/commons.model';

const money: SxProps = {
  textAlign: 'right'
}

const gain: SxProps = {
  color: '#2e7d32',
  fontWeight: 'bold'
}

const loss: SxProps = {
  color: '#d32f2f',
  fontWeight: 'bold'
}


type TradeFunction = (trade: ShareTrade) => void;

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

function calcTradeSummary(holding: ShareHoldingSummary, trade: ShareTrade): TradeSummary {
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
    <Stack direction='row' spacing='3px' bgcolor='whitesmoke'
           zIndex='1000' position='absolute' top='1px' right='0'
           padding='0px 14px 0px 25px'>
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
    <TableRow hover
        onMouseEnter={ () => setShowOptions(true) }
        onMouseLeave={ () => setShowOptions(false) }
        sx={{ position: 'relative' }}
     >
      <TableCell>{ summary.trade.quantity > 0 ? 'Buy' : 'Sell' }</TableCell>
      <TableCell>{formatShortDate(summary.trade.dateExecuted)}</TableCell>
      <TableCell sx={money}>{Math.abs(summary.trade.quantity).toLocaleString()}</TableCell>
      <TableCell sx={money}>{formatMoney(summary.trade.pricePerShare, summary.currency, true)}</TableCell>
      <TableCell sx={money}>{formatMoney(summary.trade.totalCost / 100, summary.currency)}</TableCell>
      <TableCell sx={money}>
        <Container sx={summary.gainLoss >= 0 ? gain : loss} style={{ padding: 0, margin: 0 }}>
          {formatMoney(summary.gainLoss / 100, summary.currency)}
        </Container>
      </TableCell>
      <TableCell sx={money}>
        <Container sx={summary.gainLoss >= 0 ? gain : loss} style={{ padding: 0, margin: 0 }}>
          {' '}{ summary.trade.quantity > 0 ? '(' + summary.gainLossPercent.toFixed(2) + '%)' : ''}

          { showOptions &&
            <HoverOptions summary={ summary } onEditTrade={ onEditTrade } onDeleteTrade={ onDeleteTrade }/>
          }
        </Container>
      </TableCell>
    </TableRow>
  )
}


interface Props {
  holding: ShareHoldingSummary;
  trades: ShareTrade[];
  onDeleteTrade?: TradeFunction;
  onEditTrade?: TradeFunction;
}
export default function ShareTradeList({ holding, trades, onDeleteTrade, onEditTrade }: Props) {
  if (trades.length === 0) {
    return (
      <Box>No trades exist.</Box>
    )
  }

  return (
    <Table size="small" aria-label="trades" style={{ minWidth: '700px' }}>
      <TableHead>
        <TableRow>
          <TableCell>Type</TableCell>
          <TableCell>Date</TableCell>
          <TableCell sx={money} >Quantity</TableCell>
          <TableCell sx={money} >Price</TableCell>
          <TableCell sx={money} >Total</TableCell>
          <TableCell sx={money} >Gain/Loss</TableCell>
          <TableCell sx={money} ></TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        { trades.length > 0 && trades
          .map(trade => calcTradeSummary(holding, trade))
          .map(summary => (
            <TradeRow key={ summary.trade.id } summary={ summary } onDeleteTrade={ onDeleteTrade } onEditTrade={ onEditTrade } />
        )) }
      </TableBody>
    </Table>
  );
}
