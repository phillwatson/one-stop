import LoginForm from '../components/login-form/login-form';
import StaticAppHeader from '../components/app-header/static-app-header';

export default function SignIn() {
  return (
    <StaticAppHeader title="One-Stop" header="Please Sign-In">
      <LoginForm />
    </StaticAppHeader>
  );
}
