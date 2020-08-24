import React from 'react';
import { useTranslation } from 'react-i18next';

function Login(props) {
  const { domain } = props;
  const { t } = useTranslation();

  return (
    <div>
      <a href={domain+'/api/auth/login'}>
        <img src="/eve-sso-login-black-small.png" alt={t('login.login')}/>
      </a>
    </div>
  );
}

export default Login;
