import i18n from 'i18next';
import Backend from 'i18next-xhr-backend';
import {initReactI18next} from 'react-i18next';

// noinspection JSIgnoredPromiseFromCall
i18n
  .use(Backend)
  .use(initReactI18next)
  .init({
    //debug: true,
    fallbackLng: 'en-GB',
    lng: 'en-GB',
    load: 'currentOnly',
    backend: { loadPath: '/i18n/{{lng}}.json', },
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n;
