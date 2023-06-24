import UserConsent from '../model/user-consent.model';
import PaginatedList from '../model/paginated-list.model';
import http from './http-common';

class UserConsentService {
  getConsent(bankId: string): Promise<UserConsent> {
    return http.get<UserConsent>(`/rails/consents/${bankId}`)
      .then(response => response.data);
  }

  getConsents(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<UserConsent>> {
    return http.get<PaginatedList<UserConsent>>('/rails/consents', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  registerConsent(institutionId: string): Promise<Location> {
    console.log(`Registering institution [id: ${institutionId}]`);
    
    const body = {
      callbackUri: window.location.origin + "/accounts"
    }
    return http.post(`/rails/consents/${institutionId}`, body)
      .then(response => response.data);
  }

  cancelConsent(institutionId: string) {
    console.log(`Closing bank [name: ${institutionId}]`);
    return http.delete(`/rails/consents/${institutionId}`);
  }
}

const instance = new UserConsentService();
export default instance;
