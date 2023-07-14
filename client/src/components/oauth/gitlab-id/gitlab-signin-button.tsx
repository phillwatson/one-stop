import './gitlab-signin-button.css'; // import stylesheet for styling

interface Props {
  clientId: string;
  redirectUri: string;
}

export default function GitHubSignInButton(props: Props) {
  function buttonClick() {
    var uri = 'https://gitlab.com/oauth/authorize' +
                '?response_type=code' +
                '&client_id=' + props.clientId +
                '&scope=openid profile email' +
                '&redirect_uri=' + props.redirectUri;

    window.location.href = encodeURI(uri);
  }

  return (
    <button className="gitlab-sign-in-button" onClick= { buttonClick }>
      <img src="https://about.gitlab.com/images/press/press-kit-icon.svg" alt="GitLab Logo" />
      <span>Sign in with GitLab</span>
    </button>
  );
};
