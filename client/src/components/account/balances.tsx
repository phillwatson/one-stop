
import { Paper } from "@mui/material";
import Grid from "@mui/material/Unstable_Grid2/Grid2";

import { AccountDetail } from "../../model/account.model";
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { formatDate } from '../../util/date-util';

interface Props {
  account: AccountDetail;
}

export default function Balances(props: Props) {
  const [ formatMoney ] = useMonetaryContext();
  return (
    <Paper sx={{ padding: 2, marginTop: 1, marginBottom: 1 }}>
      <Grid spacing={1} container direction={"column"}>
        {
        props.account.balance.map(balance => 
          <Grid container key={ balance.id }>
            <Grid>{formatDate(balance.referenceDate)}</Grid>
            <Grid>{balance.type}</Grid>
            <Grid>{formatMoney(balance.amount, balance.currency)}</Grid>
          </Grid>
        )}
      </Grid>
    </Paper>
  )
}