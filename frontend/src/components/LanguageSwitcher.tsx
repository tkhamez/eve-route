import React from 'react';
import { useTranslation } from 'react-i18next';

function LanguageSwitcher() {
  const { t, i18n } = useTranslation();

  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng).then();
  };

  return (
    <div>
      <button type="button" onClick={() => changeLanguage('en-GB')} title="en-GB">{t('header.en')}</button>
      <button type="button" onClick={() => changeLanguage('zh-HK')} title="zh-HK">{t('header.zh')}</button>
    </div>
  );
}

export default LanguageSwitcher;
