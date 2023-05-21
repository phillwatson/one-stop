import http from './http-common';
import UserProfile from '../model/user-profile.model';

class ProfileService {
  get() {
    console.log('Retrieving user-profile');
    return http.get<UserProfile>('/profiles');
  }
}

const instance = new ProfileService();
export default instance;
