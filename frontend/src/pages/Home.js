import React from 'react';
import axios from "axios";
import { withTranslation } from 'react-i18next';
import { GlobalDataContext } from '../GlobalDataContext.js';

class Home extends React.Component {
  static contextType = GlobalDataContext;

  render() {
    const { t } = this.props;

    return (
      <div>
        <p>
          {t('home.hello')} {this.context.user.name} {this.context.user.alliance}<br/>
          <a href={this.context.domain + '/api/auth/logout'}>{t('home.logout')}</a>
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
    );
  }

  constructor(props) {
    super(props);
    this.t = props.t;

    this.state = {
      gatesUpdated: '',
      gatesResult: [],
      routeFrom: '',
      routeTo: '',
      routeCalculateResult: [],
      dotlanHref: '',
      routeSetResult: '',
    };
    this.esiRoute = [];

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
    axios.get(this.context.domain+'/api/gates/last-update').then(response => {
      if (response.data) {
        app.setState({ gatesUpdated: response.data.updated });
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
    axios.get(this.context.domain+'/api/gates/fetch').then(response => {
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

  gatesUpdate(event) {
    const app = this;
    const button = event.target;
    button.disabled = true;
    app.setState({ gatesResult: [] });
    axios.get(this.context.domain+'/api/gates/update').then(response => {
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
    axios.get(this.context.domain+'/api/route/calculate/' + this.state.routeFrom + '/' + this.state.routeTo)
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
    axios.post(this.context.domain+'/api/route/set', JSON.stringify(this.esiRoute)).then(response => {
      app.setState({ routeSetResult: response.data.message });
    }).catch(() => {
      app.setState({ routeSetResult: app.t('home.error')+'.' });
    }).then(() => {
      button.disabled = false;
    });
  }
}

export default withTranslation()(Home);
