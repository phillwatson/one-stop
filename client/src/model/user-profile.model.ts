export default interface UserProfile {
  id: any;
  username: string;
  preferredName: string;
  title: string;
  givenName: string;
  familyName: string;
  email: string;
  phone: string;
  dateCreated: Date | undefined;
  dateOnboarded: Date | undefined;
}

export interface UserAuthProvidersResponse {
  authProviders: Array<UserAuthProvider>;
}

export interface UserAuthProvider {
  name: string;
  logo: string;
  dateCreated: Date;
}