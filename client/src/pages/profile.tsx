import { useState } from "react";

import Button from '@mui/material/Button';

import { useMessageDispatch } from "../contexts/messages/context";
import { useCurrentUser } from "../contexts/user-context";
import UserProfileForm from "../components/user-profile/user-profile";
import ProfileService from '../services/profile.service'
import UserProfile from "../model/user-profile.model";
import UserConsentList from "../components/consents/user-consents";
import AuthProviderList from "../components/auth-providers/auth-provider-list";

import styles from "./profile.module.css";
import PageHeader from "../components/page-header/page-header";

const emptyProfile: UserProfile = {
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

export default function UpdateProfile() {
  const showMessage = useMessageDispatch();
  const [ currentUser, setCurrentUser ] = useCurrentUser();

  const [profile, setProfile] = useState<UserProfile>(currentUser ? currentUser : emptyProfile);

  function validateForm(): Array<string> {
    const errors = Array<string>();

    if (profile.username?.length === 0) {
      errors.push("Username is required");
    }

    if (profile.email?.length === 0) {
      errors.push("Email is required");
    }

    if (profile.givenName?.length === 0) {
      errors.push("Given name is required");
    }

    return errors;
  }

  function handleSubmit(event: any) {
    event.preventDefault();

    validateForm().forEach(value => showMessage({ type: 'add', level: 'error', text: value}))
    ProfileService.update(profile)
      .then(update => {
        setCurrentUser(update)
        showMessage({ type: 'add', level: 'success', text: 'Profile updated' });
      })
      .catch(error => showMessage(error));
  }

  return (
    <PageHeader title="Profile information" >
      <form className={ styles.profile } onSubmit={ handleSubmit }>
        <div className={ styles.panel }>
          <div className={ styles.splitpanel }>
            <div className={ styles.box }>
            <UserProfileForm profile={ profile } setter={ setProfile }/>
            </div>
          </div>
          <div className={ styles.splitpanel }>
            <div className={ `${styles.box} ${styles.bordered}` }><AuthProviderList/></div>
            <div className={ `${styles.box} ${styles.bordered}` }><UserConsentList/></div>
          </div>
        </div>
        <Button type="submit" variant="outlined" disabled={validateForm().length > 0}>Save</Button>
      </form>
    </PageHeader>
  );
}
