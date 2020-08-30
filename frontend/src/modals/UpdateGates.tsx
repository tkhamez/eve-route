import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, TextField } from "@material-ui/core";
import CloseIcon from '@material-ui/icons/Close';
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseGatesUpdated, ResponseGates, ResponseMessage } from '../response';
import { makeStyles } from "@material-ui/core/styles";

const useStyles = makeStyles(() => ({
  submit: {
    position: "relative",
    top: "17px",
  },
}));

export default function HowThisWorks() {
  const { t } = useTranslation();
  const classes = useStyles();
  const globalData = useContext(GlobalDataContext);
  const [gatesUpdated, setGatesUpdated] = useState<Date|null>(null);
  const [gatesResult, setGatesResult] = useState<Array<string>>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchResult, setSearchResult] = useState('');
  const [submitDisabled, setSubmitDisabled] = useState(false);

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

  const search = () => {
    if (searchTerm.length === 0) {
      setSearchResult(t('updateGates.input-required'));
      return;
    }
    setSearchResult('');
    setSubmitDisabled(true);
    axios.get<ResponseMessage>(`${globalData.domain}/api/gates/search/${searchTerm}`).then(response => {
      setSearchResult(t(`responseCode.${response.data.code}`, {number: response.data.param}));
    }).catch(() => {
      setSearchResult(t('app.error'));
    }).then(() => {
      setSubmitDisabled(false);
    });
  };

  return (
    <div>
      <p>{t('updateGates.last-update')} {gatesUpdated}</p>
      <div>
        <TextField
          required
          variant="filled"
          label={t('updateGates.esi-search-label')}
          helperText={t('updateGates.esi-search-help')}
          onChange={e => setSearchTerm(e.target.value)}
        />
        {' '}
        <Button className={classes.submit} variant="contained"  disabled={submitDisabled}
                onClick={search}>{t('updateGates.submit')}</Button>
      </div>
      <div>
        {searchResult}
        &nbsp; {/* show an empty line initially */}
      </div>
      <br/>
      <br/>
      <div>
        <Button size={"small"} variant="contained" disableElevation
                onClick={fetchGates}>{t('updateGates.show-gates')}</Button>
        {' '}
        {gatesResult.length > 0 &&
          <Button size={"small"} variant="outlined" onClick={() => setGatesResult([])}><CloseIcon /></Button>
        }
        <ul>
          {gatesResult.map((value, index) => {
            return <li key={index}>{value}<br/></li>
          })}
        </ul>
      </div>
    </div>
  )
}
