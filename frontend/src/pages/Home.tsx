import React from 'react';
import { WithTranslation, withTranslation } from 'react-i18next';
import { TFunction } from 'i18next';
import { Box, Button, createStyles, Grid, IconButton, Typography } from '@material-ui/core';
import { withStyles } from '@material-ui/styles';
import SwapHorizIcon from '@material-ui/icons/SwapHoriz';
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseMapConnections, ResponseMessage, ResponseRouteFind, Waypoint } from '../response';
import SystemInput from '../components/SystemInput';
import RouteList from '../components/RouteList';
import NavModal from '../components/NavModal';
import Map from '../components/Map';

const styles = () => createStyles({

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
  routeFindResultWaypoints: Array<Waypoint>,
  mapConnections: ResponseMapConnections|null,
  routeSetResult: string,
}

class Home extends React.Component<Props, HomeState> {
  t: Function;
  private esiRoute: Array<object> = [];

  static contextType = GlobalDataContext;

  render() {
    const { t } = this.props;

    return (
      <div className='grid-spacing-2-wrapper'>

        <NavModal connectionChanged={this.connectionChanged} />

        <Grid container spacing={2} className='card'>
          <Grid item xs={12}>
            <Typography variant="h6" align="center">{t('home.select-systems')}</Typography>
          </Grid>

          <Grid item xs={5}>
            <Box display="flex" justifyContent="center">
              <SystemInput fieldId="start-system" fieldName={t('home.start-system')} onChange={this.startChanged}
                           findRoute={this.calculateRoute} fieldValue={this.state.startSystemInput} />
            </Box>
          </Grid>
          <Grid item xs={2}>
            <Box display="flex" justifyContent="center">
              <IconButton color="primary" onClick={this.swapSystems} disabled={this.state.buttonRouteFindDisabled}>
                <SwapHorizIcon />
              </IconButton>
            </Box>
          </Grid>
          <Grid item xs={5}>
            <Box display="flex" justifyContent="center">
              <SystemInput fieldId="end-system" fieldName={t('home.end-system')} onChange={this.endChanged}
                           findRoute={this.calculateRoute} fieldValue={this.state.endSystemInput} />
            </Box>
          </Grid>

          <Grid item xs={6}>
            <Box display="flex" justifyContent="flex-end">
              <Button variant="contained" color="primary" onClick={this.routeFind}
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
          <Grid item xs={12}>
            <Typography variant="body2" align="center">{t('home.check-in-game')}</Typography>
          </Grid>

          <Grid item sm={4} xs={12}>
            <RouteList waypoints={this.state.routeFindResultWaypoints}
                       message={this.state.routeFindResultMessage}
                       recalculateRoute={this.calculateRoute}
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
      routeFindResultWaypoints: [],
      mapConnections: null,
      routeSetResult: '',
    };

    this.calculateRoute = this.calculateRoute.bind(this);
    this.routeFind = this.routeFind.bind(this);
    this.routeSet = this.routeSet.bind(this);
    this.swapSystems = this.swapSystems.bind(this);
  }

  componentDidMount() {
    loadConnections(this);
  }

  connectionChanged = () => {
    loadConnections(this)
  };

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

  calculateRoute() {
    if (! this.state.buttonRouteFindDisabled) {
      this.routeFind();
    }
  };

  routeFind() {
    const app = this;
    app.setState({buttonRouteFindDisabled: true});
    app.setState({buttonRouteSetDisabled: true});
    app.setState({ routeFindResultMessage: '' });
    app.setState({ routeFindResultWaypoints: [] });
    app.setState({ routeSetResult: '' });
    const url = `${this.context.domain}/api/route/find/${this.state.routeFrom}/${this.state.routeTo}`;
    axios.get<ResponseRouteFind>(url).then(response => {
      if (response.data.route.length === 0) {
        if (response.data.code) {
          app.setState({ routeFindResultMessage: app.t(`responseCode.${response.data.code}`) });
        } else {
          app.setState({ routeFindResultMessage: app.t('home.no-route-found') });
        }
      } else {
        app.esiRoute = response.data.route;
        app.setState({ routeFindResultWaypoints: response.data.route });
        app.setState({buttonRouteSetDisabled: app.esiRoute.length <= 1});
      }
    }).catch(() => {
      app.setState({ routeFindResultMessage: app.t('app.error') });
    }).then(() => {
      this.setState({buttonRouteFindDisabled: false});
    });
  }

  routeSet() {
    const app = this;
    app.setState({buttonRouteSetDisabled: true});
    app.setState({ routeSetResult: '' });
    axios.post<ResponseMessage>(
      `${this.context.domain}/api/route/set`,
      JSON.stringify(this.esiRoute),
      { headers: { 'Content-Type': 'application/json' } },
    ).then(response => {
        app.setState({ routeSetResult: app.t(`responseCode.${response.data.code}`, {message: response.data.param}) });
    }).catch(() => {
      app.setState({ routeSetResult: app.t('app.error') });
    }).then(() => {
      app.setState({buttonRouteSetDisabled: false});
    });
  }

  swapSystems() {
    this.setState({startSystemInput: this.state.routeTo});
    this.setState({endSystemInput: this.state.routeFrom});
  }
}

export default withTranslation()(withStyles(styles)(Home));

/**
 * Load Ansiblex and temporary connections.
 */
const loadConnections = (app: Home) => {
  axios.get<ResponseMapConnections>(`${app.context.domain}/api/route/map-connections`).then(r => {
    if (r.data.code) { // error
      console.log(app.t(`responseCode.${r.data.code}`));
    }
    app.setState({mapConnections: r.data});
  }).catch(() => {
    app.setState({mapConnections: { ansiblexes: [], temporary: [], code: null }});
  });
};
