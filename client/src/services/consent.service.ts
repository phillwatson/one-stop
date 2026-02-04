import UserConsent from '../model/user-consent.model';
import PaginatedList from '../model/paginated-list.model';
import http from './http-common';

class UserConsentService {
  getConsent(institutionId: string): Promise<UserConsent> {
    return http.get<UserConsent>(`/rails/consents/${institutionId}`)
      .then(response => response.data);
  }

  getConsents(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<UserConsent>> {
    return http.get<PaginatedList<UserConsent>>('/rails/consents', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  registerConsent(institutionId: string): Promise<Location> {
    const body = {
      callbackUri: window.location.origin + "/accounts"
    }
    return http.post(`/rails/consents/${institutionId}`, body)
      .then(response => response.headers["location"] as Location);
  }

  cancelConsent(institutionId: string, purge: boolean = false): Promise<any> {
    return http.delete<void>(`/rails/consents/${institutionId}?purge=${purge}`);
  }
}

const instance = new UserConsentService();
export default instance;
