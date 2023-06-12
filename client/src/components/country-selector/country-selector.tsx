import Country from '../../model/country.model';
import Select, { SelectChangeEvent } from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import InputLabel from '@mui/material/InputLabel';
import { FormControl } from '@mui/material';

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
    <>
      <FormControl required size="small" sx={{ m: 1, minWidth: 220 }}>

        <InputLabel id="institution-select-country-label">Country</InputLabel>
        <Select className="country-list" labelId="institution-select-country-label" label="Country"
          value={(props.activeCountry === undefined ? "" : props.activeCountry.id)} onChange={handleSelect}>

          <MenuItem className="country-list-item" aria-label="None" value="" disabled>
            <em>None</em>
          </MenuItem>

          { props.countries && props.countries
            .sort((a, b) => { return a.name < b.name ? -1 : 1; } )
            .map((country: Country) =>
              <MenuItem value={country.id} selected={props.activeCountry === country}>
                { country.name }
              </MenuItem>
            )
          }
        </Select>
      </FormControl>
    </>
  );
}
