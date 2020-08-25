import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import { GlobalDataContext } from "../GlobalDataContext";

const Login = () => {
  const globalData = useContext(GlobalDataContext);
  const { t } = useTranslation();

  return (
    <div>
      <a href={globalData.domain+'/api/auth/login'}>
        <img src="/eve-sso-login-black-small.png" alt={t('login.login')}/>
      </a>
    </div>
  );
};

export default Login;
