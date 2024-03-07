import axios, {
  AxiosInstance,
  AxiosResponse,
  AxiosRequestConfig,
  AxiosError
} from "axios";

const NOOP: Promise<any> = Promise.resolve();

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

class HttpService {
  private http: AxiosInstance;
  private refreshInflight?: Promise<any> = undefined;

  constructor(baseUrl: string) {
    this.http = axios.create({
      baseURL: baseUrl,
      xsrfCookieName: "XSRF-TOKEN",
      xsrfHeaderName: "X-XSRF-TOKEN",
      headers: {
        "Content-type": "application/json",
      }
    });

    this.http.interceptors.response.use(null, this.checkError.bind(this));

    // add Ip-Address and location data to default headers
    console.log("Fetching user location");
    this.getLocation().then(location => {
      console.log("User location obtained", location)
      this.http.defaults.headers.common["X-Location-IP"] = location.ip;
      this.http.defaults.headers.common["X-Location-City"] = location.city;
      this.http.defaults.headers.common["X-Location-Country"] = location.country_name;
      this.http.defaults.headers.common["X-Location-Lat"] = location.latitude;
      this.http.defaults.headers.common["X-Location-Long"] = location.longitude;
    });
  }

  async getLocation(): Promise<LocationData> {
    const response = await fetch("https://ipapi.co/json");
    const data = await response.json();
    return data as LocationData;
  }

  get<T = any, R = AxiosResponse<T>, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R> {
    return (this.refreshInflight || NOOP).then(() => this.http.get(url, config));
  }

  delete<T = any, R = AxiosResponse<T>, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R> {
    return (this.refreshInflight || NOOP).then(() => this.http.delete(url, config));
  }

  post<T = any, R = AxiosResponse<T>, D = any>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R> {
    return (this.refreshInflight || NOOP).then(() => {
      return this.http.post(url, data, config);
    });
  }

  put<T = any, R = AxiosResponse<T>, D = any>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R> {
    return (this.refreshInflight || NOOP).then(() => this.http.put(url, data, config));
  }

  /**
   * An interceptor implementation for handling HTTP error responses. Checks the
   * response for auth failure and attempts a token refresh.
   * 
   * @param error the error to be inspected
   * @returns 
   */
  private checkError(error: AxiosError) {
    var requestUrl: string = error.request.responseURL;

    // if it's an auth error BUT NOT a request to login or logout
    if ((error.response!.status === 401) &&
        (!requestUrl.includes("/auth/login")) &&
        (!requestUrl.includes("/auth/logout")) &&
        (!requestUrl.includes("/auth/refresh"))) {

      // if a refresh has already been started
      if (this.refreshInflight) {
        // wait for the refresh to complete - retry the original request
        return this.refreshInflight.then(() => this.http.request(error.config!));
      }
      
      else {
        // attempt to refresh the tokens
        this.refreshInflight = this.http.get("/auth/refresh")
          // if the refresh was successful - retry the original request
          .then(() => this.http.request(error.config!))

          // if refresh failed - return the original error
          .catch(() => {
            console.log(error);

            // will cause a refresh of profile context - leading to login page
            //window.location.reload();
            return Promise.reject(error);
          })

          // always delete refresh promise
          .finally(() => this.refreshInflight = undefined);

        return this.refreshInflight;
      }
    }

    // non-401 error OR a login failure
    return Promise.reject(error);
  }
}

const instance = new HttpService(window.location.origin + "/api/v1");
export default instance;
