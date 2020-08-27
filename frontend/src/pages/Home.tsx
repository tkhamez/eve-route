import React from 'react';
import { WithTranslation, withTranslation } from 'react-i18next';
import { TFunction } from 'i18next';
import { Box, Grid, Link } from '@material-ui/core';
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseGates, ResponseGatesUpdated, ResponseRouteCalculate, ResponseRouteSet } from '../response';
import SystemInput from '../components/SystemInput';

interface Props extends WithTranslation {
  t: TFunction,
}

type HomeState = {
  gatesUpdated: Date|null,
  gatesResult: Array<string>,
  routeFrom: string,
  routeTo: string,
  routeCalculateResult: Array<string>,
  dotlanHref: string,
  routeSetResult: string,
}

class Home extends React.Component<Props, HomeState> {
  private t: Function;
  private esiRoute: Array<object> = [];

  static contextType = GlobalDataContext;

  render() {
    const { t } = this.props;

    return (
      <div>

        <Grid container spacing={2}>
          <Grid item xs={6}>
            <Box display="flex" justifyContent="center">
              <SystemInput fieldId="start-system" fieldName={t('home.start-system')} onChange={this.startChanged}/>
            </Box>
          </Grid>
          <Grid item xs={6}>
            <Box display="flex" justifyContent="center">
              <SystemInput fieldId="start-end" fieldName={t('home.end-system')} onChange={this.endChanged} />
            </Box>
          </Grid>
        </Grid>

        <p id="route">
          <button onClick={this.routeCalculate}>{t('home.calculate')}</button>
          <button onClick={this.routeSet}>{t('home.set-route')}</button>
          {this.state.routeSetResult}<br/>

          {this.state.dotlanHref &&
            <span>
              <Link href={this.state.dotlanHref} target="_blank" rel="noopener noreferrer">{t('home.dotlan')}</Link>
              <br />
            </span>
          }

          {this.state.routeCalculateResult.map((value, index) => {
            return <span key={index}>{value}<br/></span>
          })}
        </p>

        <p id="gates">
          <button onClick={this.gatesFetch}>{t('home.show-gates')}</button>
          <button onClick={this.gatesUpdate}>{t('home.update-gates')}</button>
          {t('home.last-update')}: {this.state.gatesUpdated}<br/>
          {this.state.gatesResult.map((value, index) => {
            return <span key={index}>{value}<br/></span>
          })}
        </p>
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
      routeCalculateResult: [],
      dotlanHref: '',
      routeSetResult: '',
    };

    this.gatesUpdated = this.gatesUpdated.bind(this);
    this.gatesFetch = this.gatesFetch.bind(this);
    this.gatesUpdate = this.gatesUpdate.bind(this);
    this.routeCalculate = this.routeCalculate.bind(this);
    this.routeSet = this.routeSet.bind(this);
  }

  componentDidMount() {
    this.gatesUpdated();
  }

  gatesUpdated() {
    const app = this;
    axios.get<ResponseGatesUpdated>(this.context.domain+'/api/gates/last-update').then(response => {
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
    axios.get<ResponseGates>(this.context.domain+'/api/gates/fetch').then(response => {
      let gates = [];
      for (let i = 0; i < response.data.ansiblexes.length; i++) {
        gates.push(response.data.ansiblexes[i].name);
      }
      app.setState({ gatesResult: gates });
    }).catch(() => {
      app.setState({ gatesResult: [app.t('home.error')+'.'] });
    }).then(() => {
      button.disabled = false;
    });
  }

  gatesUpdate(event: React.MouseEvent<HTMLButtonElement>) {
    const app = this;
    const button = event.currentTarget;
    button.disabled = true;
    app.setState({ gatesResult: [] });
    axios.get<ResponseGates>(this.context.domain+'/api/gates/update').then(response => {
      if (response.data.message) { // some error
        app.setState({ gatesResult: [response.data.message] });
        return;
      }
      let gates = [];
      for (let i = 0; i < response.data.ansiblexes.length; i++) {
        gates.push(response.data.ansiblexes[i].name);
      }
      app.setState({ gatesResult: gates });
      app.gatesUpdated();
    }).catch(() => {
      app.setState({ gatesResult: [app.t('home.error')+'.'] });
    }).then(() => {
      button.disabled = false;
    });
  }

  startChanged = (value: string) => {
    this.setState({routeFrom: value});
  };

  endChanged = (value: string) => {
    this.setState({routeTo: value});
  };

  routeCalculate(event: React.MouseEvent<HTMLButtonElement>) {
    const app = this;
    const button = event.currentTarget;
    button.disabled = true;
    app.setState({ routeCalculateResult: [] });
    const url = `${this.context.domain}/api/route/calculate/${this.state.routeFrom}/${this.state.routeTo}`;
    axios.get<ResponseRouteCalculate>(url).then(response => {
      if (response.data.route.length === 0) {
        app.setState({ routeCalculateResult: [app.t('home.no-route-found')] });
      } else {
        app.esiRoute = [];
        let route = [];
        let dotlanHref = 'https://evemaps.dotlan.net/route/';
        for (let i = 0; i < response.data.route.length; i++) {
          app.esiRoute.push({
            systemId: response.data.route[i].systemId,
            systemName: response.data.route[i].systemName, // for debugging
            ansiblexId: response.data.route[i].ansiblexId || null,
          });
          let waypoint = response.data.route[i].systemName + ' ' + response.data.route[i].systemSecurity;
          if (response.data.route[i].ansiblexName) {
            waypoint += ' "' + response.data.route[i].ansiblexName + '"';
          }
          route.push(waypoint);
          dotlanHref += response.data.route[i].systemName.replace(' ', '_');
          if (response.data.route[i].connectionType === "Stargate") {
            dotlanHref += ':';
          } else if (response.data.route[i].connectionType === "Ansiblex") {
            dotlanHref += '::';
          } // else = end system
        }
        app.setState({ routeCalculateResult: route });
        app.setState({ dotlanHref: dotlanHref });
      }
    })
    .catch(() => {
      app.setState({ routeCalculateResult: [app.t('home.error')+'.'] });
    })
    .then(() => {
      button.disabled = false;
    });
  }

  routeSet(event: React.MouseEvent<HTMLButtonElement>) {
    const app = this;
    const button = event.currentTarget;
    button.disabled = true;
    app.setState({ routeSetResult: '' });
    axios.post<ResponseRouteSet>(
      this.context.domain+'/api/route/set',
      JSON.stringify(this.esiRoute),
      { headers: { 'Content-Type': 'application/json' } },
    ).then(response => {
        app.setState({ routeSetResult: response.data.message });
      }).catch(() => {
        app.setState({ routeSetResult: app.t('home.error')+'.' });
      }).then(() => {
        button.disabled = false;
      });
  }
}

export default withTranslation()(Home);
