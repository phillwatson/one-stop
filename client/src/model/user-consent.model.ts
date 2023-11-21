export default interface UserConsent {
  id: any,

  /**
   * The date-time on which the agreements was given.
   */
  dateGiven: string,

  /**
   * The rail ID for the institution to which the consent refers.
   */
  institutionId: string,

  /**
   * The name of the institution to which the consent refers.
   */
  institutionName: string,

  /**
   * The date-time on which the agreement expires.
   */
  agreementExpires: string,

  /**
   * The agreed number of past days for which transaction data can be obtained.
   */
  maxHistory: number,

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
