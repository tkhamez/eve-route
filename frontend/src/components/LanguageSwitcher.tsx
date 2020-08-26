import React from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Menu, MenuItem, Tooltip } from "@material-ui/core";
import TranslateIcon from '@material-ui/icons/Translate';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';

export default function LanguageSwitcher() {
  const { t, i18n } = useTranslation();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = (event: React.MouseEvent<HTMLLIElement, MouseEvent>) => {
    // noinspection JSIgnoredPromiseFromCall
    i18n.changeLanguage(event.currentTarget.dataset.language || 'en-GB');
    setAnchorEl(null);
  };

  return (
    <div>
      <Tooltip title={t('header.change-language').toString()}>
        <Button aria-controls="language-menu" aria-haspopup="true" onClick={handleClick}>
          <TranslateIcon /> &nbsp;
          {t('header.'+i18n.language)}
          <ExpandMoreIcon />
        </Button>
      </Tooltip>
      <Menu id="language-menu" anchorEl={anchorEl} keepMounted open={open} onClose={handleClose}>
        <MenuItem onClick={handleClose} data-language='en-GB'>{t('header.en-GB')}</MenuItem>
        <MenuItem onClick={handleClose} data-language='zh-CN'>{t('header.zh-CN')}</MenuItem>
      </Menu>
    </div>
  );
}
