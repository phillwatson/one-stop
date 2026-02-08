import { useMemo, useState } from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import { SxProps } from '@mui/material/styles';

import { ShareTradeSummary } from '../../model/share-portfolio.model';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import ShareTradeSummaryRow from './trade-summary-row';

interface Props {
  /**
   * Array of share trade summaries to display
   */
  holdings: ShareTradeSummary[];
}

const colhead: SxProps = {
  fontWeight: 'bold',
  backgroundColor: '#f5f5f5'
};

export default function ShareTradeSummaryList({ holdings }: Props) {
  const [ formatMoney ] = useMonetaryContext();

  const [ selectedHolding, setSelectingHolding ] = useState<ShareTradeSummary | undefined>(undefined);

  function selectHolding(holding: ShareTradeSummary) {
    setSelectingHolding((holding === selectedHolding) ? undefined : holding);
  }

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

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
            <TableCell sx={colhead}>Share Name</TableCell>
            {/* <TableCell sx={colhead}>ISIN</TableCell>
            <TableCell sx={colhead}>Ticker</TableCell> */}
            <TableCell sx={colhead} align="right">Latest Price</TableCell>
            <TableCell sx={colhead} align="right">Quantity</TableCell>
            <TableCell sx={colhead} align="right">Avg. Price</TableCell>
            <TableCell sx={colhead} align="right">Total Cost</TableCell>
            <TableCell sx={colhead} align="right">Current Value</TableCell>
            <TableCell sx={colhead} align="right">Gain/Loss</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {
            holdings.map(holding =>
              <ShareTradeSummaryRow
                holding={ holding }
                selected={ holding === selectedHolding }
                showTrades={ holding === selectedHolding }
                onClick={ selectHolding }/>
            )
          }


          <TableRow key="total" sx={{ backgroundColor: '#f5f5f5', fontWeight: 'bold' }}>
            <TableCell colSpan={4} sx={{ fontWeight: 'bold' }}>Total</TableCell>
            <TableCell align="right" sx={{ fontWeight: 'bold' }}>
              {formatMoney(totalCost / 100, holdings[0].currency)}
            </TableCell>
            <TableCell align="right" sx={{ fontWeight: 'bold' }}>
              {formatMoney(totalValue / 100, holdings[0].currency)}
            </TableCell>
            <TableCell 
              align="right"
              sx={{
                fontWeight: 'bold',
                color: gainLoss >= 0 ? '#2e7d32' : '#d32f2f'
              }}
            >
              {formatMoney(gainLoss / 100, holdings[0].currency)}
              {' '}({totalCost > 0 ? ((gainLoss / totalCost) * 100).toFixed(2) : '0.00'}%)
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </TableContainer>
  );
}
