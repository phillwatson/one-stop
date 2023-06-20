import { useState } from "react";

import { useNotificationDispatch } from "../contexts/notification-context";
import RegistrationForm from "../components/registration-form/registration-form";
import UserService, { RegistrationCredentials } from '../services/user.service'
import Button from "@mui/material/Button";
import Box from "@mui/material/Box";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";

const appTitle = "One Stop";

function AppHeader() {
  return (
    <AppBar position="fixed">
      <Toolbar>
        <Typography variant="h6" noWrap component="div">
          { appTitle }
        </Typography>
      </Toolbar>
    </AppBar>
  );
}
export default function OnboardUser() {
  const showNotification = useNotificationDispatch();

  const [credentials, setCredentials] = useState<RegistrationCredentials>({ username: "", password: "", givenName: "", token: "" });

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
    <Box sx={{ display: 'flex' }}>
      <AppHeader/>
      <div style={{ padding: "90px", flexGrow: 1 }}>
        <h2>Registration information</h2>
        <hr></hr>
        <form onSubmit={ handleSubmit }>
          <RegistrationForm profile={ credentials } setter={ setCredentials }/>
          <Button type="submit" variant="outlined" disabled={validateForm().length > 0}>Save</Button>
        </form>
      </div>
    </Box>
  );
}
