import UserConsent from '../model/user-consent.model';
import http from './http-common';

class UserConsentService {
  getConsents() {
    return http.get<Array<UserConsent>>("/rails/consents");
  }

  registerConsent(bankId: string) {
    return http.post(`/rails/consents`, `{ "institutionId": "${bankId}" }`);
  }

  cancelConsent(bankId: string) {
    return http.delete(`/rails/consents/${bankId}`);
  }
}

var instance = new UserConsentService();
export default instance;
