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
      .then(response => window.location = response.data);

    //window.location.href = window.location.origin + "/api/v1/auth/login/" + provider + "?state=same-state-value";
  }

  registerNewUser(sendEmail: string): Promise<void> {
    const body = {
      sendEmail: sendEmail
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
