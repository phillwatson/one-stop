import { useState } from "react";
import Button from "@mui/material/Button";

import { useNotificationDispatch } from "../contexts/notification-context";
import UserService from '../services/user.service'
import StaticAppHeader from "../components/app-header/static-app-header";
import TextField from "@mui/material/TextField";

export default function NewUser() {
  const showNotification = useNotificationDispatch();

  const [email, setEmail] = useState<string>("");

  function validateForm(): Array<string> {
    const errors = Array<string>();

    if (email.length === 0) {
      errors.push("Email address is required");
    }

    return errors;
  }

  function handleSubmit(event: any) {
    event.preventDefault();

    validateForm().forEach(value => showNotification({ type: 'add', level: 'error', message: value}))
    UserService.registerNewUser(email)
      .then(() => {
        showNotification({ type: 'add', level: 'success', message: 'Please check your email.' });
      })
      .catch(error => {
        showNotification({ type: 'add', level: 'error', message: error});
      });
  }

  return (
    <StaticAppHeader title="One-Stop" header="Please Register Your Email Address">
        <form onSubmit={ handleSubmit }>
          <div className="panel">
            <TextField className="field" id="emailAddress" label="Email Address" required variant="outlined" fullWidth margin="normal"
              value={email} onChange={ e => setEmail(e.target.value) }/>
            <Button type="submit" variant="outlined" disabled={validateForm().length > 0}>Send</Button>
          </div>
        </form>
    </StaticAppHeader>
  );
}
