export default interface ServiceErrorResponse {
  correlationId: any,
  severity: string,
  errors: Array<ServiceError>
}

export interface ServiceError {
  severity: string,
  messageId: string,
  message: string,
  contextAttributes: any;
}