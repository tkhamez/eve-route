import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Button, Grid, IconButton, Modal, Typography } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import AddCircleOutlineIcon from '@material-ui/icons/AddCircleOutline';
import CloseRoundedIcon from '@material-ui/icons/CloseRounded';
import HelpOutlineIcon from '@material-ui/icons/HelpOutline';
import SyncIcon from '@material-ui/icons/Sync';
import { GlobalDataContext } from "../GlobalDataContext";
import HowItWorks from '../modals/HowItWorks';
import EsiUpdate from '../modals/EsiUpdate';
import AddConnection from '../modals/AddConnection';

const useStyles = makeStyles((theme) => ({
  toolbarButton: {
    display: "inline",
    textTransform: 'none',
    //fontWeight: 'normal',
  },
  toolbarButtonIcon: {
    position: "relative",
    top: "5px",
    marginRight: "3px",
  },
  modal: {
    top: '30px',
    left: '50%',
    transform: 'translate(-50%)',
    position: 'absolute',
    width: '90%',
    maxWidth: '560px',
    backgroundColor: theme.palette.background.default,
    borderRadius: '2px',
    boxShadow: theme.shadows[5],
    padding: theme.spacing(2),
  },
  modalCloseButton: {
    position: 'relative',
    top: '-5px',
  },
  modalBody: {
    maxHeight: 'calc(100vh - 30px - 15px - 68px)',
    overflow: 'auto',
    marginTop: '10px',
  },
}));

type Props = {
  connectionChanged: Function,
}

export default function HeaderModalButtons(props: Props) {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();
  const [open, setOpen] = React.useState(false);
  const [content, setContent] = React.useState('');

  const handleOpen = (content: string) => {
    setOpen(true);
    setContent(content);
  };

  const handleClose = () => {
    setOpen(false);
  };

  return (
    <div>
      {globalData.user.name &&
        <Button size="small" className={classes.toolbarButton} color="primary" disableRipple
                onClick={() => handleOpen('EsiUpdate')}>
          <SyncIcon fontSize="small" className={classes.toolbarButtonIcon}/>
          {t('navModal.esi-update')}
        </Button>
      }

      {globalData.user.name &&
        <Button size="small" className={classes.toolbarButton} color="primary" disableRipple
                onClick={() => handleOpen('AddConnection')}>
          <AddCircleOutlineIcon fontSize="small" className={classes.toolbarButtonIcon}/>
          {t('navModal.add-connection')}
        </Button>
      }

      <Button size="small" className={classes.toolbarButton} color="primary" disableRipple
              onClick={() => handleOpen('HowItWorks')}>
        <HelpOutlineIcon fontSize="small" className={classes.toolbarButtonIcon}/>
        {t('navModal.how-it-works')}
      </Button>

      <Modal open={open} onClose={handleClose}>
        <div className={classes.modal}>
          <Grid container style={{borderBottom: "1px solid black"}}>
            <Grid item xs={10}>
              <Box display="flex" justifyContent="flex-start">
                <Typography>
                  <strong>
                    {content === 'EsiUpdate' && t('navModal.esi-update')}
                    {content === 'AddConnection' && t('navModal.add-connection')}
                    {content === 'HowItWorks' && t('navModal.how-it-works')}
                  </strong>
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={2}>
              <Box display="flex" justifyContent="flex-end">
                <IconButton size="small" className={classes.modalCloseButton} onClick={handleClose}>
                  <CloseRoundedIcon/>
                </IconButton>
              </Box>
            </Grid>
          </Grid>
          <div className={classes.modalBody}>
            {content === 'EsiUpdate' && <EsiUpdate />}
            {content === 'AddConnection' && <AddConnection connectionChanged={props.connectionChanged} />}
            {content === 'HowItWorks' && <HowItWorks />}
          </div>
        </div>
      </Modal>
    </div>
  )
}
