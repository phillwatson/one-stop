import http from './http-common';
import UserProfile, { UserAuthProvider, UserAuthProvidersResponse } from '../model/user-profile.model';

class ProfileService {
  get(): Promise<UserProfile> {
    return http.get<UserProfile>('/profiles')
      .then(response => response.data);
  }

  getAuthProviders(): Promise<Array<UserAuthProvider>> {
    return http.get<UserAuthProvidersResponse>('/profiles/authproviders')
      .then(response => response.data.authProviders);
  }

  deleteAuthProvider(authProviderId: string): Promise<any> {
    return http.delete<void>('/profiles/authproviders/' + authProviderId);
  }

  update(profile: UserProfile): Promise<UserProfile> {
    return http.put<UserProfile>('/profiles', profile)
      .then(response => response.data);
  }

  login(username: string, password: string) {
    const credentials = {
      username: username,
      password: password
    }
    return http.post("/auth/login", credentials);
  }

  logout() {
    return http.get('/auth/logout');
  }
}

const instance = new ProfileService();
export default instance;
