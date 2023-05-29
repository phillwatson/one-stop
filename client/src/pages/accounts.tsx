import React from "react";

import UserConsentService from '../services/consent.service';
import UserConsent from "../model/user-consent.model";
import Bank from '../model/bank.model';
import BankService from '../services/bank.service';
import BankList from '../components/bank-list/bank-list'

export default function Accounts() {
  const [userConsents, setUserConsents] = React.useState<Array<UserConsent> | undefined>(undefined);
  const [banks, setBanks] = React.useState<Array<Bank> | undefined>(undefined);

  React.useEffect(() => {
    UserConsentService.getConsents().then((response) => {
      setUserConsents(response.data);
    });
  }, []);

  React.useEffect(() => {
    if (userConsents !== undefined) {
      const bankRequests: Array<Promise<Bank>> = userConsents.map(consent =>
        BankService.get(consent.institutionId).then(response => response.data)
      );
      Promise.all(bankRequests).then(list => setBanks(list));
    }
  }, [userConsents]);


  function handleLinkSelect(bank: Bank, link: boolean) {
    if (link) {
      UserConsentService.cancelConsent(bank.id).then(() => {
        if (userConsents !== undefined) {
          const update = userConsents.filter(consent => consent.institutionId !== bank.id);
          setUserConsents(update);
        }
      });
    }
  }


  return (
    <div>
      <h2>Accounts</h2>
      <hr></hr>
      <BankList banks={banks} userConsents={userConsents} onLinkSelect={handleLinkSelect} />
    </div>
  );
}
