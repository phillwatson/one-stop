import { useState } from 'react';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';

import "./registration-form.css";
import { RegistrationCredentials} from "../../services/user.service";
import { useMessageDispatch } from '../../contexts/messages/context';

interface Props {
  onSubmit: (profile: RegistrationCredentials) => void;
}

export default function RegistrationForm(props: Props) {
  const showMessage = useMessageDispatch();

  const [profile, setProfile] = useState<RegistrationCredentials>({ 
    username: "", 
    password: "",
    givenName: "",
    token: ""
  });


  function validateForm(): Array<string> {
    const errors = Array<string>();

    const form = profile;
    if (form.username.length === 0) {
      errors.push("Username is required");
    }

    if (form.password.length === 0) {
      errors.push("Password is required");
    }

    if (form.givenName.length === 0) {
      errors.push("Given name is required");
    }

    return errors;
  }

  function handleSubmit(event: any) {
    event.preventDefault();

    const errors = validateForm();
    if (errors.length > 0) {
      errors.forEach(value => showMessage({ type: 'add', level: 'error', text: value}))
    } else {
      props.onSubmit(profile);
    }
  }

  return (
    <form onSubmit={ handleSubmit } className="panel">
      <TextField className="field" id="givenName" label="Name" required variant="outlined" fullWidth margin="normal"
        value={profile.givenName} onChange={e => setProfile({...profile, givenName: e.target.value})}/>

      <TextField className="field" id="username" label="Username" required variant="outlined" fullWidth margin="normal"
        value={profile.username} onChange={e => setProfile({...profile, username: e.target.value})}/>

      <TextField className="field" id="password" label="Password" required type="password" variant="outlined" fullWidth margin="normal"
        value={profile.password} onChange={e => setProfile({...profile, password: e.target.value})}/>

      <Button type="submit" variant="outlined" disabled={validateForm().length > 0}>Save</Button>
    </form>
  );
}

