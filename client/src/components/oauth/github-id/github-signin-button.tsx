import './github-signin-button.css'; // import stylesheet for styling

interface Props {
  clientId: string;
  redirectUri: string;
}

export default function GitHubSignInButton(props: Props) {
  function buttonClick() {
    var uri = 'https://github.com/login/oauth/authorize' +
                '?response_type=code' +
                '&client_id=' + props.clientId +
                '&scope=openid profile email' +
                '&redirect_uri=' + props.redirectUri;

    window.location.href = encodeURI(uri);
  }

  return (
    <button className="github-sign-in-button" onClick= { buttonClick }>
      <img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png" alt="GitHub Logo" />
      <span>Sign in with GitHub</span>
    </button>
  );
};
