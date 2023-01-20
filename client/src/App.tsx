import React from 'react';
import './App.css';
import Country from './model/country.model';
import CountryService from './service/country.service';
import CountrySelector from './components/country-selector/country-selector'
import Bank from './model/bank.model';
import BankService from './service/bank.service';
import BankList from './components/bank-list/bank-list'
import BankDetail from './components/bank-detail/bank-detail'

function App() {
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
    <div className="App">
        <div className="col-1">
            <div className="country-selector">
                Select Country: <CountrySelector countries={ countries }
                                 activeCountryId={ (activeCountry === undefined) ? undefined : activeCountry.id }
                                 onSelectCountry={ setActiveCountry }/>
            </div>
            <div className="bank-list">
                <BankList banks={ banks }
                          activeBankId={ (activeBank === undefined) ? undefined : activeBank.id }
                          onSelectBank={ setActiveBank }/>
            </div>
        </div>
        <div className="col-2">
          <div className="bank-detail">
              <BankDetail bank={ activeBank } />
          </div>
        </div>
    </div>
  );
}

export default App;
