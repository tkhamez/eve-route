import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Checkbox, FormGroup, FormControlLabel, Grid, Link, Typography } from '@material-ui/core';
import { makeStyles } from "@material-ui/core/styles";
import { GlobalDataContext } from "../GlobalDataContext";
import axios from "axios";
import { ResponseMessage } from "../response";

const useStyles = makeStyles(() => ({
  row: {
    margin: "15px 0",
  },
  row1: {
    margin: "15px 0 0",
  },
  row2: {
    margin: "0 0 15px",
  },
  row3: {
    margin: "-5px 0 5px",
  },
}));

const Login = () => {
  const { t } = useTranslation();
  const classes = useStyles();
  const globalData = useContext(GlobalDataContext);
  const [loginResult, setLoginResult] = useState("");
  const [checkBoxes, setCheckBoxes] = React.useState({
    writeRoute: true,
    autoLocation: true,
    updateGates: false,
  });
  const [features, setFeatures] = React.useState("");

  useEffect(() => {
    // error from URL
    if (window.location.hash === '#callback-error-401') {
      window.location.hash = '';
      setLoginResult(t('login.callback-error-401'));
    }

    // error from API
    axios.get<ResponseMessage>(`${globalData.domain}/api/auth/result`).then(r => {
      if (r.data.code) {
        setLoginResult(t(`responseCode.${r.data.code}`));
      }
    }).catch(() => {});
  }, [globalData.domain, t]);

  useEffect(() => {
    let value = checkBoxes.writeRoute ? '1' : '0';
    value += checkBoxes.autoLocation ? '1' : '0';
    value += checkBoxes.updateGates ? '1' : '0';
    setFeatures(value);
  }, [checkBoxes]);

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setCheckBoxes({ ...checkBoxes, [event.target.name]: event.target.checked });
  };

  return (
    <Grid container spacing={2} className='card'>
      <Grid item xs={12}>
        <Box display="flex" flexDirection="column" alignItems="center">

          <Typography className={classes.row} variant="h6" align="center">{t('login.description')}</Typography>

          { loginResult &&
            <Typography className={classes.row1} color={"error"}>
              <strong>{loginResult}</strong>
            </Typography>
          }

          <Typography className={classes.row1}>{t('login.choose-features')}</Typography>
          <FormGroup>
            <FormControlLabel disabled control={<Checkbox checked />} label={t('login.find-route')} />
            <Typography className={classes.row3} variant={"caption"}>{t('login.find-route-description')}</Typography>

            <FormControlLabel
              control={<Checkbox checked={checkBoxes.writeRoute} name="writeRoute"
                                 onChange={handleChange} color="primary"/>}
              label={t('login.write-route')} />
            <Typography className={classes.row3} variant={"caption"}>
              {t('login.write-route-description')}
            </Typography>

            <FormControlLabel
              control={<Checkbox checked={checkBoxes.autoLocation} name="autoLocation"
                                 onChange={handleChange} color="primary"/>}
              label={t('login.read-location')} />
            <Typography className={classes.row3} variant={"caption"}>
              {t('login.read-location-description')}
            </Typography>

            <FormControlLabel
              control={<Checkbox checked={checkBoxes.updateGates} name="updateGates"
                                 onChange={handleChange} color="primary"/>}
              label={t('login.update-gates')} />
            <Typography className={classes.row3} variant={"caption"}>
              {t('login.update-gates-description')}
            </Typography>
            <Typography variant={"body2"}>{t('login.token-deletion')}</Typography>
          </FormGroup>

          <Link className={classes.row}
                href={`${globalData.domain}/api/auth/login/${features}`}
                rel="noopener noreferrer">
            <img src="/eve-sso-login-black-large.png" alt={t('login.log-in')} title={t('login.log-in')}/>
          </Link>

          <Typography className={classes.row2} variant="body2" align="center">
            {t('login.login-restriction')}
          </Typography>

        </Box>
      </Grid>
    </Grid>
  );
};

export default Login;
