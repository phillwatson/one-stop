import './google-signin-button.css'; // import stylesheet for styling
import UserService from "../../../services/user.service";

export default function GoogleSignInButton() {
  function buttonClick() {
    UserService.login("google");
  }

  return (
    <button className="google-sign-in-button" onClick= { buttonClick }>
      <img src="/openid/google.png" alt="Google Logo" />
      <span>Sign in with Google</span>
    </button>
  );
};
