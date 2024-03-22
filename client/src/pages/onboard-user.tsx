import { useNavigate, useSearchParams } from "react-router-dom";

import { useMessageDispatch } from "../contexts/messages/context";
import StaticAppHeader from "../components/app-header/static-app-header";
import RegistrationForm from "../components/registration-form/registration-form";
import UserService, { RegistrationCredentials } from '../services/user.service'

export default function OnboardUser() {
  const navigate = useNavigate();
  const showMessage = useMessageDispatch();
  const [ queryParams ] = useSearchParams();


  function handleSubmit(credentials: RegistrationCredentials) {
    credentials.token = queryParams.get("token")!!;
    UserService.completeRegistration(credentials)
      .then(() => {
        showMessage({ type: 'add', level: 'success', text: 'Your account has been opened.' });
        navigate('/profile');
      })
      .catch(error => showMessage(error));
  }

  return (
    <StaticAppHeader title="One-Stop" header="Registration information">
      <RegistrationForm onSubmit={ handleSubmit }/>
    </StaticAppHeader>
  );
}
