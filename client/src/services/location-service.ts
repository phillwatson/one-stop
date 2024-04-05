export interface LocationData {
  city: string,
  continent_code: string,
  country: string,
  country_area: number,
  country_calling_code: string,
  country_capital: string,
  country_code: string,
  country_code_iso3: string,
  country_name: string,
  country_population: number,
  country_tld: string,
  currency: string,
  currency_name: string,
  in_eu: boolean,
  ip: string,
  languages: string,
  latitude: number,
  longitude: number,
  network: string,
  org: string,
  postal: string,
  region: string,
  region_code: string,
  timezone: string,
  utc_offset: string,
  version: string
}

export default async function getLocation(): Promise<LocationData> {
  const response = await fetch("https://ipapi.co/json");
  const data = await response.json();
  return data as LocationData;
}
