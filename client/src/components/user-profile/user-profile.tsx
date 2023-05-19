import React from "react";
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';

import "./user-profile.css";

export default function UserProfileForm() {

  return (
    <Box className="panel" component="form" noValidate autoComplete="off">
      <TextField className="field" id="username" label="Username" required variant="outlined" fullWidth margin="normal" />
      <TextField className="field" id="preferredName" label="Preferred Name" variant="outlined" fullWidth margin="normal"/>
      <TextField className="field" id="title" label="Title" variant="outlined" fullWidth margin="normal" />
      <TextField className="field" id="givenName" label="Given name" required variant="outlined" fullWidth margin="normal" />
      <TextField className="field" id="familyName" label="Family name" variant="outlined" fullWidth margin="normal" />
      <TextField className="field" id="email" label="Email" required variant="outlined" fullWidth margin="normal" />
      <TextField className="field" id="phone" label="Phone" variant="outlined" fullWidth margin="normal" />
    </Box>
  );
}
