import React from 'react';
import { WithTranslation, withTranslation } from 'react-i18next';
import { TFunction } from 'i18next';
import {
  Box,
  Button,
  createStyles,
  Grid,
  Link,
  Theme,
  Typography
} from '@material-ui/core';
import { withStyles } from '@material-ui/styles';
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseGates, ResponseGatesUpdated, ResponseRouteFind, ResponseRouteSet, Waypoint } from '../response';
import SystemInput from '../components/SystemInput';
import RouteList from '../components/RouteList';

const styles = (theme: Theme) => createStyles({
  card: {
    backgroundColor: theme.palette.background.default,
    margin: theme.spacing(2, 0),
    borderRadius: '4px',
  },
});

interface Props extends WithTranslation {
  t: TFunction,
  classes: any,
}

type HomeState = {
  gatesUpdated: Date|null,
  gatesResult: Array<string>,
  routeFrom: string,
  routeTo: string,
  buttonRouteFindDisabled: boolean,
  buttonRouteSetDisabled: boolean,
  routeFindResultMessage: string,
  routeFindResultWaypoints: Array<Waypoint>,
  dotlanHref: string,
  routeSetResult: string,
}

class Home extends React.Component<Props, HomeState> {
  private t: Function;
  private esiRoute: Array<object> = [];

  static contextType = GlobalDataContext;

  render() {
    const { t } = this.props;
    const { classes } = this.props;

    return (
      <div style={{ width: 'calc(100% - 16px)' }}> {
        /* Grid with spacing>0 is bigger than the parent, this fixes that for spacing=2,
         see also https://github.com/mui-org/material-ui/issues/7466 */}

        <p>
          <button onClick={this.gatesFetch}>{t('home.show-gates')}</button>
          <button onClick={this.gatesUpdate}>{t('home.update-gates')}</button>
          {t('home.last-update')}: {this.state.gatesUpdated}<br/>
          {this.state.gatesResult.map((value, index) => {
            return <span key={index}>{value}<br/></span>
          })}
        </p>

        <Grid container spacing={2} className={classes.card}>
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

          <Grid item sm={4} xs={12}>
            {this.state.routeFindResultWaypoints.length > 0 &&
              <RouteList waypoints={this.state.routeFindResultWaypoints} />
            }
          </Grid>
          <Grid item sm={8} xs={12}>
            {this.state.routeFindResultMessage}
            {this.state.dotlanHref &&
            <span>
              <br />
              <Link href={this.state.dotlanHref} target="_blank" rel="noopener noreferrer">{t('home.dotlan')}</Link>
              <br />
            </span>
            }
          </Grid>
        </Grid>

      </div>
    );
  }

  constructor(props: Props) {
    super(props);
    this.t = props.t;

    this.state = {
      gatesUpdated: null,
      gatesResult: [],
      routeFrom: '',
      routeTo: '',
      buttonRouteFindDisabled: true,
      buttonRouteSetDisabled: true,
      routeFindResultMessage: '',
      routeFindResultWaypoints: [],
      dotlanHref: '',
      routeSetResult: '',
    };

    this.gatesFetch = this.gatesFetch.bind(this);
    this.gatesUpdate = this.gatesUpdate.bind(this);
    this.routeFind = this.routeFind.bind(this);
    this.routeSet = this.routeSet.bind(this);
  }

  componentDidMount() {
    this.fetchGatesUpdated();
  }

  fetchGatesUpdated() {
    const app = this;
    axios.get<ResponseGatesUpdated>(`${this.context.domain}/api/gates/last-update`).then(response => {
      if (response.data) {
        app.setState({ gatesUpdated: response.data.updated });
      }
    }).catch(() => { // 403
      // do nothing (necessary or react dev tools will complain)
    });
  }

  gatesFetch(event: React.MouseEvent<HTMLButtonElement>) {
    const app = this;
    const button = event.currentTarget;
    button.disabled = true;
    app.setState({ gatesResult: [] });
    axios.get<ResponseGates>(`${this.context.domain}/api/gates/fetch`).then(response => {
      let gates = [];
      for (let i = 0; i < response.data.ansiblexes.length; i++) {
        gates.push(response.data.ansiblexes[i].name);
      }
      app.setState({ gatesResult: gates });
    }).catch(() => {
      app.setState({ gatesResult: [app.t('home.error')] });
    }).then(() => {
      button.disabled = false;
    });
  }

  gatesUpdate(event: React.MouseEvent<HTMLButtonElement>) {
    const app = this;
    const button = event.currentTarget;
    button.disabled = true;
    app.setState({ gatesResult: [] });
    axios.get<ResponseGates>(`${this.context.domain}/api/gates/update`).then(response => {
      if (response.data.message) { // some error
        app.setState({ gatesResult: [response.data.message] });
        return;
      }
      let gates = [];
      for (let i = 0; i < response.data.ansiblexes.length; i++) {
        gates.push(response.data.ansiblexes[i].name);
      }
      app.setState({ gatesResult: gates });
      app.fetchGatesUpdated();
    }).catch(() => {
      app.setState({ gatesResult: [app.t('home.error')] });
    }).then(() => {
      button.disabled = false;
    });
  }

  startChanged = (value: string) => {
    this.setState({routeFrom: value});
    this.setState({buttonRouteFindDisabled: !(value !== '' && this.state.routeTo !== '')});
  };

  endChanged = (value: string) => {
    this.setState({routeTo: value});
    this.setState({buttonRouteFindDisabled: !(this.state.routeFrom !== '' && value !== '')});
  };

  async routeFind() {
    const app = this;
    app.setState({buttonRouteFindDisabled: true});
    app.setState({buttonRouteSetDisabled: true});
    app.setState({ routeFindResultMessage: '' });
    app.setState({ routeFindResultWaypoints: [] });
    app.setState({ dotlanHref: '' });
    app.setState({ routeSetResult: '' });
    const url = `${this.context.domain}/api/route/find/${this.state.routeFrom}/${this.state.routeTo}`;
    axios.get<ResponseRouteFind>(url).then(response => {
      if (response.data.route.length === 0) {
        app.setState({ routeFindResultMessage: app.t('home.no-route-found') });
      } else {
        app.esiRoute = [];
        let dotlanHref = 'https://evemaps.dotlan.net/route/';
        for (let i = 0; i < response.data.route.length; i++) {
          app.esiRoute.push({
            systemId: response.data.route[i].systemId,
            systemName: response.data.route[i].systemName, // for debugging
            ansiblexId: response.data.route[i].ansiblexId || null,
          });
          dotlanHref += response.data.route[i].systemName.replace(' ', '_');
          if (response.data.route[i].connectionType === 'Stargate') {
            dotlanHref += ':';
          } else if (response.data.route[i].connectionType === 'Ansiblex') {
            dotlanHref += '::';
          } // else = end system
        }
        app.setState({ routeFindResultWaypoints: response.data.route });
        app.setState({ dotlanHref: dotlanHref });
        app.setState({buttonRouteSetDisabled: app.esiRoute.length <= 1});
      }
    })
    .catch(() => {
      app.setState({ routeFindResultMessage: app.t('home.error') });
    })
    .then(() => {
      this.setState({buttonRouteFindDisabled: false});
    });
  }

  routeSet() {
    const app = this;
    app.setState({buttonRouteSetDisabled: true});
    app.setState({ routeSetResult: '' });
    axios.post<ResponseRouteSet>(
      `${this.context.domain}/api/route/set`,
      JSON.stringify(this.esiRoute),
      { headers: { 'Content-Type': 'application/json' } },
    ).then(response => {
        app.setState({ routeSetResult: response.data.message });
      }).catch(() => {
        app.setState({ routeSetResult: app.t('home.error') });
      }).then(() => {
        app.setState({buttonRouteSetDisabled: false});
      });
  }
}

export default withTranslation()(withStyles(styles)(Home));
