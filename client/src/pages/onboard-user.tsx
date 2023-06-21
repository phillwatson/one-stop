import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import Button from "@mui/material/Button";

import { useNotificationDispatch } from "../contexts/notification-context";
import RegistrationForm from "../components/registration-form/registration-form";
import UserService, { RegistrationCredentials } from '../services/user.service'
import StaticAppHeader from "../components/app-header/static-app-header";

export default function OnboardUser() {
  const showNotification = useNotificationDispatch();
  const [ queryParams ] = useSearchParams();

  const [credentials, setCredentials] = useState<RegistrationCredentials>({ 
    username: "", 
    password: "",
    givenName: "",
    token: queryParams.get("token")!!
  });

  function validateForm(): Array<string> {
    const errors = Array<string>();

    if (credentials.username.length === 0) {
      errors.push("Username is required");
    }

    if (credentials.password.length === 0) {
      errors.push("Password is required");
    }

    if (credentials.givenName.length === 0) {
      errors.push("Given name is required");
    }

    return errors;
  }

  function handleSubmit(event: any) {
    event.preventDefault();

    validateForm().forEach(value => showNotification({ type: 'add', level: 'error', message: value}))
    UserService.completeRegistration(credentials)
      .then(() => {
        showNotification({ type: 'add', level: 'success', message: 'Profile updated' });
      })
      .catch(error => showNotification({ type: 'add', level: 'error', message: error}));
  }

  return (
    <StaticAppHeader title="One-Stop" header="Registration information">
        <form onSubmit={ handleSubmit }>
          <RegistrationForm profile={ credentials } setter={ setCredentials }/>
          <Button type="submit" variant="outlined" disabled={validateForm().length > 0}>Save</Button>
        </form>
    </StaticAppHeader>
  );
}
