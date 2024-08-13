import { TransactionDetail } from "./account.model";

export type ReportParameterType = 'STRING' | 'LONG' | 'DOUBLE' | 'BOOLEAN';

export interface ReportParameter {
  name: string;
  description: string;
  type: ReportParameterType;
  defaultValue: string;
}

export interface AuditReportTemplate {
  name: string;
  description?: string;
  parameters: Array<ReportParameter>;
}

export type AuditReportSource = 'ALL' | 'ACCOUNT' | 'CATEGORY_GROUP' | 'CATEGORY';

export interface AuditReportConfig {
  id?: string;
  version?: number;
  disabled: boolean;
  templateName: string;
  name: string;
  description?: string;
  source: AuditReportSource;
  sourceId?: string;
  uncategorisedIncluded?: boolean;
  parameters: Map<string, string>;
}

export const NULL_REPORT_CONFIG: AuditReportConfig = {
  disabled: false,
  templateName: '',
  name: '',
  description: '',
  source: 'ALL',
  sourceId: undefined,
  uncategorisedIncluded: false,
  parameters: new Map()
};

export interface AuditIssue extends TransactionDetail{
  auditConfigId: string;
  acknowledged: boolean;
}
