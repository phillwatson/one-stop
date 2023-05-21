import { useEffect, useState } from "react";

import Button from '@mui/material/Button';
import UserProfileForm from "../components/user-profile/user-profile";
import ProfileService from '../services/profile.service'
import UserProfile from "../model/user-profile.model";
import { useErrorsDispatch } from "../contexts/error-context";

export default function UpdateProfile() {
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

  const showError = useErrorsDispatch();

  const [profile, setProfile] = useState<UserProfile>( emptyProfile );

  useEffect(() => {
    ProfileService.get()
      .then((response) => { setProfile(response.data); } )
      .catch((err) => showError({ type: 'add', level: 'error', message: err}));
  }, [showError]);

  function validateForm() {
    return true;
  }

  function handleSubmit(event: any) {
    event.preventDefault();
    showError({ type: 'add', level: 'info', message: 'message ' + Date.now()})
  }

  return (
    <div>
      <h2>Profile information</h2>
      <hr></hr>
      <form onSubmit={ handleSubmit }>
        <UserProfileForm profile={ profile }/>
        <Button type="submit" variant="outlined" disabled={!validateForm()}>Save</Button>
      </form>
    </div>
  );
}
