import { useState } from "react";
import Button from "@mui/material/Button";

import { useMessageDispatch } from "../contexts/messages/context";
import UserService from '../services/user.service'
import StaticAppHeader from "../components/app-header/static-app-header";
import TextField from "@mui/material/TextField";

export default function NewUser() {
  const showMessage = useMessageDispatch();

  const [sendEmail, setEmail] = useState<string>("");
  const [submitted, setSubmitted] = useState<boolean>(false);

  function validateForm(): Array<string> {
    const errors = Array<string>();

    if (sendEmail.length === 0) {
      errors.push("Email address is required");
    }

    return errors;
  }

  function handleSubmit(event: any) {
    event.preventDefault();

    const errors = validateForm();
    if (errors.length > 0) {
      errors.forEach(value => showMessage({ type: 'add', level: 'error', text: value}))
    } else {
      UserService.registerNewUser(sendEmail)
        .then(() => {
          setSubmitted(true);
          showMessage({ type: 'add', level: 'success', text: 'Please check your sendEmail.' });
        })
        .catch(error => showMessage(error));
    }
  }

  return (
    <StaticAppHeader title="One-Stop" header="Please Register Your Email Address">
        <form onSubmit={ handleSubmit }>
          <div className="panel">
            <TextField className="field" id="emailAddress" label="Email Address" required variant="outlined" fullWidth margin="normal"
              value={sendEmail} onChange={ e => setEmail(e.target.value) }/>
            <Button type="submit" variant="outlined" disabled={(!submitted) && validateForm().length > 0}>Send</Button>
          </div>
        </form>
    </StaticAppHeader>
  );
}
