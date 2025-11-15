import { useState } from "react";

import Button from '@mui/material/Button';
import Paper from "@mui/material/Paper";
import Grid from "@mui/material/Grid";

import { useMessageDispatch } from "../contexts/messages/context";
import { useCurrentUser } from "../contexts/user-context";
import UserProfileForm from "../components/user-profile/user-profile";
import ProfileService from '../services/profile.service'
import UserProfile, { NULL_PROFILE } from "../model/user-profile.model";
import UserConsentList from "../components/consents/user-consents";
import AuthProviderList from "../components/auth-providers/auth-provider-list";

import PageHeader from "../components/page-header/page-header";

export default function UpdateProfile() {
  const showMessage = useMessageDispatch();
  const [ currentUser, setCurrentUser ] = useCurrentUser();

  const [profile, setProfile] = useState<UserProfile>(currentUser ? currentUser : NULL_PROFILE);

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
    <PageHeader title="Profile Information" >
      <form onSubmit={ handleSubmit }>
        <Grid container direction="row" justifyContent="center" rowSpacing={ 3 }>
          <Grid item padding={ 5 }>
            <UserProfileForm profile={ profile } setter={ setProfile }/>
          </Grid>

          <Grid container direction="row" justifyContent="center" columns={ 2 } columnGap={ 2 } rowGap={ 2 }>
            <Grid item>
              <Paper square={ false } variant="outlined" sx={{ minWidth: "650px", maxWidth: "650px" }}>
                <AuthProviderList/>
              </Paper>
            </Grid>
            <Grid item>
              <Paper square={ false } variant="outlined" sx={{ minWidth: "650px", maxWidth: "650px" }}>
                <UserConsentList/>
              </Paper>
            </Grid>
          </Grid>

          <Grid container direction="row" justifyContent="flex-end" columnGap={ 2 } padding={ 2 }>
            <Button type="submit" variant="contained" disabled={validateForm().length > 0}>Save</Button>
          </Grid>
        </Grid>
      </form>
    </PageHeader>
  );
}
