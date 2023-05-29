import { useState } from "react";

import Button from '@mui/material/Button';

import { useNotificationDispatch } from "../contexts/notification-context";
import { useCurrentUser } from "../contexts/user-context";
import UserProfileForm from "../components/user-profile/user-profile";
import ProfileService from '../services/profile.service'
import UserProfile from "../model/user-profile.model";

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
  const showNotification = useNotificationDispatch();
  const [ currentUser, setCurrentUser ] = useCurrentUser();

  const [profile, setProfile] = useState<UserProfile>(currentUser ? currentUser : emptyProfile);

  function validateForm(): Array<string> {
    const errors = Array<string>();

    if (profile.username.length === 0) {
      errors.push("Username is required");
    }

    if (profile.email.length === 0) {
      errors.push("Email is required");
    }

    if (profile.givenName.length === 0) {
      errors.push("Given name is required");
    }

    return errors;
  }

  function handleSubmit(event: any) {
    event.preventDefault();

    validateForm().forEach(value => showNotification({ type: 'add', level: 'error', message: value}))
    ProfileService.update(profile)
      .then(() => {
        setCurrentUser(profile)
        showNotification({ type: 'add', level: 'success', message: 'Profile updated' });
      })
      .catch(error => showNotification({ type: 'add', level: 'error', message: error}));
  }

  return (
    <div>
      <h2>Profile information</h2>
      <hr></hr>
      <form onSubmit={ handleSubmit }>
        <UserProfileForm profile={ profile } setter={ setProfile }/>
        <Button type="submit" variant="outlined" disabled={validateForm().length > 0}>Save</Button>
      </form>
    </div>
  );
}
