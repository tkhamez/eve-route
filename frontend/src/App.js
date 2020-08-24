import axios from 'axios';
import React from 'react';
import { withTranslation } from 'react-i18next';
import './App.css';
import { GlobalDataContext } from './GlobalDataContext.js';
import LanguageSwitcher from './components/LanguageSwitcher.js';
import Login from './pages/Login.js';
import Home from './pages/Home.js';

class App extends React.Component {
  render() {
    return (
      <GlobalDataContext.Provider value={this.globalData}>
        <LanguageSwitcher />
        <h1>EVE Route</h1>

        { this.state.isLoggedIn === false && <Login /> }
        { this.state.isLoggedIn           && <Home /> }

      </GlobalDataContext.Provider>
    );
  }

  constructor(props) {
    super(props);

    this.globalData = {
      domain: '',
      user: null,
    };

    this.state = {
      isLoggedIn: null,
    };

    if (window.location.port === '3000') {
      this.globalData.domain = 'http://localhost:8080'; // backend dev port
    }

    axios.defaults.withCredentials = true;
  }

  componentDidMount() {
    const app = this;
    axios.get(this.globalData.domain+'/api/auth/user').then(response => {
      app.globalData.user = {
        name: response.data.characterName,
        alliance: response.data.allianceId || '(unknown alliance)',
      };
      app.setState({ isLoggedIn: true }); // change state *after* the user data is set
    }).catch(() => { // 403
      app.setState({ isLoggedIn: false });
    });
  }
}

export default withTranslation()(App);
