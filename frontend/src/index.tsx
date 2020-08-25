import React, { Suspense } from 'react';
import ReactDOM from 'react-dom';
import "fontsource-roboto/300-normal.css"
import "fontsource-roboto/400-normal.css"
import "fontsource-roboto/500-normal.css"
import "fontsource-roboto/700-normal.css"
import CssBaseline from '@material-ui/core/CssBaseline';
import { ThemeProvider } from '@material-ui/core/styles';
import './i18n';
import theme from './theme';
import './index.css';
import App from './App';
//import * as serviceWorker from './serviceWorker';

ReactDOM.render(
  <React.StrictMode>
    <Suspense fallback="loading">
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <App />
      </ThemeProvider>
    </Suspense>
  </React.StrictMode>,
  document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
//serviceWorker.unregister();
