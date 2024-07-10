import PageHeader from "../components/page-header/page-header";
import StatisticsGraph from "../components/graph/statistics-graph";

export default function StatisticsPage() {
  return (
    <PageHeader title="Transaction Statistics" >
      <StatisticsGraph />
    </PageHeader>
  );
}