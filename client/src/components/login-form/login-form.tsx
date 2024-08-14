import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';

import "./login-form.css";
import { useMessageDispatch } from "../../contexts/messages/context";
import { useCurrentUser } from "../../contexts/user-context";
import ProfileService from "../../services/profile.service";
import GoogleSignInButton from "../oauth/google-id/google-signin-button";
import GitLabSignInButton from "../oauth/gitlab-id/gitlab-signin-button";
import GitHubSignInButton from "../oauth/github-id/github-signin-button";

interface Credentials {
  username: string,
  password: string
}

export default function LoginForm() {
  const [credentials, setCredentials] = useState<Credentials>({ username: "", password: "" });

  const showMessage = useMessageDispatch();
  const [, setCurrentUser ] = useCurrentUser();

  const navigate = useNavigate();

  function validateForm(): boolean {
    const c = credentials;
    return c.username.length > 0 && c.password.length > 0;
  }

  function handleSubmit(event: any) {
    event.preventDefault();
    ProfileService.login(credentials.username, credentials.password)
      .then(() =>  ProfileService.get().then(user => setCurrentUser(user)))
      .catch(e => showMessage(e));
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
          <Button type="submit" variant="contained" disabled={!validateForm()}>Login</Button>
        </div>
      </form>

      <div className="panel">
        <Button variant="outlined" onClick={ handleNewUser }>I don't have an account</Button>
      </div>

      <div className="panel">
        Or, to make it easy ...
      </div>

      <div className="panel">
        <GoogleSignInButton/>
      </div>

      <div className="panel">
        <GitLabSignInButton/>
      </div>

      <div className="panel">
        <GitHubSignInButton/>
      </div>
    </div>
  );
}
