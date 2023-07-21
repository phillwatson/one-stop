import './gitlab-signin-button.css'; // import stylesheet for styling
import UserService from "../../../services/user.service";

export default function GitHubSignInButton() {
  function buttonClick() {
    UserService.login("gitlab");
  }

  return (
    <button className="gitlab-sign-in-button" onClick= { buttonClick }>
      <img src="https://about.gitlab.com/images/press/press-kit-icon.svg" alt="GitLab Logo" />
      <span>Sign in with GitLab</span>
    </button>
  );
};
