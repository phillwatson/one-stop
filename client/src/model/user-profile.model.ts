export default interface UserProfile {
  id: any,
  username: string,
  preferredName: string,
  title: string,
  givenName: string,
  familyName: string,
  email: string,
  phone: string,
  dateCreated: Date | undefined,
  dateOnboarded: Date | undefined
}