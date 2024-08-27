import './github-signin-button.css'; // import stylesheet for styling
import UserService from "../../../services/user.service";

export default function GitHubSignInButton() {
  function buttonClick() {
    UserService.login("github");
  }

  return (
    <button className="github-sign-in-button" onClick= { buttonClick }>
      <img src="/openid/github.png" alt="GitHub Logo" />
      <span>Sign in with GitHub</span>
    </button>
  );
};
