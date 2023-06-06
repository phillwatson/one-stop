import './bank-detail.css';
import Institution from '../../model/institution.model';

interface Props {
  institution: Institution | undefined;
}

export default function BankDetail(props: Props) {
  if (props.institution === undefined) {
    return null;
  }

  return (
    <div className="bank-detail">
      <div><img src={ props.institution.logo } alt="{ props.bank.name } logo" width="50px" height="50px"/></div>
      <div><span className="label">Name:</span><span className="value">{ props.institution.name }</span></div>
      <div><span className="label">BIC:</span><span className="value">{ props.institution.bic }</span></div>
    </div>
  );
}
