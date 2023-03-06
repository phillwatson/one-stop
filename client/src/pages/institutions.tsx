import React from "react";
import Country from '../model/country.model';
import CountryService from '../services/country.service';
import CountrySelector from '../components/country-selector/country-selector'
import Bank from '../model/bank.model';
import BankService from '../services/bank.service';
import BankList from '../components/bank-list/bank-list'
import BankDetail from '../components/bank-detail/bank-detail'
import UserConsentService from '../services/consent.service';
import UserConsent from "../model/user-consent.model";

export default function Institutions() {
  const [countries, setCountries] = React.useState<Array<Country> | undefined>(undefined);
  const [activeCountry, setActiveCountry] = React.useState<Country | undefined>(undefined);

  const [banks, setBanks] = React.useState<Array<Bank> | undefined>(undefined);
  const [selectedBank, setSelectedBank] = React.useState<Bank | undefined>(undefined);

  const [userConsents, setUserConsents] = React.useState<Array<UserConsent> | undefined>(undefined);

  React.useEffect(() => {
    CountryService.getAll().then((response) => {
      setCountries(response.data);
    });

    UserConsentService.getConsents().then((response) => {
      setUserConsents(response.data);
    });
  }, []);

  React.useEffect(() => {
    if (activeCountry === undefined) {
        setBanks(undefined);
    } else {
        BankService.getAll(activeCountry.id).then((response) => {
          setBanks(response.data);
        });
    }
  }, [ activeCountry ]);

  React.useEffect(() => {
    setSelectedBank(undefined);
  }, [ banks ]);

  function handleSelectBank(bank: Bank, isActive: boolean) {
    console.log(`Selected bank [name: ${bank.name}, active: ${isActive}]`);
    setSelectedBank(bank);
  }
  
  function handleLinkSelect(bank: Bank, link: boolean) {
    if (link) {
      console.log(`Closing bank [name: ${bank.name}]`);
      UserConsentService.cancelConsent(bank.id);
    } else {
      console.log(`Linking to bank [name: ${bank.name}]`);
      UserConsentService.registerConsent(bank.id);
    }
  }

  return (
    <div>
      <div>
        Select Country: <CountrySelector countries={ countries }
            activeCountryId={ (activeCountry === undefined) ? undefined : activeCountry.id }
            onSelectCountry={ setActiveCountry }/>
      </div>
      <div>
        <BankList banks={ banks } userConsents={ userConsents }
            onSelectBank={ handleSelectBank } onLinkSelect={ handleLinkSelect }/>
      </div>
      <div>
        <BankDetail bank={ selectedBank } />
      </div>
    </div>
  );
}
