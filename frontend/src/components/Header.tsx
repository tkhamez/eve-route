import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import {
  AppBar,
  Button,
  Container,
  Slide,
  Toolbar,
  Tooltip,
  Typography,
  useScrollTrigger
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import ExitToAppIcon from '@material-ui/icons/ExitToApp';
import LocationOnOutlinedIcon from '@material-ui/icons/LocationOnOutlined';
import { GlobalDataContext } from '../GlobalDataContext';
import LanguageSwitcher from './LanguageSwitcher';
import HeaderModalButtons from './HeaderModalButtons';

interface HideOnScrollProps {
  children: React.ReactElement;
}

function HideOnScroll(props: HideOnScrollProps) {
  const { children } = props;
  const trigger = useScrollTrigger({
    threshold: 47,
    disableHysteresis: true
  });

  return (
    <Slide appear={false} direction="down" in={!trigger}>
      {children}
    </Slide>
  );
}

const useStyles = makeStyles((theme) => ({
  appBar: {
    backgroundColor: theme.palette.background.default,
    position: "sticky",
  },
  toolbar: {
    flexWrap: 'wrap',
    paddingLeft: 0,
    paddingRight: 0,
  },
  toolbarTitle: {
    marginRight: '8px',
    cursor: 'pointer',
  },
  toolbarButton: {
    position: "relative",
    top: "3px",
  },
  toolbarSpace: {
    flexGrow: 1,
  },
  logout: {
    minWidth: 0,
  },
}));

type Props = {
  connectionChanged: Function,
}

export default function Header(props: Props) {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();

  const openPage = (page: string) => {
    window.location.hash = page;
  };

  return (
    <HideOnScroll>
      <AppBar color="default" elevation={8} className={classes.appBar}>
        <Container maxWidth="lg">
          <Toolbar className={classes.toolbar} variant="dense">
            <LocationOnOutlinedIcon style={{color: '#90caf9', fontSize: '1.75rem', marginRight: '4px'}}/>
            <Typography variant="h6" noWrap className={classes.toolbarTitle} onClick={() => openPage('Home')}>
              {t('app.name')}
            </Typography>

            {globalData.user.name &&
              <Button size="small" color="primary" disableRipple className={classes.toolbarButton}
                      onClick={() => openPage('Admin')}>
                {t('header.admin')}
              </Button>
            }

            <HeaderModalButtons connectionChanged={props.connectionChanged}/>

            <span className={classes.toolbarSpace} />

            <div style={{marginLeft: '5px'}}>
              { globalData.user.name &&
                <span style={{position: 'relative', top: '1px'}}>
                  <span style={{marginRight: '8px', position: 'relative', top: '2px'}}>
                    [
                    {globalData.user.allianceTicker &&
                      <span data-title={globalData.user.allianceName} aria-label={globalData.user.allianceName}>
                        {globalData.user.allianceTicker}
                      </span>
                    }
                    {! globalData.user.allianceTicker && t('header.no-alliance') }
                    ]
                    {' '}
                    {globalData.user.name}
                  </span>

                  <Tooltip title={t('header.logout').toString()}>
                    <Button size="medium" color="secondary" className={classes.logout} onClick={globalData.logoutUser}>
                      <ExitToAppIcon />
                    </Button>
                  </Tooltip>
                </span>
              }
              <LanguageSwitcher />
            </div>

          </Toolbar>
        </Container>
      </AppBar>
    </HideOnScroll>
  );
}
