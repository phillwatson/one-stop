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
      { props.countries && props.countries
        .sort((a, b) => { return a.name < b.name ? -1 : 1; } )
        .map((country: Country, index: number) =>
          <option className="country-list-item"
            onClick={() => { props.onSelectCountry(country); } }
            key={index}
          >
            { country.name }
          </option>
        )
      }
    </select>
  );
}
