import { useState } from "react";
import PageHeader from "../components/page-header/page-header";
import Grid from '@mui/material/Grid';
import Item from '@mui/material/Grid';

import StatisticsGraph from "../components/graph/statistics-graph";
import CategoryTransactions, { Props } from "../components/categories/category-transactions";
import { CategoryStatistics } from "../model/category.model";

export default function StatisticsPage() {
  const [transactionProps, setTransactionProps] = useState<Props | undefined>();

  function showTransactions(category: CategoryStatistics, fromDate: Date, toDate: Date) {
    setTransactionProps({ category, fromDate, toDate });
  }

  return (
    <PageHeader title="Transaction Statistics" >
      <Grid container direction="row" rowGap={ 3 } columnGap={ 3 } justifyContent="space-evenly">
        <Grid key={1}>
          <Item sx={{ minWidth: { xs: 430, md: 600 }, maxWidth: {  md: 800 } }}>
            <StatisticsGraph onCategorySelected={ showTransactions } elevation={ 1 }/>
          </Item>
        </Grid>

        <Grid key={2}>
          <Item sx={{ minWidth: { xs: 400, md: 490 }, maxWidth: 880 }}>
            { transactionProps &&
              <CategoryTransactions elevation={ 1 }
                category={ transactionProps.category }
                fromDate={ transactionProps.fromDate }
                toDate={ transactionProps.toDate } />
            }
          </Item>
        </Grid>
      </Grid>
    </PageHeader>
  );
}