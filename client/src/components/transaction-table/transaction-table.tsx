import { useState } from "react";
import Transaction from "../../model/transaction.model";

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';

// a component to show a table of transactions
export default function TransactionTable() {
  // maintain a list of transactions
  const [transactions, setTransactions] = useState<Transaction[]>([]);


  return (
    <TableContainer component={Paper}>
      <Table sx={{ minWidth: 650 }} aria-label="simple table">
        <TableHead>
          <TableRow>
            <TableCell align="right">Date</TableCell>
            <TableCell align="right">Creditor</TableCell>
            <TableCell align="right">Amount</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {transactions.map((row) => (
            <TableRow
              key={row.id}
              sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
            >
              <TableCell component="th" scope="row">{row.bookingDate.toDateString()}</TableCell>
              <TableCell align="right">{row.creditorName}</TableCell>
              <TableCell align="right">{row.transactionAmount}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>    
  );
}