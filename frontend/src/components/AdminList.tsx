import React, { useContext, useState } from 'react';
import { useTranslation } from "react-i18next";
import {
  Button,
  Dialog, DialogActions, DialogContent, DialogContentText,
  DialogTitle,
  Grid,
  IconButton, Table, TableBody, TableCell, TableHead, TableRow,
  Typography
} from "@material-ui/core";
import DeleteForeverIcon from '@material-ui/icons/DeleteForever';
import axios from "axios";
import { Ansiblex,  ResponseMessage } from "../response";
import { GlobalDataContext } from "../GlobalDataContext";
import { makeStyles } from "@material-ui/core/styles";

const useStyles = makeStyles(() => ({
  tableCell: {
    padding: '1px 24px 1px 16px;',
  },
}));

type Props = {
  disabled: boolean,
  connectionChanged: Function,
  ansiblexesChanged: Function,
  gatesPerRegion: Array<Ansiblex[]>,
}

const AdminList = (props: Props) => {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();
  const [message, setMessage] = useState('');
  const [askDeleteGateOpen, setAskDeleteGateOpen] = React.useState(false);
  const [ansiblexDelete, setAnsiblexDelete] = React.useState<Ansiblex|null>(null);

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
            props.ansiblexesChanged();
            props.connectionChanged();
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

      {props.gatesPerRegion.map((gates) => {
        return (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell colSpan={2}>{gates[0].regionName}</TableCell>
                <TableCell>{t('adminGates.source')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {gates.map((ansiblex) => {
                return (
                  <TableRow>
                    <TableCell className={classes.tableCell}>
                      <IconButton size="small" title={t('adminGates.delete-ansiblex')}
                                  disabled={props.disabled} onClick={() => askDeleteGate(ansiblex)}>
                        <DeleteForeverIcon color={props.disabled ? 'disabled' : 'error'} />
                      </IconButton>
                    </TableCell>
                    <TableCell className={classes.tableCell}>{ansiblex.name}</TableCell>
                    <TableCell className={classes.tableCell}>{t(`adminGates.source-${ansiblex.source}`)}</TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        )
      })}

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
