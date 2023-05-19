import http from './http-common';
import UserProfile from '../model/user-profile.model';

class ProfileService {
  get() {
    console.log(`Retrieving user-profile`);
    return http.get<UserProfile>(`/api/v1/profiles`);
  }
}

const instance = new ProfileService();
export default instance;
