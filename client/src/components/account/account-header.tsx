import { PropsWithChildren } from "react";

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Avatar from '@mui/material/Avatar';

import { AccountDetail } from "../../model/account.model";

interface Props extends PropsWithChildren {
  title: string;
  account: AccountDetail;
}

export default function AccountHeader(props: Props) {
  return (
    <div>
      <h2>{ props.title }</h2>
      <hr></hr>
      { props.account &&
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow key={props.account.id}>
                <TableCell>
                  <Avatar src={ props.account.institution.logo } alt="{ props.bank.name } logo" sx={{ width: "38px", height: "38px" }}></Avatar>
                </TableCell>
                <TableCell>{props.account.institution.name}</TableCell>
                <TableCell>{props.account.ownerName}</TableCell>
                <TableCell>{props.account.name}</TableCell>
                <TableCell>{props.account.iban}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell colSpan={5}>
                  {props.children}
                </TableCell>
              </TableRow>
            </TableBody>
        </Table>
        </TableContainer>
      }
    </div>
  );
}