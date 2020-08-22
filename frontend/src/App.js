import axios from 'axios';
import React, { Suspense } from 'react';
import { withTranslation } from 'react-i18next';
import './App.css';

class EveRoute extends React.Component {
  render() {
    const { t } = this.props;

    return (
      <div className="App">
        <h1>EVE Route</h1>

        <div id="login" className={ this.state.isLoggedIn ? 'cloak' : '' }>
          <a href={this.domain+'/api/auth/login'}>
            <img src="/eve-sso-login-black-small.png" alt={t('login.login')}/>
          </a>
        </div>

        <div id="home" className={ this.state.isLoggedIn ? '' : 'cloak' }>
          <p>
            {t('home.hello')} {this.state.homeUser}<br/>
            <a href={this.domain+'/api/auth/logout'}>{t('home.logout')}</a>
          </p>
          <p id="route">
            <label>
              {t('home.from')}
              <input type="text" value={this.state.routeFrom} onChange={this.inputFromChange} />
            </label>
            <label>
              {t('home.to')}
              <input type="text" value={this.state.routeTo} onChange={this.inputToChange} />
            </label>
            <button onClick={this.routeCalculate}>{t('home.calculate')}</button>
            <button onClick={this.routeSet}>{t('home.set-route')}</button>
            {this.state.routeSetResult}<br/>
            <a href={this.state.dotlanHref} target="_blank" rel="noopener noreferrer">Dotlan</a><br/>
            {this.state.routeCalculateResult.map((value, index) => { return <span key={index}>{value}<br/></span> })}
          </p>
          <p id="gates">
            <button onClick={this.gatesFetch}>{t('home.show-gates')}</button>
            <button onClick={this.gatesUpdate}>{t('home.update-gates')}</button>
            {t('home.last-update')}: {this.state.gatesUpdated}<br/>
            {this.state.gatesResult.map((value, index) => { return <span key={index}>{value}<br/></span> })}
          </p>
        </div>

      </div>
    );
  }

  constructor(props) {
    super(props);
    this.t = props.t;

    this.state = {
      isLoggedIn: false,
      homeUser: '',
      gatesUpdated: '',
      gatesResult: [],
      routeFrom: '',
      routeTo: '',
      routeCalculateResult: [],
      dotlanHref: '',
      routeSetResult: '',
    };

    this.domain = '';
    if (window.location.port === '3000') {
      this.domain = 'http://localhost:8080'; // backend dev port
    }
    this.esiRoute = [];

    this.gatesUpdated = this.gatesUpdated.bind(this);
    this.gatesFetch = this.gatesFetch.bind(this);
    this.gatesUpdate = this.gatesUpdate.bind(this);
    this.routeCalculate = this.routeCalculate.bind(this);
    this.routeSet = this.routeSet.bind(this);

    axios.defaults.withCredentials = true;
  }

  componentDidMount() {
    const app = this;
    axios.get(this.domain+'/api/auth/user').then(response => {
      app.setState({
        isLoggedIn: true,
        homeUser: response.data.characterName + ' ' + (response.data.allianceId || '(unknown alliance)')
      })
    }).catch(() => { // 403
      app.setState({ isLoggedIn: false })
    });

    this.gatesUpdated();
  }

  gatesUpdated() {
    const app = this;
    axios.get(this.domain+'/api/gates/last-update').then(response => {
      if (response.data) {
        app.setState({ gatesUpdated: response.data.updated })
      }
    }).catch(() => { // 403
      // do nothing (necessary or react dev tools will complain)
    });
  }

  gatesFetch(event) {
    const app = this;
    const button = event.target;
    button.disabled = true;
    app.setState({ gatesResult: [] });
    axios.get(this.domain+'/api/gates/fetch').then(response => {
      let gates = [];
      for (let i = 0; i < response.data.ansiblexes.length; i++) {
        gates.push(response.data.ansiblexes[i].name);
      }
      app.setState({ gatesResult: gates });
    }).catch(() => {
      app.setState({ gatesResult: [app.t('home.error')+'.'] })
    }).then(() => {
      button.disabled = false;
    });
  }

  gatesUpdate(event) {
    const app = this;
    const button = event.target;
    button.disabled = true;
    app.setState({ gatesResult: [] });
    axios.get(this.domain+'/api/gates/update').then(response => {
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

  inputFromChange = (e) => {
    this.setState({routeFrom: e.target.value});
  };

  inputToChange = (e) => {
    this.setState({routeTo: e.target.value});
  };

  routeCalculate(event) {
    const app = this;
    const button = event.target;
    button.disabled = true;
    app.setState({ routeCalculateResult: [] });
    axios.get(this.domain+'/api/route/calculate/' + this.state.routeFrom + '/' + this.state.routeTo)
    .then(response => {
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

  routeSet(event) {
    const app = this;
    const button = event.target;
    button.disabled = true;
    app.setState({ routeSetResult: '' });
    axios.post(this.domain+'/api/route/set', JSON.stringify(this.esiRoute)).then(response => {
      app.setState({ routeSetResult: response.data.message });
    }).catch(() => {
      app.setState({ routeSetResult: app.t('home.error')+'.' });
    }).then(() => {
      button.disabled = false;
    });
  }
}

const EveRouteComponent = withTranslation()(EveRoute);

// i18n translations might still be loaded by the http backend
// use react's Suspense
export default function App() {
  return (
    <Suspense fallback="loading">
      <EveRouteComponent />
    </Suspense>
  );
};
