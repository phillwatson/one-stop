import http from './http-common';


export interface RegistrationCredentials {
  username: string,
  password: string,
  givenName: string,
  token: string
}


class UserService {
  registerNewUser(email: string): Promise<void> {
    console.log(`Initiating user registration "${email}"`);

    const body = {
      email: email
    }
    return http.post<void>('/users/onboard/register', body)
      .then(response => response.data);
  }

  completeRegistration(credentials: RegistrationCredentials): Promise<void> {
    console.log(`Completing user registration for "${credentials.givenName}"`);

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
