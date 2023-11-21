import { useEffect, useState } from 'react';

import { SxProps } from '@mui/material/styles';
import TextField from '@mui/material/TextField';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';

import "./user-profile.css";
import UserProfile, { UserAuthProvider } from "../../model/user-profile.model";
import ProfileService from "../../services/profile.service"

interface Props {
  profile: UserProfile;
  setter: (profile: UserProfile) => void;
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

export default function UserProfileForm(props: Props) {
  const [profile, setProfile] = [ props.profile, props.setter ];

  const [authProviders, setAuthProviders] = useState<Array<UserAuthProvider>>([]);

  useEffect(() => {
    ProfileService.getAuthProviders().then( response => setAuthProviders(response));
  }, []);

  return (
    <div className="panel">
      <TextField className="field" id="username" label="Username" required variant="outlined" fullWidth margin="normal"
        value={profile.username} onChange={e => setProfile({...profile, username: e.target.value})}/>

      <TextField className="field" id="preferredName" label="Preferred Name" variant="outlined" fullWidth margin="normal"
        value={profile.preferredName} onChange={e => setProfile({...profile, preferredName: e.target.value})}/>

      <TextField className="field" id="title" label="Title" variant="outlined" fullWidth margin="normal"
        value={profile.title} onChange={e => setProfile({...profile, title: e.target.value})}/>

      <TextField className="field" id="givenName" label="Given name" required variant="outlined" fullWidth margin="normal"
        value={profile.givenName} onChange={e => setProfile({...profile, givenName: e.target.value})}/>

      <TextField className="field" id="familyName" label="Family name" variant="outlined" fullWidth margin="normal"
        value={profile.familyName} onChange={e => setProfile({...profile, familyName: e.target.value})}/>

      <TextField className="field" id="email" label="Email" required variant="outlined" fullWidth margin="normal"
        value={profile.email} onChange={e => setProfile({...profile, email: e.target.value})}/>

      <TextField className="field" id="phone" label="Phone" variant="outlined" fullWidth margin="normal"
        value={profile.phone} onChange={e => setProfile({...profile, phone: e.target.value})}/>

      <Table size="small" aria-label="authproviders">
        <caption><i>You're registered with the above auth providers</i></caption>
        <TableHead>
          <TableRow>
            <TableCell sx={colhead} colSpan={2}>Auth Provider</TableCell>
            <TableCell sx={colhead}>Created</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          { authProviders.map(authProvider => (
            <TableRow key={authProvider.name}>
              <TableCell><img src={ authProvider.logo } alt="{ authProvider.name } logo" width="32px" height="32px"/></TableCell>
              <TableCell width={"70%"}>{authProvider.name}</TableCell>
              <TableCell>{new Date(authProvider.dateCreated).toLocaleDateString("en-GB")}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
