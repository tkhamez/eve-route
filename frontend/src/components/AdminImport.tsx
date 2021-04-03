import React, { useContext, useState } from 'react';
import { useTranslation } from "react-i18next";
import axios from "axios";
import { ResponseMessage } from "../response";
import { GlobalDataContext } from "../GlobalDataContext";
import {
  Button,
  FormControl,
  FormControlLabel,
  FormLabel, Grid,
  Radio,
  RadioGroup,
  TextField,
  Typography
} from "@material-ui/core";
import { makeStyles } from "@material-ui/core/styles";

const useStyles = makeStyles(() => ({
  caption: {
    margin: "-5px 0 5px 0",
  },
}));

type Props = {
  disabled: boolean,
  connectionChanged: Function,
}

const AdminImport = (props: Props) => {
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
    <Grid item md={6} xs={12}>
      <h3>{t('adminImport.headline')}</h3>
      <FormControl component="fieldset">
        <FormLabel component="legend">{t('adminImport.importMode')}:</FormLabel>
        <RadioGroup aria-label={t('adminImport.importMode')} name="mode" value={mode} onChange={handleModeChange}>
          <FormControlLabel value={IMPORT_MODE_ADD_GATES} label={t('adminImport.addGates')}
                            control={<Radio color="primary" disabled={props.disabled} />} />
          <Typography className={classes.caption} variant={"caption"}>{t('adminImport.addDescription')}</Typography>
          <FormControlLabel value={IMPORT_MODE_REPLACE_REGION} label={t('adminImport.replaceRegion')}
                            control={<Radio color="primary" disabled={props.disabled} />} />
          <Typography className={classes.caption} variant={"caption"}>{t('adminImport.replaceDescription')}</Typography>
        </RadioGroup>
      </FormControl>
      <p>
        <strong>{t('adminImport.howTo')}</strong><br/>
        <em>{t('adminImport.copy1Label')}:</em>{' '}
        {t('adminImport.copy1Instruction')}<br/>
        <em>{t('adminImport.copy2Label')}:</em>{' '}
        {t('adminImport.copy2Instruction')}
      </p>
      <Typography variant="body2" color={'error'}>{t('adminImport.warning')}</Typography>
      <TextField
        value={input}
        onChange={handleInput}
        multiline
        rows={6}
        fullWidth
        variant="filled"
        label={t('adminImport.textFieldLabel')}
        disabled={props.disabled}
      /><br/>
      <br/>
      <Button variant="contained" color="primary" onClick={submit} disabled={submitDisabled || props.disabled}>
        {t('adminImport.submit')}
      </Button>
      <br/>
      {result}
    </Grid>
  );
};

export default AdminImport;
