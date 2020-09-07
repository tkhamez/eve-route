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
  isLoggedIn: Boolean|null,
}

class App extends React.Component<Props, AppState> {
  private readonly globalData: any;

  render() {
    const { classes } = this.props;

    return (
      <GlobalDataContext.Provider value={this.globalData}>
        <div className={classes.root}>
          <Header />

          <Container component="main" maxWidth="md">
            { this.state.isLoggedIn === false && <Login /> }
            { this.state.isLoggedIn           && <Home /> }
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

    this.globalData = {};

    this.state = {
      isLoggedIn: null,
    };

    this.globalData.domain = '';
    if (window.location.port === '3000') { // frontend dev port
      this.globalData.domain = `http://${window.location.hostname}:8080`; // backend dev port
    }

    axios.defaults.withCredentials = true;
  }

  componentDidMount() {
    const app = this;
    axios.get<ResponseAuthUser>(this.globalData.domain+'/api/auth/user').then(response => {
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

export default withTranslation()(withStyles(styles)(App));
