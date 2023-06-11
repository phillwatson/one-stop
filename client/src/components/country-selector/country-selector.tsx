import './country-selector.css';
import Country from '../../model/country.model';

interface Props {
    countries: Array<Country> | undefined;
    activeCountryId: string;
    onSelectCountry: any;
}

export default function CountrySelector(props: Props) {
  return (
    <select className="country-list">
      <option className="country-list-item"
        key={""}
        onClick={() => { props.onSelectCountry(undefined); } }
        selected={props.activeCountryId===undefined}
      >
      </option>
      { props.countries && props.countries
        .sort((a, b) => { return a.name < b.name ? -1 : 1; } )
        .map((country: Country) =>
          <option className="country-list-item"
            key={country.id}
            onClick={() => { props.onSelectCountry(country); } }
            selected={props.activeCountryId===country.id}
          >
            { country.name }
          </option>
        )
      }
    </select>
  );
}
