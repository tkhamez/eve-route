import React, { useContext, useState } from 'react';
import { useTranslation } from "react-i18next";
import {
  Button,
  FormControl,
  FormControlLabel,
  FormLabel,
  Grid,
  Radio,
  RadioGroup,
  TextField,
  Typography
} from '@material-ui/core';
import axios from "axios";
import { ResponseMessage } from "../response";
import { GlobalDataContext } from "../GlobalDataContext";
import { makeStyles } from "@material-ui/core/styles";

const useStyles = makeStyles(() => ({
  caption: {
    margin: "-5px 0 5px 0",
  },
}));

type Props = {
  connectionChanged: Function,
}

const Admin = (props: Props) => {
  const IMPORT_MODE_REPLACE_REGION = 'replace-region';
  const IMPORT_MODE_ADD_GATES = 'add-gates';

  const { t } = useTranslation();
  const classes = useStyles();
  const globalData = useContext(GlobalDataContext);
  const [input, setInput] = useState('');
  const [mode, setMode] = useState(IMPORT_MODE_ADD_GATES);
  const [result, setResult] = useState('');
  const [submitDisabled, setSubmitDisabled] = useState(false);

  const handleModeChange = (event: any) => {
    setMode(event.target.value);
  };

  const handleInput = (event: any) => {
    setInput(event.target.value);
  };

  const submit = () => {
    setResult('');
    setSubmitDisabled(true);
    axios.post<ResponseMessage>(
      `${globalData.domain}/api/import/from-game?mode=${mode}`,
      input,
      { headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' } },
    ).then(r => {
      if (r.data.success) {
        setInput('');
        setResult(t(`responseCode.${r.data.code}`, {number: r.data.param}));
        props.connectionChanged();
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

        <FormControl component="fieldset">
          <FormLabel component="legend">{t('import.importMode')}</FormLabel>
          <RadioGroup aria-label={t('import.importMode')} name="mode" value={mode} onChange={handleModeChange}>
            <FormControlLabel value={IMPORT_MODE_ADD_GATES} control={<Radio color="primary" />}
                              label={t('import.addGates')} />
            <Typography className={classes.caption} variant={"caption"}>{t('import.addDescription')}</Typography>
            <FormControlLabel value={IMPORT_MODE_REPLACE_REGION} control={<Radio color="primary" />}
                              label={t('import.replaceRegion')} />
            <Typography className={classes.caption} variant={"caption"}>{t('import.replaceDescription')}</Typography>
          </RadioGroup>
        </FormControl>

        <p>
          <strong>{t('import.howTo')}</strong><br/>
          <em>{t('import.copy1Label')}:</em>{' '}
          {t('import.copy1Instruction')}<br/>
          <em>{t('import.copy2Label')}:</em>{' '}
          {t('import.copy2Instruction')}
        </p>

        <Typography color={"error"}>{t('import.warning')}</Typography>

        <TextField
          value={input}
          onChange={handleInput}
          multiline
          rows={6}
          fullWidth
          variant="filled"
          label={t('import.textFieldLabel')}
        /><br/>
        <br/>

        <Button variant="contained" color="primary" onClick={submit} disabled={submitDisabled}>
          {t('import.submit')}
        </Button>
        <br/>

        {result}
      </Grid>
    </Grid>
  );
};

export default Admin;
