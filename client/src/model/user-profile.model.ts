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

export const NULL_PROFILE: UserProfile = {
  id: undefined,
  username: '',
  preferredName: '',
  title: '',
  givenName: '',
  familyName: '',
  email: '',
  phone: '',
  dateCreated: undefined,
  dateOnboarded: undefined
};

export interface UserAuthProvidersResponse {
  authProviders: Array<UserAuthProvider>;
}

export interface UserAuthProvider {
  id: string;
  name: string;
  logo: string;
  dateCreated: string;
  dateLastUsed: string;
}
