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
        <Button aria-controls="language-menu" aria-haspopup="true" onClick={handleClick}
                style={{paddingLeft: 0, paddingRight: 0}}>
          <TranslateIcon />
          <ExpandMoreIcon />
        </Button>
      </Tooltip>
      <Menu id="language-menu" anchorEl={anchorEl} keepMounted open={open} onClose={handleClose}>
        <MenuItem onClick={handleClose} data-language='en-GB' selected={i18n.language === 'en-GB'} title="en-GB">
          {t('header.en-GB')}
        </MenuItem>
        <MenuItem onClick={handleClose} data-language='ru-RU' selected={i18n.language === 'ru-RU'} title="ru-RU">
          {t('header.ru-RU')}
        </MenuItem>
        <MenuItem onClick={handleClose} data-language='zh-CN' selected={i18n.language === 'zh-CN'} title="zh-CN">
          {t('header.zh-CN')}
        </MenuItem>
      </Menu>
    </div>
  );
}
