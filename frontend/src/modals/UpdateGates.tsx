import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseGatesUpdated, ResponseGates } from '../response';

export default function HowThisWorks() {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const [gatesUpdated, setGatesUpdated] = useState<Date|null>(null);
  const [gatesResult, setGatesResult] = useState<Array<string>>([]);

  useEffect(() => {
    if (gatesUpdated == null) {
      fetchGatesUpdated();
    }
  });

  const fetchGatesUpdated = () => {
    axios.get<ResponseGatesUpdated>(`${globalData.domain}/api/gates/last-update`).then(response => {
      if (response.data) {
        setGatesUpdated(response.data.updated);
      }
    }).catch(() => { // 403
      // do nothing (necessary or react dev tools will complain)
    });
  };

  const fetchGates = (event: React.MouseEvent<HTMLButtonElement>) => {
    const button = event.currentTarget;
    button.disabled = true;
    setGatesResult([]);
    axios.get<ResponseGates>(`${globalData.domain}/api/gates/fetch`).then(response => {
      let gates = [];
      for (let i = 0; i < response.data.ansiblexes.length; i++) {
        gates.push(response.data.ansiblexes[i].name);
      }
      setGatesResult(gates);
    }).catch(() => {
      setGatesResult([t('app.error')]);
    }).then(() => {
      button.disabled = false;
    });
  };

  const updateGates = (event: React.MouseEvent<HTMLButtonElement>) => {
    const button = event.currentTarget;
    button.disabled = true;
    setGatesResult([]);
    axios.get<ResponseGates>(`${globalData.domain}/api/gates/update`).then(response => {
      if (response.data.message) { // some error
        setGatesResult([response.data.message]);
        return;
      }
      let gates = [];
      for (let i = 0; i < response.data.ansiblexes.length; i++) {
        gates.push(response.data.ansiblexes[i].name);
      }
      setGatesResult(gates);
      fetchGatesUpdated();
    }).catch(() => {
      setGatesResult([t('app.error')]);
    }).then(() => {
      button.disabled = false;
    });
  };

  return (
    <p>
      <button onClick={fetchGates}>{t('updateGates.show-gates')}</button>
      <button onClick={updateGates}>{t('navModal.update-gates')}</button>
      {t('updateGates.last-update')}: {gatesUpdated}<br/>
      {gatesResult.map((value, index) => {
        return <span key={index}>{value}<br/></span>
      })}
    </p>
  )
}
