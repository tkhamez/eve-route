import { createMuiTheme } from "@material-ui/core";

const theme = createMuiTheme({
  palette: {
    type: 'dark',
    primary: {
      main: '#90caf9'
    }
  },
  typography: {
    fontFamily: [
      'Saira',
      'Roboto',
      'Helvetica',
      'Arial',
      'sans-serif',
    ].join(','),
  },
});

export default theme;
