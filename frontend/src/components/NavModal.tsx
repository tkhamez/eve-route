import React from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Button, Grid, IconButton, Modal, Typography } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import AddCircleOutlineIcon from '@material-ui/icons/AddCircleOutline';
import CloseRoundedIcon from '@material-ui/icons/CloseRounded';
import HelpOutlineIcon from '@material-ui/icons/HelpOutline';
import SyncIcon from '@material-ui/icons/Sync';
import HowThisWorks from '../modals/HowThisWorks';
import UpdateGates from '../modals/UpdateGates';
import AddConnection from '../modals/AddConnection';

const useStyles = makeStyles((theme) => ({
  topButton: {
    display: "inline",
    margin: theme.spacing(0, 1),
    textTransform: 'none',
  },
  topButtonIcon: {
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
  closeButton: {
    position: 'relative',
    top: '-5px',
  },
  body: {
    maxHeight: 'calc(100vh - 30px - 15px - 68px)',
    overflow: 'auto',
    marginTop: '10px',
  },
}));

export default function NavModal() {
  const { t } = useTranslation();
  const classes = useStyles();
  const [open, setOpen] = React.useState(false);
  const [content, setContent] = React.useState('');

  const handleOpen = (content: string) => {
    setOpen(true);
    setContent(content)
  };

  const handleClose = () => {
    setOpen(false);
  };

  return (
    <div>
      <Grid container spacing={2} className='card'>
        <Grid item xs={5}>
          <Box display="flex" justifyContent="left">
            <Button size="small" className={classes.topButton} color="primary" disableRipple
                    onClick={() => handleOpen('HowThisWorks')}>
              <HelpOutlineIcon fontSize="small" className={classes.topButtonIcon} />
              {t('navModal.how-this-works-limitations')}
            </Button>
          </Box>
        </Grid>
        <Grid item xs={7}>
          <Box display="flex" justifyContent="right">
            <Button size="small" className={classes.topButton} color="primary" disableRipple
                    onClick={() => handleOpen('UpdateGates')}>
              <SyncIcon fontSize="small" className={classes.topButtonIcon} />
              {t('navModal.update-gates')}
            </Button>
            <Button size="small" className={classes.topButton} color="primary" disableRipple
                    onClick={() => handleOpen('AddConnection')}>
              <AddCircleOutlineIcon fontSize="small" className={classes.topButtonIcon} />
              {t('navModal.add-connection')}
            </Button>
          </Box>
        </Grid>
      </Grid>
      <Modal open={open} onClose={handleClose}>
        <div className={classes.modal}>
          <Grid container style={{borderBottom: "1px solid black"}}>
            <Grid item xs={10}>
              <Box display="flex" justifyContent="left">
                <Typography>
                  {content === 'HowThisWorks' && t('navModal.how-this-works-limitations')}
                  {content === 'UpdateGates' && t('navModal.update-gates')}
                  {content === 'AddConnection' && t('navModal.add-connection')}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={2}>
              <Box display="flex" justifyContent="right">
                <IconButton size="small" className={classes.closeButton} onClick={handleClose}>
                  <CloseRoundedIcon/>
                </IconButton>
              </Box>
            </Grid>
          </Grid>
          <div className={classes.body}>
            {content === 'HowThisWorks' && <HowThisWorks />}
            {content === 'UpdateGates' && <UpdateGates />}
            {content === 'AddConnection' && <AddConnection />}
          </div>
        </div>
      </Modal>
    </div>
  )
}
