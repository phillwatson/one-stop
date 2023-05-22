export interface ContextParameter {
  name: string,
  value: string
}

export default interface ServiceError {
  correlationId: any,
  severity: string,
  messageId: string,
  message: string,
  contextAttributes: Array<ContextParameter>
}