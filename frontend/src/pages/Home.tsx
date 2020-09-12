import React from 'react';
import { WithTranslation, withTranslation } from 'react-i18next';
import { TFunction } from 'i18next';
import {
  Box,
  Button,
  createStyles,
  Grid,
  Typography
} from '@material-ui/core';
import { withStyles } from '@material-ui/styles';
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseMessage, ResponseRouteFind, Waypoint } from '../response';
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
  buttonRouteFindDisabled: boolean,
  buttonRouteSetDisabled: boolean,
  routeFindResultMessage: string,
  routeFindResultWaypoints: Array<Waypoint>,
  routeSetResult: string,
}

class Home extends React.Component<Props, HomeState> {
  private t: Function;
  private esiRoute: Array<object> = [];

  static contextType = GlobalDataContext;

  render() {
    const { t } = this.props;

    return (
      <div className='grid-spacing-2-wrapper'>

        <NavModal />

        <Grid container spacing={2} className='card'>
          <Grid item xs={12}>
            <Typography variant="h6" align="center">{t('home.select-systems')}</Typography>
          </Grid>

          <Grid item xs={6}>
            <Box display="flex" justifyContent="center">
              <SystemInput fieldId="start-system" fieldName={t('home.start-system')} onChange={this.startChanged} />
            </Box>
          </Grid>
          <Grid item xs={6}>
            <Box display="flex" justifyContent="center">
              <SystemInput fieldId="start-end" fieldName={t('home.end-system')} onChange={this.endChanged} />
            </Box>
          </Grid>

          <Grid item xs={6}>
            <Box display="flex" justifyContent="right">
              <Button variant="contained" color="primary" onClick={this.routeFind}
                      disabled={this.state.buttonRouteFindDisabled}>
                {t('home.find-route')}
              </Button>
            </Box>
          </Grid>
          <Grid item xs={6}>
            <Box display="flex" justifyContent="left">
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
                       recalculateRoute={this.recalculateRoute}
            />
          </Grid>
          <Grid item sm={8} xs={12}>
            <Map waypoints={this.state.routeFindResultWaypoints} />
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
      buttonRouteFindDisabled: true,
      buttonRouteSetDisabled: true,
      routeFindResultMessage: '',
      routeFindResultWaypoints: [],
      routeSetResult: '',
    };

    this.recalculateRoute = this.recalculateRoute.bind(this);
    this.routeFind = this.routeFind.bind(this);
    this.routeSet = this.routeSet.bind(this);
  }

  startChanged = (value: string) => {
    this.setState({routeFrom: value});
    this.setState({buttonRouteFindDisabled: !(value !== '' && this.state.routeTo !== '')});
  };

  endChanged = (value: string) => {
    this.setState({routeTo: value});
    this.setState({buttonRouteFindDisabled: !(this.state.routeFrom !== '' && value !== '')});
  };

  recalculateRoute() {
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
}

export default withTranslation()(withStyles(styles)(Home));
