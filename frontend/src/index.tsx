import React, { Suspense } from 'react';
import ReactDOM from 'react-dom';
import "fontsource-saira/300-normal.css"
import "fontsource-saira/400-normal.css"
import "fontsource-saira/500-normal.css"
import "fontsource-saira/700-normal.css"
import "fontsource-roboto/300-normal.css"
import "fontsource-roboto/400-normal.css"
import "fontsource-roboto/500-normal.css"
import "fontsource-roboto/700-normal.css"
import CssBaseline from '@material-ui/core/CssBaseline';
import { ThemeProvider } from '@material-ui/core/styles';
import './i18n';
import theme from './theme';
import App from './App';

ReactDOM.render(
  <React.StrictMode>
    <Suspense fallback="">
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <App />
      </ThemeProvider>
    </Suspense>
  </React.StrictMode>,
  document.getElementById('root')
);
