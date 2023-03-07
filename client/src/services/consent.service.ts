import UserConsent from '../model/user-consent.model';
import http from './http-common';

class UserConsentService {
  getConsent(bankId: string) {
    return http.get<UserConsent>(`/rails/consents/${bankId}`);
  }

  getConsents() {
    return http.get<Array<UserConsent>>("/rails/consents");
  }

  registerConsent(bankId: string) {
    console.log(`Registering bank [name: ${bankId}]`);
    return http.post(`/rails/consents`, `{ "institutionId": "${bankId}" }`);
  }

  cancelConsent(bankId: string) {
    console.log(`Closing bank [name: ${bankId}]`);
    return http.delete(`/rails/consents/${bankId}`);
  }
}

const instance = new UserConsentService();
export default instance;
