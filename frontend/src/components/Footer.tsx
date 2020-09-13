import React from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { Container, Link, Typography } from '@material-ui/core';
import GitHubIcon from '@material-ui/icons/GitHub';

export default function Footer() {
  const { t } = useTranslation();
  return (
      <Container maxWidth="md">
        <Typography variant="body2" color="textSecondary" align="center">
          Created by <Link color="textPrimary" href="https://evewho.com/character/96061222" target="_blank" rel="noopener noreferrer">Tian Khamez</Link>
          {' '}|{' '}
          <strong>Ƶ</strong> {t('footer.donate')}
          {' '}|{' '}
          <Link color="textPrimary" href="https://github.com/tkhamez/eve-route" target="_blank" rel="noopener noreferrer">
            <GitHubIcon fontSize="inherit" style={{position: 'relative', top: '2px'}}/> GitHub
          </Link>
          {' '}|{' '}
          <Link color="textPrimary" href="https://discord.gg/EjzHx8p" target="_blank" rel="noopener noreferrer">
            <img src="/discord.svg" width="18px" alt="Discord" style={{position: 'relative', top: '5px'}} /> Discord
          </Link>
        </Typography>
        <Typography variant="body2" color="textSecondary" align="center">
          <small>
            <Trans i18nKey="footer.ccp">
              %<Link color="textPrimary" href="https://www.ccpgames.com" target="_blank" rel="noopener noreferrer">%</Link>%
            </Trans>
          </small>
        </Typography>
      </Container>
  );
}
