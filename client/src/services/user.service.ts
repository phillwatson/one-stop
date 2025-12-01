import http from './http-common';


export interface RegistrationCredentials {
  username: string,
  password: string,
  givenName: string,
  token: string
}


class UserService {
  login(provider: string) {
    return http.get<Location>(`auth/login/${provider}`, { params: { "state": "same-state-value" }})
      .then(response => window.location.assign(response.data.toString()));
  }

  registerNewUser(email: string): Promise<void> {
    const body = {
      email: email
    }
    return http.post<void>('/users/onboard/register', body)
      .then(response => response.data);
  }

  completeRegistration(credentials: RegistrationCredentials): Promise<void> {
    const body = {
      username: credentials.username,
      password: credentials.password,
      givenName: credentials.givenName,
      token: credentials.token
    }
    return http.post<void>('/users/onboard/complete', body)
      .then(response => response.data);
  }
}

const instance = new UserService();
export default instance;
