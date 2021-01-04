import React, { useContext, useState } from 'react';
import { useTranslation } from "react-i18next";
import { Button, Grid, TextField, Typography } from '@material-ui/core';
import axios from "axios";
import { ResponseMessage } from "../response";
import { GlobalDataContext } from "../GlobalDataContext";

const Admin = () => {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const [input, setInput] = useState('');
  const [result, setResult] = useState('');
  const [submitDisabled, setSubmitDisabled] = useState(false);

  const handleInput = (event: any) => {
    setInput(event.target.value);
  };

  const submit = () => {
    setResult('');
    setSubmitDisabled(true);
    axios.post<ResponseMessage>(
      `${globalData.domain}/api/import/from-game`,
      input,
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' } },
    ).then(r => {
      if (r.data.success) {
        setInput('');
        setResult(t(`responseCode.${r.data.code}`, {number: r.data.param}));
      } else {
        setResult(t(`responseCode.${r.data.code}`));
      }
      setSubmitDisabled(false);
    }).catch(() => {
      setResult('Error');
      setSubmitDisabled(false);
    });
  };

  return (
    <Grid container spacing={2} className='card'>
      <Grid item xs={12}>
        <h3>{t('import.headline')}</h3>
        <p>{t('import.description')}</p>
        <p>{t('import.instruction')}</p>
        <Typography color={"error"}>{t('import.warning')}</Typography>
        <TextField value={input} onChange={handleInput} multiline rows={6} fullWidth variant="filled"/><br/>
        <Button variant="contained" color="primary" onClick={submit} disabled={submitDisabled}>
          {t('import.submit')}
        </Button><br/>
        {result}
      </Grid>
    </Grid>
  );
};

export default Admin;
