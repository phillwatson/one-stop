import { useState } from "react";
import PageHeader from "../components/page-header/page-header";
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
      <StatisticsGraph onCategorySelected={ showTransactions }/>

      { transactionProps &&
        <CategoryTransactions
          category={ transactionProps.category }
          fromDate={ transactionProps.fromDate }
          toDate={ transactionProps.toDate } />
      }
    </PageHeader>
  );
}