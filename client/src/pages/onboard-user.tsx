import { useNavigate, useSearchParams } from "react-router-dom";

import { useNotificationDispatch } from "../contexts/notification-context";
import StaticAppHeader from "../components/app-header/static-app-header";
import RegistrationForm from "../components/registration-form/registration-form";
import UserService, { RegistrationCredentials } from '../services/user.service'

export default function OnboardUser() {
  const navigate = useNavigate();
  const showNotification = useNotificationDispatch();
  const [ queryParams ] = useSearchParams();


  function handleSubmit(credentials: RegistrationCredentials) {
    credentials.token = queryParams.get("token")!!;
    UserService.completeRegistration(credentials)
      .then(() => {
        showNotification({ type: 'add', level: 'success', message: 'Your account has been opened.' });
        navigate('/profile');
      })
      .catch(error => showNotification({ type: 'add', level: 'error', message: error}));
  }

  return (
    <StaticAppHeader title="One-Stop" header="Registration information">
      <RegistrationForm onSubmit={ handleSubmit }/>
    </StaticAppHeader>
  );
}
