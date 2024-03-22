import { useState } from "react";

import Button from '@mui/material/Button';

import { useMessageDispatch } from "../contexts/messages/context";
import { useCurrentUser } from "../contexts/user-context";
import UserProfileForm from "../components/user-profile/user-profile";
import ProfileService from '../services/profile.service'
import UserProfile from "../model/user-profile.model";
import UserConsentList from "../components/consents/user-consents";
import AuthProviderList from "../components/auth-providers/auth-provider-list";

import "./profile.css";

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
    <div>
      <h2>Profile information</h2>
      <hr></hr>
      <form onSubmit={ handleSubmit }>
        <div className="panel">
          <div className="splitpanel">
            <div className="box">
            <UserProfileForm profile={ profile } setter={ setProfile }/>
            </div>
          </div>
          <div className="splitpanel">
            <div className="box grid"><AuthProviderList/></div>
            <div className="box grid"><UserConsentList/></div>
          </div>
        </div>
        <Button type="submit" variant="outlined" disabled={validateForm().length > 0}>Save</Button>
      </form>
    </div>
  );
}
