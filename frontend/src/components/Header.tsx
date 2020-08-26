import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import { AppBar, Button, Toolbar, Typography } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import LanguageSwitcher from './LanguageSwitcher';
import { GlobalDataContext } from '../GlobalDataContext';

const useStyles = makeStyles((theme) => ({
  appBar: {
    borderBottom: `1px solid ${theme.palette.divider}`,
  },
  toolbar: {
    flexWrap: 'wrap',
  },
  toolbarTitle: {
    flexGrow: 1,
  },
  link: {
    margin: theme.spacing(1, 1.5),
  },
}));

function Header() {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();

  return (
    <AppBar position="sticky" color="default" elevation={0} className={classes.appBar}>
      <Toolbar className={classes.toolbar}>
        <Typography variant="h6" noWrap className={classes.toolbarTitle}>{t('app.name')}</Typography>

        { globalData.user &&
          <span>
            {globalData.user.name}, {globalData.user.alliance}
            <Button href={globalData.domain + '/api/auth/logout'} color="secondary" variant="outlined"
                    className={classes.link}>
              {t('header.logout')}
            </Button>
          </span>
        }

        <LanguageSwitcher />
      </Toolbar>
    </AppBar>
  );
}

export default Header;
