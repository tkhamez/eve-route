import React, { useCallback, useContext, useEffect, useRef, useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import SystemInput from '../components/SystemInput';
import {
  Box,
  Button,
  Grid, IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow, Typography
} from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import DeleteIcon from '@material-ui/icons/Delete';
import axios from "axios";
import { ResponseMessage, ResponseTemporaryConnections, TemporaryConnection } from "../response";
import { GlobalDataContext } from "../GlobalDataContext";
import { dateAddDays, dateFormat } from "../date";

const useStyles = makeStyles(() => ({
  wrap: {
    minHeight: '410px',
  },
}));

export default function AddConnection() {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();
  const system1Input = useRef();
  const system2Input = useRef();
  const [submitError, setSubmitResult] = useState('');
  const [system1, setSystem1] = React.useState('');
  const [system2, setSystem2] = React.useState('');
  const [submitDisabled, setSubmitDisabled] = useState(true);
  const [temporaryConnections, setTemporaryConnections] = useState<TemporaryConnection[]>([]);
  const [fetchAllError, setFetchAllError] = useState('');

  const system1Changed = (value: string) => {
    setSystem1(value);
    setSubmitDisabled(value === '' || system2 === '');
  };

  const system2Changed = (value: string) => {
    setSystem2(value);
    setSubmitDisabled(system1 === '' || value === '');
  };

  const fetch = useCallback(() => {
    setTemporaryConnections([]);
    setFetchAllError('');
    axios.get<ResponseTemporaryConnections>(`${globalData.domain}/api/connection/get-all`).then(r => {
      if (r.data.code) { // auth error
        setFetchAllError(t(`responseCode.${r.data.code}`));
      }
      setTemporaryConnections(r.data.temporaryConnections);
    }).catch(() => {
        setFetchAllError(t('app.error'));
    });
  }, [globalData.domain, t]);

  const submit = () => {
    setSubmitDisabled(true);
    setSubmitResult('');
    axios.post<ResponseMessage>(
      `${globalData.domain}/api/connection/add`,
      JSON.stringify([system1, system2]),
      { headers: { 'Content-Type': 'application/json' } },
    ).then(r => {
      setSubmitResult(t(`responseCode.${r.data.code}`));
      if (r.data.success) {
        setSystem1('');
        setSystem2('');

        // @ts-ignore
        system1Input.current.clearInput();
        // @ts-ignore
        system2Input.current.clearInput();

        fetch();
      }
    }).catch(() => {
      setSubmitResult(t('app.error'));
    });
  };

  const remove = (id1: bigint, id2: bigint) => {
    axios.delete(`${globalData.domain}/api/connection/delete/${id1}/${id2}`).then(() => {
      fetch();
    }).catch(() => {})
  };

  useEffect(() => {
    fetch();
  }, [fetch]);

  return (
    <div className={`grid-spacing-2-wrapper ${classes.wrap}`}>
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Trans i18nKey="addConnection.choose-systems">%<em>%</em>%</Trans>
        </Grid>
        <Grid item xs={6}>
          <Box display="flex" justifyContent="center">
            <SystemInput ref={system1Input} fieldId="system1" fieldName={t('addConnection.from')}
                         onChange={system1Changed} />
          </Box>
        </Grid>
        <Grid item xs={6}>
          <Box display="flex" justifyContent="center">
            <SystemInput ref={system2Input} fieldId="system2" fieldName={t('addConnection.to')}
                         onChange={system2Changed} />
          </Box>
        </Grid>
        <Grid item xs={12}>
          <Button variant="contained" color="primary" disabled={submitDisabled}
                  onClick={submit}>{t('updateGates.submit')}</Button>
          {' '}{submitError}
        </Grid>
        <Grid item xs={12}>
          <hr/>
        </Grid>
        <Grid item xs={12}>
          <Typography variant="h6" >
            {t('addConnection.your-temporary-connections')}
          </Typography>
          {fetchAllError}
        </Grid>
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('addConnection.from')}</TableCell>
                <TableCell>{t('addConnection.to')}</TableCell>
                <TableCell>{t('addConnection.expires')}</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {temporaryConnections.map((row: TemporaryConnection, index) => (
                <TableRow key={index}>
                  <TableCell>{row.system1Name}</TableCell>
                  <TableCell>{row.system2Name}</TableCell>
                  <TableCell>{dateFormat(dateAddDays(row.created, 2))}</TableCell>
                  <TableCell>
                    <IconButton size="small" color="secondary"
                                aria-label={t('addConnection.delete')} title={t('addConnection.delete')}
                                onClick={() => remove(row.system1Id, row.system2Id)}>
                      <DeleteIcon fontSize="small"/>
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Grid>
    </div>
  )
}
