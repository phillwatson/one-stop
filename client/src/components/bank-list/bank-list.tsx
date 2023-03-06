import './bank-list.css';
import Bank from '../../model/bank.model';
import BankCard from '../bank-card/bank-card'
import UserConsent from '../../model/user-consent.model';

interface Props {
    banks: Array<Bank> | undefined;
    userConsents: Array<UserConsent> | undefined;
    onSelectBank: (bank: Bank, isActive: boolean) => void;
    onLinkSelect: (bank: Bank, link: boolean) => void;
}

export default function BankList(props: Props) {

  function getConsent(bank: Bank): UserConsent | undefined {
    return (props.userConsents) &&
     (props.userConsents.find(consent => consent.institutionId === bank.id));
  }

  return (
    <div className="bank-list">
      { props.banks && props.banks
        .sort((a, b) => { return a.name < b.name ? -1 : 1; } )
        .map((bank, index: number) =>
          <div className="bank-item">
            <BankCard key={ index } bank={ bank } consent={ getConsent(bank) } onLinkSelect= { props.onLinkSelect }/>
          </div>
        )
      }
    </div>
  );
}
