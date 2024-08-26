import Select, { SelectChangeEvent } from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import InputLabel from '@mui/material/InputLabel';
import FormControl from '@mui/material/FormControl';

import Country from '../../model/country.model';

interface Props {
    countries: Array<Country>;
    activeCountry: Country | undefined;
    onSelectCountry: any;
}

export default function CountrySelector(props: Props) {
  function handleSelect(event: SelectChangeEvent) {
    const countryId = event.target.value
    if (countryId === undefined) {
      props.onSelectCountry(undefined);
    } else {
      props.onSelectCountry(props.countries.find(country => country.id === countryId))
    }
  }

  return (
    <FormControl required fullWidth>
      <InputLabel id="institution-select-country-label">Country</InputLabel>
      <Select className="country-list" labelId="institution-select-country-label" label="Country"
        value={(props.activeCountry === undefined ? "" : props.activeCountry.id)} onChange={handleSelect}>

        <MenuItem className="country-list-item" aria-label="None" value="" disabled>
          <em>None</em>
        </MenuItem>

        { props.countries && props.countries
          .sort((a, b) => a.name.localeCompare(b.name) )
          .map((country: Country) =>
            <MenuItem key={ country.id } value={country.id} selected={props.activeCountry === country}>
              { country.name }
            </MenuItem>
          )
        }
      </Select>
    </FormControl>
  );
}
