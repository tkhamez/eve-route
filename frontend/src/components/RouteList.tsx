import React from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { Link, List, ListItem, ListItemIcon, ListItemText, Typography } from '@material-ui/core';
import ArrowDropDownCircleOutlinedIcon from '@material-ui/icons/ArrowDropDownCircleOutlined';
import ArrowDropDownCircleTwoToneIcon from '@material-ui/icons/ArrowDropDownCircleTwoTone';
import FiberManualRecordOutlinedIcon from '@material-ui/icons/FiberManualRecordOutlined';
import SlowMotionVideoTwoToneIcon from '@material-ui/icons/SlowMotionVideoTwoTone';
import { makeStyles } from '@material-ui/core/styles';
import { RouteType, Waypoint } from '../response';

const useStyles = makeStyles((theme) => ({
  list: {
    backgroundColor: theme.palette.background.paper,
    borderRadius: '4px',
  },
  listIcon: {
    minWidth: 0,
    marginRight: '15px',
  }
}));

type Props = {
  waypoints: Array<Waypoint>,
  message: string,
}

export default function RouteList(props: Props) {
  const { t } = useTranslation();
  const classes = useStyles();

  let dotlanHref = 'https://evemaps.dotlan.net/route/';
  for (const waypoint of props.waypoints) {
    if (waypoint.wormhole) {
      continue;
    }
    dotlanHref += waypoint.systemName.replace(' ', '_');
    if (waypoint.connectionType === RouteType.Stargate) {
      dotlanHref += ':';
    } else { // Ansiblex or Temporary
      dotlanHref += '::';
    } // else = end system
  }

  return (
      <List dense={true} className={classes.list}>
        <ListItem>
          <strong>{t('routeList.route')}</strong>&nbsp;
          {props.waypoints.length > 0 &&
            <small style={{marginLeft: "auto"}}>
              <Trans i18nKey="routeList.dotlan">
                %<Link href={dotlanHref} target="_blank" rel="noopener noreferrer">%</Link>%
              </Trans>
            </small>
          }
        </ListItem>
        {props.message &&
          <ListItem>{props.message}</ListItem>
        }
        {props.waypoints.map((value, index) => {
          const last = index + 1 === props.waypoints.length;
          return (
            <ListItem key={index}>
              {! last && value.connectionType === RouteType.Temporary &&
                <ListItemIcon className={classes.listIcon}>
                  <SlowMotionVideoTwoToneIcon style={{transform: "rotate(90deg)", color: "darkgrey"}} />
                </ListItemIcon>
              }
              {! last && value.connectionType === RouteType.Ansiblex &&
                <ListItemIcon className={classes.listIcon}><ArrowDropDownCircleTwoToneIcon /></ListItemIcon>
              }
              {! last && value.connectionType === RouteType.Stargate &&
                <ListItemIcon className={classes.listIcon}><ArrowDropDownCircleOutlinedIcon/></ListItemIcon>
              }
              {last &&
                <ListItemIcon className={classes.listIcon}><FiberManualRecordOutlinedIcon /></ListItemIcon>
              }
              <ListItemText
                primary={
                  <React.Fragment>
                    {value.systemName}
                    <Typography component="span" variant="body2" color="textSecondary">
                      <small>{' ' + value.systemSecurity}</small>
                    </Typography>
                  </React.Fragment>
                }
                secondary={<small>{
                  value.connectionType === RouteType.Ansiblex ?
                    (value.ansiblexName ? value.ansiblexName : t('routeList.unknown-ansiblex')):
                    (value.connectionType === RouteType.Temporary ? t('routeList.temporary-connection') : '')
                }</small>}
              />
            </ListItem>
          )
        })}
      </List>
  )
}
