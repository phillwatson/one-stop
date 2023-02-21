import axios, {
  AxiosInstance,
  AxiosResponse,
  AxiosRequestConfig,
  AxiosError
} from "axios";

class HttpService {
  private http: AxiosInstance;
  private inflight: boolean = false;

  constructor(baseUrl: string) {
    this.http = axios.create({
      baseURL: baseUrl,
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

  private checkError(error: AxiosError) {
    var requestUrl: string = error.request.responseURL;
    console.log(`checking error [url: ${requestUrl}, status: ${error.response!.status}, inflight: ${this.inflight} ]`);
    if ((!requestUrl.includes("/auth/login")) && (!this.inflight) && (error.response!.status === 401)) {
      this.inflight = true;
      console.log("Trying token refresh");
      return this.http.get("/auth/refresh")
        .then((response) => {
          console.log(`Refresh response: ${response.statusText}`);
          this.inflight = false;
          return new Promise((resolve) => {
            resolve(this.http.request(error.config!));
          })
        .catch((e) => {
          this.inflight = false;
          return new Promise((resolve) => {
            resolve(error);
          });
        });
      });
    }

    return Promise.reject(error);
  }
}

var instance = new HttpService("http://localhost:3000/api/v1");
//instance.post("/auth/login", '{ "username": "admin", "password": "password" }')
export default instance;
