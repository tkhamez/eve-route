import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import { AppBar, Button, Container, Slide, Toolbar, Typography, useScrollTrigger } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import LanguageSwitcher from './LanguageSwitcher';
import { GlobalDataContext } from '../GlobalDataContext';

interface HideOnScrollProps {
  children: React.ReactElement;
}

function HideOnScroll(props: HideOnScrollProps) {
  const { children } = props;
  const trigger = useScrollTrigger({ target: window });

  return (
    <Slide appear={false} direction="down" in={!trigger}>
      {children}
    </Slide>
  );
}

const useStyles = makeStyles((theme) => ({
  appBar: {
    backgroundColor: theme.palette.background.default,
  },
  toolbar: {
    flexWrap: 'wrap',
    paddingLeft: 0,
    paddingRight: 0,
  },
  toolbarTitle: {
    flexGrow: 1,
  },
  link: {
    margin: theme.spacing(1, 1.5),
  },
}));

export default function Header() {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();

  return (
    <HideOnScroll>
      <AppBar position="sticky" color="default" elevation={8} className={classes.appBar}>
        <Container maxWidth="md">
          <Toolbar className={classes.toolbar} variant="dense">
            <Typography variant="h6" noWrap className={classes.toolbarTitle}>{t('app.name')}</Typography>

            { globalData.user.name &&
              <span>
                {globalData.user.name}, {globalData.user.alliance}
                <Button onClick={globalData.logoutUser} color="secondary" variant="outlined" className={classes.link}>
                  {t('header.logout')}
                </Button>
              </span>
            }

            <LanguageSwitcher />
          </Toolbar>
        </Container>
      </AppBar>
    </HideOnScroll>
  );
}
