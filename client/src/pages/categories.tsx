import PageHeader from "../components/page-header/page-header";
import CategoryList from "../components/categories/category-list";

export default function Categories() {
  return (
    <PageHeader title="Transaction Categories">
      <CategoryList/>
    </PageHeader>
  );
}
