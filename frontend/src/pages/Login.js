import React from 'react';

function Login(props) {
  const { t, domain } = props;

  return (
    <div>
      <a href={domain+'/api/auth/login'}>
        <img src="/eve-sso-login-black-small.png" alt={t('login.login')}/>
      </a>
    </div>
  );
}

export default Login;
