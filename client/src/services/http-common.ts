import axios, {
  AxiosInstance,
  AxiosResponse,
  AxiosRequestConfig,
  AxiosError
} from "axios";

class HttpService {

  private http: AxiosInstance;
  private refreshInflight?: Promise<any> = undefined;

  constructor(baseUrl: string) {
    this.http = axios.create({
      baseURL: baseUrl,
      xsrfCookieName: "XSRF-TOKEN",
      xsrfHeaderName: "X-XSRF-TOKEN",
      headers: {
        "Content-type": "application/json"
      }
    });

    this.http.interceptors.response.use(null, this.checkError.bind(this));
  }

  get<T = any, R = AxiosResponse<T>, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R> {
    return this.http.get(url, config);
  }

  delete<T = any, R = AxiosResponse<T>, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R> {
    return this.http.delete(url, config);
  }

  post<T = any, R = AxiosResponse<T>, D = any>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R> {
    return this.http.post(url, data, config);
  }

  put<T = any, R = AxiosResponse<T>, D = any>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R> {
    return this.http.put(url, data, config);
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

    // if it's an auth error BUT not a request to login
    if ((error.response!.status === 401) && (!requestUrl.includes("/auth/login"))) {
      // if a refresh has already been started
      if (this.refreshInflight) {
        // wait for the refresh to complete
        return this.refreshInflight.then(() => new Promise((resolve) => {
          // retry the original request
          return resolve(this.http.request(error.config!));
        } ));
      }
      
      else {
        // attempt to refresh the tokens
        this.refreshInflight = this.http.get("/auth/refresh")
          .then(() => {
            // if the refresh was successful - retry the original request
            return new Promise((resolve) => resolve(this.http.request(error.config!)));
          })
          .catch(() => {
            // if refresh failed - return the original error
            return new Promise((resolve) => resolve(error) );
          })
          .finally(() => this.refreshInflight = undefined );

        return this.refreshInflight;
      }
    }

    // non-401 error OR a login failure
    return Promise.reject(error);
  }
}

const instance = new HttpService(window.location.origin + "/api/v1");
export default instance;
