import Notification from '../model/notification-model';
import PaginatedList from '../model/paginated-list.model';
import http from './http-common';

class NotificationService {
  getNotifications(after: string, page: number = 0, pageSize: number = 20): Promise<PaginatedList<Notification>> {
    return http.get<PaginatedList<Notification>>('/notifications', { params: { "after": after, "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  deleteNotification(notificationId: string) {
    console.log(`Deleting notification [id: ${notificationId}]`);
    return http.delete(`/rails/notifications/${notificationId}`);
  }
}

const instance = new NotificationService();
export default instance;
