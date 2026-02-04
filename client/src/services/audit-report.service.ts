import http from './http-common';
import PaginatedList from '../model/paginated-list.model';
import { AuditIssue, AuditIssueSummary, AuditReportConfig, AuditReportTemplate } from '../model/audit-report.model';

class AuditReportService {
  getReportTemplates(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<AuditReportTemplate>> {
    return http.get<PaginatedList<AuditReportTemplate>>('/rails/audit/templates', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllTemplates(): Promise<Array<AuditReportTemplate>> {
    var response = await this.getReportTemplates(0, 100);
    var templates = response.items as Array<AuditReportTemplate>;
    while (response.links.next) {
      response = await this.getReportTemplates(response.page + 1, 100);
      templates = templates.concat(response.items);
    }
    return templates;
  }

  getAuditConfigs(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<AuditReportConfig>> {
    return http.get<PaginatedList<AuditReportConfig>>('/rails/audit/configs', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllConfigs(): Promise<Array<AuditReportConfig>> {
    var response = await this.getAuditConfigs(0, 100);
    var templates = response.items as Array<AuditReportConfig>;
    while (response.links.next) {
      response = await this.getAuditConfigs(response.page + 1, 100);
      templates = templates.concat(response.items);
    }
    return templates;
  }

  createAuditConfig(config: AuditReportConfig): Promise<AuditReportConfig> {
    return http.post<AuditReportConfig>('/rails/audit/configs', config)
      .then(response => response.data);
  }

  getAuditConfig(configId: string): Promise<AuditReportConfig> {
    return http.get<AuditReportConfig>(`/rails/audit/configs/${configId}`)
      .then(response => response.data);
  }

  updateAuditConfig(config: AuditReportConfig): Promise<AuditReportConfig> {
    return http.put<AuditReportConfig>(`/rails/audit/configs/${config.id}`, config)
      .then(response => response.data);
  }

  deleteAuditConfig(configId: string): Promise<any> {
    return http.delete<void>(`/rails/audit/configs/${configId}`);
  }

  getAuditIssueSummaries(): Promise<Array<AuditIssueSummary>> {
    return http.get<Array<AuditIssueSummary>>('/rails/audit/summaries')
      .then(response => response.data);
  }

  getAuditIssues(configId: string, acknowledged?: boolean, page: number = 0, pageSize: number = 1000): Promise<PaginatedList<AuditIssue>> {
    var params: any = { "page": page, "page-size": pageSize };
    if ((acknowledged !== undefined) && (acknowledged !== null)) {
      params = { "acknowledged": acknowledged, ...params };
    }

    return http.get<PaginatedList<AuditIssue>>(`/rails/audit/configs/${configId}/issues`, { params: params })
      .then(response => response.data);
  }

  getAuditIssue(issueId: string): Promise<AuditIssue> {
    return http.get<AuditIssue>(`/rails/audit/issues/${issueId}`)
      .then(response => response.data);
  }

  updateAuditIssue(issueId: string, acknowledged: boolean): Promise<AuditIssue> {
    const request = { acknowledged: acknowledged };
    return http.put<AuditIssue>(`/rails/audit/issues/${issueId}`, request)
      .then(response => response.data);
  }

  deleteAuditIssue(issueId: string): Promise<any> {
    return http.delete<void>(`/rails/audit/issues/${issueId}`);
  }
}

const instance = new AuditReportService();
export default instance;
