import http from './http-common';
import PaginatedList from '../model/paginated-list.model';
import { AuditIssue, AuditReportConfig, AuditReportTemplate } from '../model/audit-report.model';

class AuditReportService {
  getReportTemplates(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<AuditReportTemplate>> {
    console.log(`List audit report templates [page: ${page}, pageSize: ${pageSize}]`);
    return http.get<PaginatedList<AuditReportTemplate>>('/rails/audit/templates', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllTemplates(): Promise<Array<AuditReportTemplate>> {
    console.log('Retrieving ALL report templates');
    var response = await this.getReportTemplates(0, 100);
    var templates = response.items as Array<AuditReportTemplate>;
    while (response.links.next) {
      response = await this.getReportTemplates(response.page + 1, 100);
      templates = templates.concat(response.items);
    }
    return templates;
  }

  getAuditConfigs(page: number = 0, pageSize: number = 1000): Promise<PaginatedList<AuditReportConfig>> {
    console.log(`List audit report configs [page: ${page}, pageSize: ${pageSize}]`);
    return http.get<PaginatedList<AuditReportConfig>>('/rails/audit/configs', { params: { "page": page, "page-size": pageSize }})
      .then(response => response.data);
  }

  async fetchAllConfigs(): Promise<Array<AuditReportConfig>> {
    console.log('Retrieving ALL report configs');
    var response = await this.getAuditConfigs(0, 100);
    var templates = response.items as Array<AuditReportConfig>;
    while (response.links.next) {
      response = await this.getAuditConfigs(response.page + 1, 100);
      templates = templates.concat(response.items);
    }
    return templates;
  }

  createAuditConfig(config: AuditReportConfig): Promise<AuditReportConfig> {
    console.log(`Create audit report config [name: ${config.name}]`);
    return http.post<AuditReportConfig>('/rails/audit/configs', config)
      .then(response => response.data);
  }

  getAuditConfig(configId: string): Promise<AuditReportConfig> {
    console.log(`Get audit report config [id: ${configId}]`);
    return http.get<AuditReportConfig>(`/rails/audit/configs/${configId}`)
      .then(response => response.data);
  }

  updateAuditConfig(config: AuditReportConfig): Promise<AuditReportConfig> {
    console.log(`Updating audit report config [id: ${config.id}]`);
    return http.put<AuditReportConfig>(`/rails/audit/configs/${config.id}`, config)
      .then(response => response.data);
  }

  deleteAuditConfig(configId: string): Promise<any> {
    console.log(`Delete audit report config [id: ${configId}]`);
    return http.delete<any>(`/rails/audit/configs/${configId}`);
  }

  getAuditIssues(configId: string, acknowledged?: boolean, page: number = 0, pageSize: number = 1000): Promise<PaginatedList<AuditIssue>> {
    console.log(`Get audit issues [configId: ${configId}, acknowledged: ${acknowledged}, page: ${page}, pageSize: ${pageSize}]`);
    return http.get<PaginatedList<AuditIssue>>(`/rails/audit/configs/${configId}/issues`,
       { params: { "page": page, "page-size": pageSize, "acknowledged": acknowledged }})
      .then(response => response.data);
  }

  getAuditIssue(issueId: string): Promise<AuditIssue> {
    console.log(`Get audit issue [issueId: ${issueId}]`);
    return http.get<AuditIssue>(`/rails/audit/issues/${issueId}`)
      .then(response => response.data);
  }

  updateAuditIssue(issueId: string, acknowledged: boolean): Promise<AuditIssue> {
    console.log(`Update audit issue [issueId: ${issueId}, acknowledged: ${acknowledged}]`);
    const request = { acknowledged: acknowledged };
    return http.put<AuditIssue>(`/rails/audit/issues/${issueId}`, request)
      .then(response => response.data);
  }

  deleteAuditIssue(issueId: string): Promise<any> {
    console.log(`Delete audit issue [issueId: ${issueId}]`);
    return http.delete<any>(`/rails/audit/issues/${issueId}`);
  }
}

const instance = new AuditReportService();
export default instance;
