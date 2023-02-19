import React from "react";
import Country from '../model/country.model';
import CountryService from '../services/country.service';
import CountrySelector from '../components/country-selector/country-selector'
import Bank from '../model/bank.model';
import BankService from '../services/bank.service';
import BankList from '../components/bank-list/bank-list'
import BankDetail from '../components/bank-detail/bank-detail'

export default function Institutions() {
  const [countries, setCountries] = React.useState<Array<Country> | undefined>(undefined);
  const [activeCountry, setActiveCountry] = React.useState<Country | undefined>(undefined);

  const [banks, setBanks] = React.useState<Array<Bank> | undefined>(undefined);
  const [activeBank, setActiveBank] = React.useState<Bank | undefined>(undefined);

  React.useEffect(() => {
    CountryService.getAll().then((response) => {
      setCountries(response.data);
    });
  }, []);

  React.useEffect(() => {
    var countryId = (activeCountry === undefined) ? 'GB' : activeCountry.id;
    BankService.getAll(countryId).then((response) => {
      setBanks(response.data);
    });
  }, [ activeCountry ]);

  React.useEffect(() => {
    setActiveBank(undefined);
  }, [ banks ]);

  return (
    <div>
      <div>
        Select Country: <CountrySelector countries={ countries }
            activeCountryId={ (activeCountry === undefined) ? undefined : activeCountry.id }
            onSelectCountry={ setActiveCountry }/>
      </div>
      <div>
        <BankList banks={ banks }
            activeBankId={ (activeBank === undefined) ? undefined : activeBank.id }
            onSelectBank={ setActiveBank }/>
      </div>
      <div>
        <BankDetail bank={ activeBank } />
      </div>
    </div>
  );
}
