import React, { useCallback, useContext, useEffect, useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { Button, Grid, IconButton, Link, TextField, Typography } from "@material-ui/core";
import CloseIcon from '@material-ui/icons/Close';
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseGatesUpdated, ResponseGates, ResponseMessage } from '../response';
import { dateFormat } from "../date";

export default function UpdateGates() {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const [gatesUpdated, setGatesUpdated] = useState('');
  const [gatesResult, setGatesResult] = useState<Array<string>|null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchResult, setSearchResult] = useState('');
  const [submitDisabled, setSubmitDisabled] = useState(false);

  const fetchGatesUpdated = useCallback(() => {
    axios.get<ResponseGatesUpdated>(`${globalData.domain}/api/gates/last-update`).then(response => {
      if (response.data.code) {
        setGatesUpdated(t(`responseCode.${response.data.code}`));
      } else if (response.data.updated) {
        setGatesUpdated(dateFormat(response.data.updated));
      }
    }).catch(() => {});
  }, [globalData.domain, t]);

  const fetchGates = (event: React.MouseEvent<HTMLButtonElement>) => {
    const button = event.currentTarget;
    button.disabled = true;
    setGatesResult(null);
    axios.get<ResponseGates>(`${globalData.domain}/api/gates/fetch`).then(response => {
      let gates = [];
      if (response.data.code) {
        gates.push(t(`responseCode.${response.data.code}`));
      } else {
        for (let i = 0; i < response.data.ansiblexes.length; i++) {
          gates.push(response.data.ansiblexes[i].name);
        }
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
      fetchGatesUpdated();
    }).catch(() => {
      setSearchResult(t('app.error'));
    }).then(() => {
      setSubmitDisabled(false);
    });
  };

  useEffect(() => {
    if (gatesUpdated === '') {
      fetchGatesUpdated();
    }
  }, [fetchGatesUpdated, gatesUpdated]);

  return (
    <div className="grid-spacing-2-wrapper">
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Typography variant="body2">
            <Trans i18nKey="updateGates.intro">
              %
              <Link href="https://developers.eveonline.com/blog/article/the-esi-api-is-a-shared-resource-do-not-abuse-it"
                    target="_blank" rel="noopener noreferrer">%</Link>
              %<Link href="https://github.com/esi/esi-issues/issues/1185"
                     target="_blank" rel="noopener noreferrer">%</Link>%
            </Trans>
          </Typography>
          <br/>
          <Typography variant="body2">{t('updateGates.last-update')} {gatesUpdated}</Typography>
        </Grid>
        <Grid item xs={12}>
          <TextField
            required
            variant="filled"
            label={t('updateGates.esi-search-label')}
            helperText={t('updateGates.esi-search-help')}
            onChange={e => setSearchTerm(e.target.value)}
            style={{verticalAlign: 'baseline'}}
          />
          {' '}
          <Button variant="contained"  disabled={submitDisabled}
                  onClick={search}>{t('updateGates.submit')}</Button>

          <div>{searchResult}</div>
        </Grid>
        <Grid item xs={12}>
          <br/>
          <Button size={"small"} variant="contained" disableElevation
                  onClick={fetchGates}>{t('updateGates.show-gates')}</Button>
          {' '}
          {gatesResult !== null &&
          <span>
            <IconButton size="small" onClick={() => setGatesResult(null)}><CloseIcon /></IconButton>
            {' '}
            {t('updateGates.num-gates', {number: gatesResult.length})}
          </span>
          }
          {gatesResult !== null &&
            <ul>
              {gatesResult.map((value, index) => {
                return <li key={index}>{value}<br/></li>
              })}
            </ul>
          }
        </Grid>
      </Grid>
    </div>
  )
}
