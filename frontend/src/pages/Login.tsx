import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Link } from '@material-ui/core';
import { GlobalDataContext } from "../GlobalDataContext";

const Login = () => {
  const globalData = useContext(GlobalDataContext);
  const { t } = useTranslation();

  return (
      <Box display="flex" justifyContent="center">
        <Link href={globalData.domain+'/api/auth/login'} rel="noopener noreferrer">
          <img src="/eve-sso-login-black-large.png" alt={t('login.log-in')} title={t('login.log-in')}/>
        </Link>
      </Box>
  );
};

export default Login;
