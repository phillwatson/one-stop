import TextField from '@mui/material/TextField';

import UserProfile from "../../model/user-profile.model";

interface Props {
  profile: UserProfile;
  setter: (profile: UserProfile) => void;
}

export default function UserProfileForm(props: Props) {
  const [profile, setProfile] = [ props.profile, props.setter ];

  return (
    <>
      <TextField id="username" label="Username" required variant="outlined" fullWidth margin="normal"
        value={profile.username} onChange={e => setProfile({...profile, username: e.target.value})}/>

      <TextField id="preferredName" label="Preferred Name" variant="outlined" fullWidth margin="normal"
        value={profile.preferredName} onChange={e => setProfile({...profile, preferredName: e.target.value})}/>

      <TextField id="title" label="Title" variant="outlined" fullWidth margin="normal"
        value={profile.title} onChange={e => setProfile({...profile, title: e.target.value})}/>

      <TextField id="givenName" label="Given name" required variant="outlined" fullWidth margin="normal"
        value={profile.givenName} onChange={e => setProfile({...profile, givenName: e.target.value})}/>

      <TextField id="familyName" label="Family name" variant="outlined" fullWidth margin="normal"
        value={profile.familyName} onChange={e => setProfile({...profile, familyName: e.target.value})}/>

      <TextField id="email" label="Email" required variant="outlined" fullWidth margin="normal"
        value={profile.email} onChange={e => setProfile({...profile, email: e.target.value})}/>

      <TextField id="phone" label="Phone" variant="outlined" fullWidth margin="normal"
        value={profile.phone} onChange={e => setProfile({...profile, phone: e.target.value})}/>
    </>
  );
}
