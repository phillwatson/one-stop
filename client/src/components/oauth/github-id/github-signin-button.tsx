import './github-signin-button.css'; // import stylesheet for styling

interface Props {
  clientId: string;
  redirectUri: string;
}

export default function GitHubSignInButton(props: Props) {
  function buttonClick() {
    var uri = 'https://accounts.google.com/o/oauth2/v2/auth' +
                '?response_type=code' +
                '&client_id=' + props.clientId +
                '&scope=openid profile email' +
                '&redirect_uri=' + props.redirectUri;

    window.location.href = encodeURI(uri);
  }

  return (
    <button className="github-sign-in-button" onClick= { buttonClick }>
      <img src="https://img.icons8.com/color/48/000000/google-logo.png" alt="GitHub Logo" />
      <span>Sign in with GitHub</span>
    </button>
  );
};
