import TextField from '@mui/material/TextField';

import "./registration-form.css";
import { RegistrationCredentials} from "../../services/user.service";

interface Props {
  profile: RegistrationCredentials;
  setter: (profile: RegistrationCredentials) => void;
}

export default function RegistrationForm(props: Props) {
  const [profile, setProfile] = [ props.profile, props.setter ];

  return (
    <div className="panel">
      <TextField className="field" id="givenName" label="Name" required variant="outlined" fullWidth margin="normal"
        value={profile.givenName} onChange={e => setProfile({...profile, givenName: e.target.value})}/>

      <TextField className="field" id="username" label="Username" required variant="outlined" fullWidth margin="normal"
        value={profile.username} onChange={e => setProfile({...profile, username: e.target.value})}/>

      <TextField className="field" id="password" label="Password" type="password" variant="outlined" fullWidth margin="normal"
        value={profile.password} onChange={e => setProfile({...profile, password: e.target.value})}/>
    </div>
  );
}
