import axios from 'axios';
import React from 'react';
import { withTranslation } from 'react-i18next';
import './App.css';
import LanguageSwitcher from './components/LanguageSwitcher.js';
import Login from './pages/Login.js';
import Home from './pages/Home.js';

class App extends React.Component {
  render() {
    return (
      <div>
        <LanguageSwitcher />
        <h1>EVE Route</h1>

        {! this.state.isLoggedIn > 0 &&
          <Login domain={this.domain} />
        }
        {this.state.isLoggedIn > 0 &&
          <Home domain={this.domain} user={this.state.user} />
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

export default withTranslation()(App);
