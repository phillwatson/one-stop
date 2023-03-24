import './google-signin-button.css'; // import stylesheet for styling

interface Props {
  clientId: string;
  redirectUri: string;
}

export default function GoogleSignInButton(props: Props) {
  function buttonClick() {
    var uri = 'https://accounts.google.com/o/oauth2/auth' +
                '?response_type=code' +
                '&client_id=' + props.clientId +
                '&scope=openid profile email' +
                '&redirect_uri=' + props.redirectUri;

    window.location.href = encodeURI(uri);
  }

  return (
    <button className="google-sign-in-button" onClick= { buttonClick }>
      <img src="https://img.icons8.com/color/48/000000/google-logo.png" alt="Google Logo" />
      <span>Sign in with Google</span>
    </button>
  );
};
