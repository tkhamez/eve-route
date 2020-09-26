import React, { useCallback, useContext, useEffect, useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { IconButton, Link, List, ListItem, ListItemIcon, ListItemText, Typography } from '@material-ui/core';
import AdjustOutlinedIcon from '@material-ui/icons/AdjustOutlined';
import ArrowDropDownCircleOutlinedIcon from '@material-ui/icons/ArrowDropDownCircleOutlined';
import ArrowDropDownCircleTwoToneIcon from '@material-ui/icons/ArrowDropDownCircleTwoTone';
import HighlightOffSharpIcon from '@material-ui/icons/HighlightOffSharp';
import RemoveCircleOutlineIcon from '@material-ui/icons/RemoveCircleOutline';
import RotateLeftSharpIcon from '@material-ui/icons/RotateLeftSharp';
import SlowMotionVideoTwoToneIcon from '@material-ui/icons/SlowMotionVideoTwoTone';
import { makeStyles } from '@material-ui/core/styles';
import axios from 'axios';
import { ConnectedSystems, ResponseConnectedSystems, ResponseSystems, RouteType, System, Waypoint } from '../response';
import { GlobalDataContext } from "../GlobalDataContext";

const useStyles = makeStyles((theme) => ({
  list: {
    backgroundColor: theme.palette.background.paper,
    borderRadius: '4px',
  },
  avoidedRemoved: {
    alignItems: 'start',
  },
  listItem: {
    alignItems: 'start',
  },
  listIcon: {
    minWidth: 0,
    marginRight: '4px',
  },
  listText: {
    marginTop: 0,
    marginBottom: 0,
  },
  listTextContent: {
    display: 'flex',
    alignItems: 'start',
  },
  security: {
    opacity: 0.7,
  },
  secondary: {
    display: 'flex',
    alignItems: 'start',
  },
  actionButton: {
    position: 'relative',
    top: '-1px',
  },
  primaryActionButton: {
    color: '#bf1350', // f50057 * 0,7
  },
  secondaryActionButton: {
    top: '-2px',
    color: '#c67613', // darkorange * 0.7
  },
}));

type Props = {
  waypoints: Array<Waypoint>,
  message: string,
  recalculateRoute: Function,
}

export default function RouteList(props: Props) {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();
  const [avoidedSystems, setAvoidedSystems] = useState<System[]>([]);
  const [removedConnections, setRemovedConnections] = useState<ConnectedSystems[]>([]);
  const [resetDisabled, setResetDisabled] = useState(true);

  // build Dotlan link
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

  const loadAvoidedSystems = useCallback(() => {
    axios.get<ResponseSystems>(`${globalData.domain}/api/route/avoided-systems`)
      .then(r => {
        setAvoidedSystems(r.data.systems);
      })
      .catch(() => {});
  }, [globalData.domain]);

  const loadRemovedConnections = useCallback(() => {
    axios.get<ResponseConnectedSystems>(`${globalData.domain}/api/route/removed-connections`)
      .then(r => {
        setRemovedConnections(r.data.connections);
      })
      .catch(() => {});
  }, [globalData.domain]);

  const avoidSystem = (systemId: bigint) => {
    axios.post(`${globalData.domain}/api/route/avoid-system/${systemId}`)
      .then(() => {
        loadAvoidedSystems();
        props.recalculateRoute();
      })
      .catch(() => {});
  };

  const removeConnection = (from: String, to: String) => {
    axios.post(`${globalData.domain}/api/route/remove-connection/${from}/${to}`)
      .then(() => {
        loadRemovedConnections();
        props.recalculateRoute();
      })
      .catch(() => {});
  };

  const resetAvoidedAndRemoved = () => {
    axios.post(`${globalData.domain}/api/route/reset-avoided-system-and-removed-connection`)
      .then(() => {
        loadAvoidedSystems();
        loadRemovedConnections();
        props.recalculateRoute();
      })
      .catch(() => {});
  };

  useEffect(() => {
    loadAvoidedSystems();
    loadRemovedConnections();
    if (removedConnections.length > 0 || avoidedSystems.length > 0) {
      setResetDisabled(false);
    } else {
      setResetDisabled(true);
    }
  }, [avoidedSystems.length, loadAvoidedSystems, loadRemovedConnections, removedConnections.length]);

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
        <ListItem className={classes.avoidedRemoved}>
          <IconButton aria-label={t('routeList.reset')} data-title={t('routeList.reset')} size="small"
                      onClick={resetAvoidedAndRemoved} color={"primary"} disabled={resetDisabled}>
            <RotateLeftSharpIcon />
          </IconButton>
          <small>
            {t('routeList.avoided-systems')}:{' '}
            {avoidedSystems.map((system) => { return system.name }).join(', ')}
            <br/>
            {t('routeList.removed-connections')}:{' '}
            {removedConnections.map((connection, index) => {
              return (
                <span key={index}>
                  <span style={{whiteSpace: 'nowrap'}}>{`${connection.system1}<>${connection.system2}`}</span>
                  {index + 1 < removedConnections.length && <span>, </span>}
                </span>
              )
            })}
          </small>
        </ListItem>
        <ListItem>
          {props.waypoints.length > 0 &&
            <small>{t('routeList.number-of-jumpsCount', {count: props.waypoints.length - 1})}</small>
          }
        </ListItem>
        {props.waypoints.map((value, index) => {
          const last = index + 1 === props.waypoints.length;
          const secondaryName = value.connectionType === RouteType.Ansiblex ?
            (value.ansiblexName ? value.ansiblexName : t('routeList.unknown-ansiblex')) :
            (value.connectionType === RouteType.Temporary ? t('routeList.temporary-connection') : '');
          return (
            <ListItem key={index} className={classes.listItem}>
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
                <ListItemIcon className={classes.listIcon}><AdjustOutlinedIcon /></ListItemIcon>
              }
              <ListItemText className={classes.listText}
                primary={
                  <span className={classes.listTextContent}>
                    {!last && index !== 0 &&
                      <IconButton className={`${classes.actionButton} ${classes.primaryActionButton}`}
                                  aria-label={t('routeList.avoid-system')}
                                  data-title={t('routeList.avoid-system')}
                                  size="small" onClick={() => avoidSystem(value.systemId)}>
                        <RemoveCircleOutlineIcon fontSize="inherit"/>
                      </IconButton>
                    }
                    {(last || index === 0) &&
                      <IconButton disabled={true}/>
                    }
                    <span>
                      {value.systemName}
                      <small className={classes.security}>{' ' + value.systemSecurity}</small>
                    </span>
                  </span>
                }
                secondary={
                  <small>
                    {(value.connectionType === RouteType.Ansiblex || value.connectionType === RouteType.Temporary) &&
                      <span className={classes.secondary}>
                        <IconButton className={`${classes.actionButton} ${classes.secondaryActionButton}`}
                                    aria-label={t('routeList.remove-connection')}
                                    data-title={t('routeList.remove-connection')}
                                    size="small"
                                    onClick={() => removeConnection(value.systemName, value.targetSystem)}>
                          <HighlightOffSharpIcon fontSize="inherit" />
                        </IconButton>
                        <span>{secondaryName}</span>
                      </span>
                    }
                  </small>
                }
              />
            </ListItem>
          )
        })}
        <ListItem>
          {props.waypoints.length > 0 &&
            <Typography variant="caption">{t('routeList.check-in-game')}</Typography>
          }
        </ListItem>
      </List>
  )
}
