import Institution from '../../model/institution.model';
import BankCard from '../bank-card/bank-card'
import UserConsent from '../../model/user-consent.model';

import styles from './bank-list.module.css';

interface Props {
    institutions: Array<Institution> | undefined;
    userConsents: Array<UserConsent> | undefined;
    onLinkSelect?: (institution: Institution) => void;
}

export default function BankList(props: Props) {

  function getConsent(institution: Institution): UserConsent | undefined {
    return (props.userConsents) &&
     (props.userConsents.find(consent => consent.institutionId === institution.id));
  }

  return (
    <span className={ styles.bank_list }>
      { props.institutions && props.institutions
        .sort((a, b) => { return a.name < b.name ? -1 : 1; } )
        .map((institution, index: number) =>
          <BankCard key={ index } institution={ institution } consent={ getConsent(institution) } onLinkSelect= { props.onLinkSelect }/>
        )
      }
    </span>
  );
}
