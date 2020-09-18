import React from 'react';
import { render } from '@testing-library/react';
import { ThemeProvider } from "@material-ui/core/styles";
import theme from "./theme";
import App from './App';

test('renders title', () => {
  const { getByText } = render(<ThemeProvider theme={theme}><App /></ThemeProvider>);
  const titleElement = getByText(/app.name/i);
  expect(titleElement).toBeInTheDocument();
});
