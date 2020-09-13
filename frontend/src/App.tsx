import React from 'react';
import { WithTranslation, withTranslation } from 'react-i18next';
import { createStyles, Theme, Container } from '@material-ui/core';
import { withStyles } from '@material-ui/styles';
import axios from 'axios';
import { GlobalDataContext } from './GlobalDataContext';
import Header from './components/Header';
import Footer from './components/Footer';
import Login from './pages/Login';
import Home from './pages/Home';
import './App.css';
import { ResponseAuthUser } from "./response";

const styles = (theme: Theme) => createStyles({
  root: {
    display: 'flex',
    flexDirection: 'column',
    minHeight: '100vh',
    backgroundColor: theme.palette.grey[900],
  },
  footer: {
    padding: theme.spacing(2, 0),
    marginTop: 'auto',
    backgroundColor: theme.palette.background.default,
  },
});

interface Props extends WithTranslation {
  classes: any
}

type AppState = {
  loaded: boolean,
  user: {
    name: string,
    allianceName: string,
    allianceTicker: string,
  },
}

class App extends React.Component<Props, AppState> {
  private readonly domain: string;

  render() {
    const { classes } = this.props;

    const globalData = {
      domain: this.domain,
      user: this.state.user,
      logoutUser: this.logout
    };

    return (
      <GlobalDataContext.Provider value={globalData}>
        <div className={classes.root}>
          <Header />

          <Container component="main" maxWidth="md">
            { this.state.loaded && this.state.user.name === '' && <Login /> }
            { this.state.loaded && this.state.user.name        && <Home /> }
          </Container>

          <footer className={classes.footer}>
            <Footer />
          </footer>
        </div>
      </GlobalDataContext.Provider>
    );
  }

  constructor(props: Props) {
    super(props);

    this.state = {
      loaded: false,
      user: {
        name: '',
        allianceName: '',
        allianceTicker: '',
      },
    };

    this.domain = '';
    if (window.location.port === '3000') { // frontend dev port
      this.domain = `http://${window.location.hostname}:8080`; // backend dev port
    }

    this.logout = this.logout.bind(this);
    this.fetchUser = this.fetchUser.bind(this);
    this.logoutUser = this.logoutUser.bind(this);

    axios.defaults.withCredentials = true;
  }

  componentDidMount() {
    setInterval(this.fetchUser, 1000 * 60 * 10); // every 10 minutes
    this.fetchUser();
  }

  logout() {
    axios.get(`${this.domain}/api/auth/logout`).then(() => {
      this.logoutUser();
    }).catch(() => {})
  }

  fetchUser() {
    axios.get<ResponseAuthUser>(this.domain+'/api/auth/user').then(response => {
      this.setState({
        loaded: true,
        user: response.data,
      });
    }).catch(() => { // 403
      this.logoutUser();
    });
  }

  logoutUser() {
    this.setState({
      loaded: true,
      user: {
        name: '',
        allianceName: '',
        allianceTicker: '',
      }
    });
  }
}

export default withTranslation()(withStyles(styles)(App));
