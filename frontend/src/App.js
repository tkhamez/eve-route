import axios from 'axios';
import React from 'react';
import { withTranslation } from 'react-i18next';
import './App.css';
import Login from './pages/Login.js';
import Home from './pages/Home.js';

class App extends React.Component {
  render() {
    const { t } = this.props;

    const changeLanguage = (lng) => {
      this.props.i18n.changeLanguage(lng).then();
    };

    return (
      <div>
        <div>
          <button type="button" onClick={() => changeLanguage('en-GB')} title="en-GB">{t('head.en')}</button>
          <button type="button" onClick={() => changeLanguage('zh-HK')} title="zh-HK">{t('head.zh')}</button>
        </div>

        <h1>EVE Route</h1>

        {! this.state.isLoggedIn > 0 &&
          <Login domain={this.domain} t={t} />
        }
        {this.state.isLoggedIn > 0 &&
          <Home domain={this.domain} t={t} user={this.state.user} />
        }
      </div>
    );
  }

  constructor(props) {
    super(props);

    this.state = {
      isLoggedIn: false,
      user: "",
    };

    this.domain = '';
    if (window.location.port === '3000') {
      this.domain = 'http://localhost:8080'; // backend dev port
    }

    axios.defaults.withCredentials = true;
  }

  componentDidMount() {
    const app = this;
    axios.get(this.domain+'/api/auth/user').then(response => {
      app.setState({
        isLoggedIn: true,
        user: response.data.characterName + ' ' + (response.data.allianceId || '(unknown alliance)')
      });
    }).catch(() => { // 403
      app.setState({ isLoggedIn: false });
    });
  }
}

export default withTranslation('translations')(App);
