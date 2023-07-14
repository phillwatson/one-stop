import { useState } from "react";
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';

import "./login-form.css";
import GoogleSignInButton from "../oauth/google-id/google-signin-button";
import { useNotificationDispatch } from "../../contexts/notification-context";
import { useCurrentUser } from "../../contexts/user-context";
import ProfileService from "../../services/profile.service";
import { useNavigate } from "react-router-dom";
import GitLabSignInButton from "../oauth/gitlab-id/gitlab-signin-button";

interface Credentials {
  username: string,
  password: string
}

export default function LoginForm() {
  const [credentials, setCredentials] = useState<Credentials>({ username: "", password: "" });

  const showNotification = useNotificationDispatch();
  const [, setCurrentUser ] = useCurrentUser();

  const navigate = useNavigate();

  function validateForm(): boolean {
    const c = credentials;
    return c.username.length > 0 && c.password.length > 0;
  }

  function handleSubmit(event: any) {
    event.preventDefault();
    ProfileService.login(credentials.username, credentials.password)
      .then(() => {
        ProfileService.get().then(user => setCurrentUser(user))
      })
      .catch(e => {
        showNotification({ type: 'add', level: "warning", message: e.response.statusText });
      });
  }

  function handleNewUser(event: any) {
    event.preventDefault();
    navigate('/new-user');
  }

  return (
    <div className="panel">
      <form onSubmit={ handleSubmit }>
        <div className="field">
          <TextField className="field" id="username" label="Username" required variant="outlined" fullWidth margin="normal"
            value={credentials.username} onChange={(e) => setCredentials({ ...credentials, username: e.target.value})} />
        </div>
        <div className="field">
          <TextField className="field" id="password" label="Password" type="password" required variant="outlined" fullWidth margin="normal"
            value={credentials.password} onChange={(e) => setCredentials({ ...credentials, password: e.target.value})} />
        </div>
        <div className="panel">
          <Button type="submit" variant="outlined" disabled={!validateForm()}>Login</Button>
        </div>
      </form>

      <div className="panel">
        <Button variant="outlined" onClick={ handleNewUser }>I don't have an account</Button>
      </div>

      <div className="panel">
        Or, to make it easy ...
      </div>

      <div className="panel">
        <GoogleSignInButton
          clientId={'284564870769-e3qm0g1dgim9kjd1gp3qmia610evn88a.apps.googleusercontent.com'}
          redirectUri={'http://localhost/api/v1/auth/validate/google'}/>
      </div>

      <div className="panel">
        <GitLabSignInButton
          clientId={'31673b3e2b7f9ab7992084452b178b45609b93278f77f4d312ad1cf6d246b981'}
          redirectUri={'http://localhost/api/v1/auth/validate/gitlab'}/>
      </div>
    </div>
  );
}
