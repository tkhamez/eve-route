import React, { useCallback, useContext, useEffect, useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { Button, Grid, Link, TextField, Typography } from "@material-ui/core";
import axios from 'axios';
import { GlobalDataContext } from '../GlobalDataContext';
import { ResponseGatesUpdated, ResponseMessage } from '../response';
import { dateFormat } from "../date";

export default function EsiUpdate() {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const [gatesUpdated, setGatesUpdated] = useState('');
  const [searchTerm, setSearchTerm] = useState('»');
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

  const search = () => {
    if (searchTerm.length === 0) {
      setSearchResult(t('esiUpdate.input-required'));
      return;
    }
    setSearchResult('');
    setSubmitDisabled(true);
    axios.post<ResponseMessage>(`${globalData.domain}/api/gates/search/${searchTerm}`).then(response => {
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
    <div className="grid-spacing-2-wrapper modal">
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Typography variant="body2">
            <Trans i18nKey="esiUpdate.intro">
              %<Link target="_blank" rel="noopener noreferrer"
                href="https://developers.eveonline.com/blog/article/the-esi-api-is-a-shared-resource-do-not-abuse-it"
              >%</Link>
              %<Link href="https://github.com/esi/esi-issues/issues/1185"
                     target="_blank" rel="noopener noreferrer">%</Link>%
            </Trans>
          </Typography>
          <br/>
          <Typography variant="body2">
            {t('esiUpdate.update-hint')}<br/>
            {t('esiUpdate.last-update')} {gatesUpdated}
          </Typography>
        </Grid>
        <Grid item xs={12}>
          <TextField
            required
            variant="filled"
            label={t('esiUpdate.esi-search-label')}
            helperText={t('esiUpdate.esi-search-help')}
            onChange={e => setSearchTerm(e.target.value)}
            style={{verticalAlign: 'baseline'}}
            defaultValue="»"
          />
          {' '}
          <Button variant="contained" disabled={submitDisabled} onClick={search}>{t('esiUpdate.submit')}</Button>
          <div>{searchResult}</div>
        </Grid>
      </Grid>
    </div>
  )
}
