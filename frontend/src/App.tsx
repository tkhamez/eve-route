import React from 'react';
import { WithTranslation, withTranslation } from 'react-i18next';
import { TFunction } from "i18next";
import { createStyles, Theme, Container } from '@material-ui/core';
import { withStyles } from '@material-ui/styles';
import axios from 'axios';
import { GlobalDataContext, emptyUser, userType } from './GlobalDataContext';
import Header from './components/Header';
import Footer from './components/Footer';
import Login from './pages/Login';
import Home from './pages/Home';
import Admin from './pages/Admin';
import './App.css';
import { ResponseAuthUser, ResponseMapConnections } from "./response";

const styles = (theme: Theme) => createStyles({
  root: {
    display: 'flex',
    flexDirection: 'column',
    minHeight: '100vh',
    backgroundColor: theme.palette.grey[900],
  },
  footer: {
    padding: theme.spacing(0.5, 0),
    marginTop: 'auto',
    backgroundColor: theme.palette.background.default,
  },
});

interface Props extends WithTranslation {
  t: TFunction,
  classes: any
}

type AppState = {
  loaded: boolean,
  user: userType,
  page: string,
  mapConnections: any, // ResponseMapConnections|null
}

class App extends React.Component<Props, AppState> {
  t: Function;
  readonly domain: string;

  render() {
    const { classes } = this.props;

    const globalData = {
      domain: this.domain,
      user: this.state.user,
      mapConnections: this.state.mapConnections,
      logoutUser: this.logout
    };

    return (
      <GlobalDataContext.Provider value={globalData}>
        <div className={classes.root}>
          <Header connectionChanged={this.connectionChanged}/>

          <Container component="main" maxWidth="lg">
            { this.state.loaded &&
              <div className='grid-spacing-2-wrapper'>
                { this.state.user.name === '' && <Login /> }
                { this.state.user.name && this.state.page === 'Home' && <Home /> }
                { this.state.user.name && this.state.page === 'Admin' && <Admin /> }
              </div>
            }
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
    this.t = props.t;

    this.state = {
      loaded: false,
      user: emptyUser,
      page: getPage(),
      mapConnections: null,
    };

    this.domain = '';
    if (window.location.port === '3000') { // frontend dev port
      this.domain = `http://${window.location.hostname}:8080`; // backend dev port
    }

    this.logout = this.logout.bind(this);
    this.fetchUser = this.fetchUser.bind(this);
    this.logoutUser = this.logoutUser.bind(this);
  }

  componentDidMount() {
    window.addEventListener('hashchange', () => setPage(this));
    setupAxios(this);
    setInterval(this.fetchUser, 1000 * 60 * 10); // every 10 minutes
    this.fetchUser();
    loadConnections(this);
  }

  connectionChanged = () => {
    loadConnections(this)
  };

  logout() {
    axios.get(`${this.domain}/api/auth/logout`).then(() => {
      this.logoutUser();
    }).catch(() => {})
  }

  fetchUser() {
    axios.get<ResponseAuthUser>(this.domain+'/api/auth/user').then(response => {
      axios.defaults.headers.post[response.data.csrfHeaderKey] = response.data.csrfToken;
      axios.defaults.headers.put[response.data.csrfHeaderKey] = response.data.csrfToken;
      axios.defaults.headers.delete[response.data.csrfHeaderKey] = response.data.csrfToken;
      this.setState({
        loaded: true,
        user: response.data,
      });
    }).catch((e) => {
      if (e.response && e.response.status === 401) {
        this.logoutUser();
      }
    });
  }

  logoutUser() {
    this.setState({
      loaded: true,
      user: emptyUser
    });
  }
}

export default withTranslation()(withStyles(styles)(App));

const setupAxios = (app: App) => {
  axios.defaults.withCredentials = true;
  axios.interceptors.response.use((response) => {
    return response;
  }, (error) => {
    if (error.response && error.response.status === 401) {
      app.logoutUser();
    }
    return Promise.reject(error);
  });
};

/**
 * Load Ansiblex and temporary connections.
 */
const loadConnections = (app: App) => {
  axios.get<ResponseMapConnections>(`${app.domain}/api/route/map-connections`).then(r => {
    if (r.data.code) { // error
      console.log(app.t(`responseCode.${r.data.code}`));
    }
    app.setState({mapConnections: r.data});
  }).catch(() => {
    app.setState({mapConnections: {ansiblexes: [], temporary: [], code: null}});
  });
};

const getPage = (): string => {
  const validPages = ['Home', 'Admin'];
  const page = window.location.hash.substring(1);
  if (validPages.indexOf(page) !== -1) {
    return page;
  } else {
    return 'Home';
  }
};

const setPage = (app: App) => {
  app.setState({
    page: getPage(),
  });
};
