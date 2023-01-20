import './bank-detail.css';
import Bank from '../../model/bank.model';

interface Props {
    bank: Bank | undefined;
}

export default function BankDetail(props: Props) {
  if (props.bank === undefined) {
    return null;
  }

  return (
    <div className="bank-detail">
      <div><img src={ props.bank.logo } alt="{ props.bank.name } logo" width="50px" height="50px"/></div>
      <div><span className="label">Name:</span><span className="value">{ props.bank.name }</span></div>
      <div><span className="label">BIC:</span><span className="value">{ props.bank.bic }</span></div>
      <div><span className="label">Countries:</span><span className="value">{ props.bank.countries.join() }</span></div>
      <div><span className="label">Transactions:</span><span className="value">{ props.bank.transaction_total_days } Days</span></div>
    </div>
  );
}
