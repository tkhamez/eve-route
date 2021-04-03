import React, { useCallback, useContext, useEffect, useState } from 'react';
import { useTranslation } from "react-i18next";
import {
  Button,
  Dialog, DialogActions, DialogContent, DialogContentText,
  DialogTitle,
  Grid,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Typography
} from "@material-ui/core";
import DeleteForeverIcon from '@material-ui/icons/DeleteForever';
import axios from "axios";
import { Ansiblex, ResponseGates, ResponseMessage } from "../response";
import { GlobalDataContext } from "../GlobalDataContext";
import { makeStyles } from "@material-ui/core/styles";

const useStyles = makeStyles(() => ({
  listItem: {
    display: 'block',
    paddingTop: 0,
    paddingBottom: 0,
  },
  listItemRow: {
    display: 'flex',
    justifyContent: 'flex-start',
    alignItems: 'start',
  },
  listIcon: {
    minWidth: 0,
    marginRight: '4px',
  },
  listText: {
    marginTop: '5px',
    marginBottom: 0,
  },
}));

type Props = {
  disabled: boolean,
}

const AdminList = (props: Props) => {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();
  const [gatesResult, setGatesResult] = useState<Array<Ansiblex>>([]);
  const [message, setMessage] = useState('');
  const [askDeleteGateOpen, setAskDeleteGateOpen] = React.useState(false);
  const [ansiblexDelete, setAnsiblexDelete] = React.useState<Ansiblex|null>(null);

  let lastRegion = '';

  const fetchGates = useCallback(() => {
    setMessage('');
    axios.get<ResponseGates>(`${globalData.domain}/api/gates/fetch`).then(response => {
      if (response.data.code) {
        setMessage(t(`responseCode.${response.data.code}`));
      } else {
        setGatesResult(response.data.ansiblexes);
      }
    }).catch(() => {
      setMessage(t('app.error'));
    });
  }, [globalData.domain, t]);

  useEffect(() => {
    fetchGates();
  }, [fetchGates]);

  const askDeleteGateClose = (deleteAnsiblex: boolean) => {
    setAskDeleteGateOpen(false);
    if (deleteAnsiblex) {
      deleteGate();
    }
  };

  const askDeleteGate = (ansiblex: Ansiblex) => {
    setAnsiblexDelete(ansiblex);
    setAskDeleteGateOpen(true);
  };

  const deleteGate = () => {
    if (!ansiblexDelete) {
      return;
    }
    setMessage('');
    axios.post<ResponseMessage>(`${globalData.domain}/api/gates/delete/${ansiblexDelete.id}`)
      .then((response) => {
        window.scrollTo(0, 0);
        if (response.data.code) { // an error
          setMessage(t(`responseCode.${response.data.code}`));
        } else {
          if (response.data.success) {
            setGatesResult([]);
            fetchGates();
            setMessage(t('adminGates.deleteSuccess'));
          } else {
            setMessage(t('adminGates.deleteError'));
          }
        }
      })
      .catch(() => {
        setMessage(t('app.error'));
      });
  };

  return (
    <Grid item md={6} xs={12}>
      <h3>{t('adminGates.headline')}</h3>
      <Typography color="primary">{message}</Typography>
      <List dense={true}>
        {gatesResult.map((ansiblex, index) => {
          return (
            <ListItem key={index} className={classes.listItem}>
              {lastRegion !== ansiblex.regionName &&
                <Typography>{lastRegion = ansiblex.regionName}</Typography>
              }
              <div className={classes.listItemRow}>
                <IconButton className={classes.listIcon} size="small" title={t('adminGates.delete-ansiblex')}
                            disabled={props.disabled} onClick={() => askDeleteGate(ansiblex)}>
                  <DeleteForeverIcon color={props.disabled ? 'disabled' : 'error'} />
                </IconButton>
                <ListItemText className={classes.listText} primary={ansiblex.name}/>
              </div>
            </ListItem>
          )
        })}
      </List>
      <Dialog
        open={askDeleteGateOpen}
        onClose={askDeleteGateClose}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">{t('adminGates.confirm-delete-title')}</DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            {t('adminGates.confirm-delete-text')}<br/>
            <br/>
            {ansiblexDelete?.name}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => askDeleteGateClose(false)} color="default" autoFocus>{t('adminGates.no')}</Button>
          <Button onClick={() => askDeleteGateClose(true)} color="secondary">{t('adminGates.yes')}</Button>
        </DialogActions>
      </Dialog>
    </Grid>
  );
};

export default AdminList;
