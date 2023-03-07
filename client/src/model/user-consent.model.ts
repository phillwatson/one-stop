export default interface UserConsent {
  id: any,
  dateCreated: Date,
  dateAccepted?: Date,
  dateDenied?: Date,
  dateCancelled?: Date,
  userId: any,

  /**
   * The rail ID for the institution to which the consent refers.
   */
  institutionId: string,

  /**
   * The rail ID for the agreement to which the consent refers.
   */
  agreementId: string,

  /**
   * The date-time on which the agreement expires.
   */
  agreementExpires: Date,

  /**
   * The agreed number of past days for which transaction data can be obtained.
   */
  maxHistory: number,

  /**
   * The rail ID for the requisition for access to which the consent refers.
   */
  requisitionId: string,

  /**
   * Indicates the position in the flow to obtain consent from the user.
   */
  status: string,

  /**
   * If consent is denied, this records the error code returned by the rail.
   */
  errorCode?: string,

  /**
   * If consent is denied, this records the detail of the error returned by the rail.
   */
  errorDetail?: string
}
