import React, { useState } from "react";
import Button from '@mui/material/Button';

import http from "../../services/http-common"
import "./login-form.css";

export default function LoginForm() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  React.useEffect(() => {
    http.get("/auth/logout");
  }, []);

  function validateForm() {
    return username.length > 0 && password.length > 0;
  }

  function handleSubmit(event: any) {
    event.preventDefault();
    http.post("/auth/login", JSON.stringify({ username, password }))
      .then(() => null)
      .catch(error => setError(error.response.statusText));
  }

  return (
    <div className="Login">
      <form onSubmit={ handleSubmit }>
        <div className="panel">
          <div className="field">
            <label>Username:</label>
            <input autoFocus type="text" value={username} onChange={(e) => setUsername(e.target.value)} />
          </div>
          <div className="field">
            <label>Password:</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <Button type="submit" variant="outlined" disabled={!validateForm()}>Login</Button>
        </div>
        <div className="error-panel">
          { error }
        </div>
      </form>
    </div>
  );
}
