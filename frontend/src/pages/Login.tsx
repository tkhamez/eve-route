import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Grid, Link, Typography } from '@material-ui/core';
import { GlobalDataContext } from "../GlobalDataContext";
import axios from "axios";
import { ResponseMessage } from "../response";
import { makeStyles } from "@material-ui/core/styles";

const useStyles = makeStyles(() => ({
  row: {
    margin: "15px 0",
  },
  row2: {
    margin: "0 0 15px",
  },
}));

const Login = () => {
  const { t } = useTranslation();
  const classes = useStyles();
  const globalData = useContext(GlobalDataContext);
  const [loginResult, setLoginResult] = useState("");

  useEffect(() => {
    axios.get<ResponseMessage>(`${globalData.domain}/api/auth/result`).then(r => {
      if (r.data.code) {
        setLoginResult(t(`responseCode.${r.data.code}`));
      }
    }).catch(() => {
    });
  }, [globalData.domain, t]);

  return (
    <div className='grid-spacing-2-wrapper'>
      <Grid container spacing={2} className='card'>
        <Grid item xs={12}>
          <Box display="flex" flexDirection="column" alignItems="center">

            <Typography className={classes.row}>{t('login.description')}</Typography>

            <Link  className={classes.row} href={globalData.domain+'/api/auth/login'} rel="noopener noreferrer">
              <img src="/eve-sso-login-black-large.png" alt={t('login.log-in')} title={t('login.log-in')}/>
            </Link>
            { loginResult && <Typography className={classes.row2} color={"error"}>{loginResult}</Typography>}

          </Box>
        </Grid>
      </Grid>
    </div>
  );
};

export default Login;
