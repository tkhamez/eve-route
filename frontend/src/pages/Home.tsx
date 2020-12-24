import React from 'react';
import { WithTranslation, withTranslation } from 'react-i18next';
import { TFunction } from 'i18next';
import { Box, Button, createStyles, Grid, IconButton, Theme, Typography } from '@material-ui/core';
import { withStyles } from '@material-ui/styles';
import SwapHorizIcon from '@material-ui/icons/SwapHoriz';
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseMapConnections, ResponseMessage, ResponseRouteFind, Waypoint } from '../response';
import SystemInput from '../components/SystemInput';
import RouteList from '../components/RouteList';
import Map from '../components/Map';

const styles = (theme: Theme) => createStyles({
  swapButton: {
    [theme.breakpoints.down('xs')]: {
      paddingTop: 0,
      paddingBottom: 0,
    }
  }
});

interface Props extends WithTranslation {
  t: TFunction,
  classes: any,
}

type HomeState = {
  routeFrom: string,
  routeTo: string,
  startSystemInput: string,
  endSystemInput: string,
  buttonRouteFindDisabled: boolean,
  buttonRouteSetDisabled: boolean,
  routeFindResultMessage: string,
  numberOfRoutes: number,
  activeRoute: number,
  routeFindResultWaypoints: Array<Waypoint>,
  mapConnections: ResponseMapConnections|null,
  routeSetResult: string,
}

class Home extends React.Component<Props, HomeState> {
  t: Function;
  private esiRoute: Array<Waypoint> = [];
  private routes: Array<Array<Waypoint>> = [];

  static contextType = GlobalDataContext;

  render() {
    const { classes } = this.props;
    const { t } = this.props;

    return (
      <div className='grid-spacing-2-wrapper'>

        <Grid container spacing={2} className='card'>
          <Grid item xs={12}>
            <Typography variant="h6" align="center">{t('home.select-systems')}</Typography>
          </Grid>

          <Grid item sm={5} xs={12}>
            <Box display="flex" justifyContent="center">
              <SystemInput fieldId="start-system" fieldName={t('home.start-system')} onChange={this.startChanged}
                           findRoute={this.calculateRoute} fieldValue={this.state.startSystemInput} />
            </Box>
          </Grid>
          <Grid item sm={2} xs={12}>
            <Box display="flex" justifyContent="center">
              <IconButton color="primary" className={classes.swapButton} title={t('home.swap-systems')}
                          onClick={this.swapSystems} disabled={this.state.buttonRouteFindDisabled}>
                <SwapHorizIcon />
              </IconButton>
            </Box>
          </Grid>
          <Grid item sm={5} xs={12}>
            <Box display="flex" justifyContent="center">
              <SystemInput fieldId="end-system" fieldName={t('home.end-system')} onChange={this.endChanged}
                           findRoute={this.calculateRoute} fieldValue={this.state.endSystemInput} />
            </Box>
          </Grid>

          <Grid item xs={6}>
            <Box display="flex" justifyContent="flex-end">
              <Button variant="contained" color="primary" onClick={() => this.routeFind()}
                      disabled={this.state.buttonRouteFindDisabled}>
                {t('home.find-route')}
              </Button>
            </Box>
          </Grid>
          <Grid item xs={6}>
            <Box display="flex" justifyContent="flex-start">
              <Typography>
                <Button variant="contained" color="primary" onClick={this.routeSet}
                        disabled={this.state.buttonRouteSetDisabled}>
                  {t('home.set-route')}
                </Button>
                {' '}{this.state.routeSetResult}
              </Typography>
            </Box>
          </Grid>

          <Grid item sm={4} xs={12}>
            <RouteList waypoints={this.state.routeFindResultWaypoints}
                       message={this.state.routeFindResultMessage}
                       numberOfRoutes={this.state.numberOfRoutes}
                       activeRoute={this.state.activeRoute}
                       recalculateRoute={this.calculateRoute}
                       chooseRoute={this.chooseRoute}
            />
          </Grid>
          <Grid item sm={8} xs={12}>
            <Map waypoints={this.state.routeFindResultWaypoints} mapConnections={this.state.mapConnections}  />
          </Grid>
        </Grid>

      </div>
    );
  }

  constructor(props: Props) {
    super(props);
    this.t = props.t;

    this.state = {
      routeFrom: '',
      routeTo: '',
      startSystemInput: '',
      endSystemInput: '',
      buttonRouteFindDisabled: true,
      buttonRouteSetDisabled: true,
      routeFindResultMessage: '',
      numberOfRoutes: 0,
      activeRoute: 0,
      routeFindResultWaypoints: [],
      mapConnections: null,
      routeSetResult: '',
    };

    this.routeFind = this.routeFind.bind(this);
    this.routeSet = this.routeSet.bind(this);
    this.chooseRoute = this.chooseRoute.bind(this);
  }

  startChanged = (value: string) => {
    this.setState({routeFrom: value});
    this.setState({startSystemInput: value});
    this.setState({buttonRouteFindDisabled: !(value !== '' && this.state.routeTo !== '')});
  };

  endChanged = (value: string) => {
    this.setState({routeTo: value});
    this.setState({endSystemInput: value});
    this.setState({buttonRouteFindDisabled: !(this.state.routeFrom !== '' && value !== '')});
  };

  swapSystems = () => {
    this.routeFind(this.state.routeTo, this.state.routeFrom); /// reload route with already swapped systems
    this.setState({startSystemInput: this.state.routeTo});
    this.setState({endSystemInput: this.state.routeFrom});
  };

  calculateRoute = () => {
    if (! this.state.buttonRouteFindDisabled) {
      this.routeFind();
    }
  };

  routeFind(from?: String, to?: String) {
    const app = this;
    app.setState({buttonRouteFindDisabled: true});
    app.setState({buttonRouteSetDisabled: true});
    app.setState({routeFindResultMessage: ''});
    app.setState({numberOfRoutes: 0});
    app.setState({activeRoute: 0});
    app.setState({routeFindResultWaypoints: []});
    app.setState({routeSetResult: ''});
    from = from || this.state.routeFrom;
    to = to || this.state.routeTo;
    const url = `${this.context.domain}/api/route/find/${from}/${to}`;
    axios.get<ResponseRouteFind>(url).then(response => {
      if (response.data.routes.length === 0) {
        if (response.data.code) {
          app.setState({routeFindResultMessage: app.t(`responseCode.${response.data.code}`)});
        } else {
          app.setState({routeFindResultMessage: app.t('home.no-route-found')});
        }
      } else {
        app.routes = response.data.routes;
        app.setState({numberOfRoutes: response.data.routes.length});
        app.chooseRoute(0);
      }
    }).catch(() => {
      app.setState({routeFindResultMessage: app.t('app.error')});
    }).then(() => {
      this.setState({buttonRouteFindDisabled: false});
    });
  }

  routeSet() {
    const app = this;
    app.setState({buttonRouteSetDisabled: true});
    app.setState({routeSetResult: ''});
    axios.post<ResponseMessage>(
      `${this.context.domain}/api/route/set`,
      JSON.stringify(this.esiRoute),
      {headers: {'Content-Type': 'application/json'}},
    ).then(response => {
        app.setState({routeSetResult: app.t(`responseCode.${response.data.code}`, {message: response.data.param})});
    }).catch(() => {
      app.setState({routeSetResult: app.t('app.error')});
    }).then(() => {
      app.setState({buttonRouteSetDisabled: false});
    });
  }

  chooseRoute = (number: number) => {
    if (this.routes.length - 1 < number) {
      return
    }
    this.esiRoute = this.routes[number];
    this.setState({activeRoute: number});
    this.setState({routeFindResultWaypoints: this.esiRoute});
    this.setState({buttonRouteSetDisabled: this.esiRoute.length <= 1});
  };
}

export default withTranslation()(withStyles(styles)(Home));
