import React from 'react';
import { Trans } from 'react-i18next';
import { Container, Grid, Link, Typography } from '@material-ui/core';
import GitHubIcon from '@material-ui/icons/GitHub';

export default function Footer() {
  return (
      <Container maxWidth="md">
        <Grid container spacing={0}>
          <Grid item xs={6}>
            <Typography variant="body2" color="textSecondary" align="center">
              <GitHubIcon fontSize="inherit"/>{' '}
              <Trans i18nKey="footer.github">
                %<Link color="inherit" underline="always" target="_blank" rel="noopener noreferrer"
                       href="https://github.com/tkhamez/eve-route">%</Link>%
              </Trans>
            </Typography>
          </Grid>
          <Grid item xs={6}>
            <Typography variant="body2" color="textSecondary" align="center">
              <strong>Ƶ</strong>{' '}
              <Trans i18nKey="footer.donate">
                %<Link color="inherit" underline="always" target="_blank" rel="noopener noreferrer" title="Tian Khamez"
                       href="https://evewho.com/character/96061222">%</Link>%
              </Trans>
            </Typography>
          </Grid>
        </Grid>
        <Typography variant="body2" color="textSecondary" align="center">
          <small>
            <Trans i18nKey="footer.ccp">
              %<Link color="inherit" underline="always" target="_blank" rel="noopener noreferrer"
                     href="https://www.ccpgames.com">%</Link>%
            </Trans>
          </small>
        </Typography>
      </Container>
  );
}
