import { useState } from "react";
import Button from '@mui/material/Button';
import Snackbar from "@mui/material/Snackbar";
import Alert, { AlertColor } from "@mui/material/Alert";
import TextField from '@mui/material/TextField';
import { useNavigate } from "react-router-dom";

import http from "../../services/http-common"
import "./login-form.css";
import GoogleSignInButton from "../oauth/google-id/google-signin-button";

interface ErrorMessage {
  severity: AlertColor | undefined,
  message: String | undefined
}

export default function LoginForm() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<Array<ErrorMessage>>([]);

  function validateForm() {
    return username.length > 0 && password.length > 0;
  }

  function handleSubmit(event: any) {
    event.preventDefault();
    http.post("/auth/login", JSON.stringify({ username, password }))
      .then(() => navigate("/accounts"))
      .catch(e => {
        setErrors(() => [ { severity: "warning", message: e.response.statusText }, ...errors ] );
      });
  }

  function showErrors(): JSX.Element[] {
    return (
      errors.map( (e, index) => 
      <Snackbar key={index} open={errors.length > 0} autoHideDuration={6000} anchorOrigin={{ vertical: 'top', horizontal: 'right'}}>
        <Alert severity={e.severity} sx={{ width: '100%' }} onClose={() => {}}>{ e.message }</Alert> 
      </Snackbar>
      ) 
    );
  }

  return (
    <div>
      <div className="panel">
        <form onSubmit={ handleSubmit }>
          <div className="field">
            <TextField className="field" id="username" label="Username" required variant="outlined" fullWidth margin="normal"
              value={username} onChange={(e) => setUsername(e.target.value)} />
          </div>
          <div className="field">
            <TextField className="field" id="password" label="Password" type="password" required variant="outlined" fullWidth margin="normal"
              value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <div className="panel">
            <Button type="submit" variant="outlined" disabled={!validateForm()}>Login</Button>
          </div>
        </form>

        <GoogleSignInButton
          clientId={'284564870769-e3qm0g1dgim9kjd1gp3qmia610evn88a.apps.googleusercontent.com'}
          redirectUri={'http://localhost/api/v1/auth/validate/google'}/>
        
        <div>
          { showErrors() }
        </div>
      </div>
    </div>
  );
}
