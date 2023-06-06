import http from './http-common';
import UserProfile from '../model/user-profile.model';

class ProfileService {
  get(): Promise<UserProfile> {
    console.log('Retrieving user-profile');
    return http.get<UserProfile>('/profiles')
      .then(response => response.data);
  }

  update(profile: UserProfile): Promise<UserProfile> {
    console.log('Updating user-profile');
    return http.put<UserProfile>('/profiles', profile)
      .then(response => response.data);
  }

  login(username: string, password: string) {
    console.log('Logging in ' + username);
    const credentials = {
      username: username,
      password: password
    }
    return http.post("/auth/login", credentials);
  }

  logout() {
    console.log('Logging out');
    return http.get('/auth/logout');
  }
}

const instance = new ProfileService();
export default instance;
