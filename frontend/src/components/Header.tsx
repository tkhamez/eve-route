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

export default function Header() {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();

  return (
    <HideOnScroll>
      <AppBar position="sticky" color="default" elevation={0} className={classes.appBar}>
        <Container maxWidth="md">
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
        </Container>
      </AppBar>
    </HideOnScroll>
  );
}
