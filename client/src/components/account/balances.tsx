
import { Paper } from "@mui/material";
import Grid from "@mui/material/Unstable_Grid2/Grid2";

import { AccountDetail } from "../../model/account.model";
import CurrencyService from '../../services/currency.service';
import { formatDate } from '../../util/date-util';

interface Props {
  account: AccountDetail;
}

export default function Balances(props: Props) {
  return (
    <Paper sx={{ padding: 2, marginBottom: 1 }}>
      <Grid spacing={1} container direction={"column"}>
        {
        props.account.balance.map(balance => 
          <Grid container>
            <Grid>{formatDate(balance.referenceDate)}</Grid>
            <Grid>{balance.type}</Grid>
            <Grid>{CurrencyService.format(balance.amount, balance.currency)}</Grid>
          </Grid>
        )}
      </Grid>
    </Paper>
  )
}