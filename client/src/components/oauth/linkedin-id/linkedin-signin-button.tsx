import './linkedin-signin-button.css'; // import stylesheet for styling
import UserService from "../../../services/user.service";

export default function LinkedInSignInButton() {
  function buttonClick() {
    UserService.login("linkedin");
  }

  return (
    <button className="linkedin-sign-in-button" onClick= { buttonClick }>
      <img src="/openid/linkedin.png" alt="LinkedIn Logo" />
      <span>Sign in with LinkedIn</span>
    </button>
  );
};
