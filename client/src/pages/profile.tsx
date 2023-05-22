import { useEffect, useState } from "react";

import Button from '@mui/material/Button';
import UserProfileForm from "../components/user-profile/user-profile";
import ProfileService from '../services/profile.service'
import { useErrorsDispatch } from "../contexts/error-context";
import { useCurrentUser } from "../contexts/user-context";
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
  const showError = useErrorsDispatch();
  const [ currentUser, setCurrentUser ] = [ ...useCurrentUser() ];

  const [profile, setProfile] = useState<UserProfile>(currentUser ? currentUser : emptyProfile);

  useEffect(() => {
    ProfileService.get()
      .then((response) => setCurrentUser(response.data) )
      .catch((err) => showError({ type: 'add', level: 'error', message: err}));
  }, [setCurrentUser, showError]);

  function validateForm() {
    return true;
  }

  function handleSubmit(event: any) {
    event.preventDefault();
    ProfileService.update(profile)
      .then(() => showError({ type: 'add', level: 'success', message: 'Profile updated' }))
      .catch(error => showError({ type: 'add', level: 'error', message: error}));
  }

  return (
    <div>
      <h2>Profile information</h2>
      <hr></hr>
      <form onSubmit={ handleSubmit }>
        <UserProfileForm profile={ profile } setter={ setProfile }/>
        <Button type="submit" variant="outlined" disabled={!validateForm()}>Save</Button>
      </form>
    </div>
  );
}
