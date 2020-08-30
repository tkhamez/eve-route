import React from 'react';
import { useTranslation } from "react-i18next";
import { Box, Button, Grid, Modal } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import HowThisWorks from "../modals/HowThisWorks";
import Limitations from "../modals/Limitations";
import UpdateGates from "../modals/UpdateGates";
import Wormholes from "../modals/Wormholes";

const useStyles = makeStyles((theme) => ({
  topButton: {
    margin: theme.spacing(1),
    textTransform: 'none',
  },
  content: {
    top: '15px',
    left: '50%',
    transform: `translate(-50%)`,
    position: 'absolute',
    width: '90%',
    maxWidth: '882px', // 960 - 2*24 + 2*15
    maxHeight: 'calc(100vh - 15px - 15px)',
    overflow: 'auto',
    backgroundColor: theme.palette.background.default,
    //border: '2px solid #000',
    borderRadius: '2px',
    boxShadow: theme.shadows[5],
    padding: theme.spacing(2),
  },
}));

type Props = {
  classesCard: any,
}

export default function NavModal(props: Props) {
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
      <Grid container spacing={2} className={props.classesCard}>
        <Grid item xs={6}>
          <Box display="flex" justifyContent="left">
            <Button size="small" className={classes.topButton} color="primary" disableRipple
                    onClick={() => handleOpen('HowThisWorks')}>{t('navModal.how-this-works')}</Button>
            <Button size="small" className={classes.topButton} color="primary" disableRipple
                    onClick={() => handleOpen('Limitations')}>{t('navModal.limitations')}</Button>
          </Box>
        </Grid>
        <Grid item xs={6}>
          <Box display="flex" justifyContent="right">
            <Button size="small" className={classes.topButton} color="primary" disableRipple
                    onClick={() => handleOpen('UpdateGates')}>{t('navModal.update-gates')}</Button>
            <Button size="small" className={classes.topButton} color="primary" disableRipple
                    onClick={() => handleOpen('Wormholes')}>{t('navModal.add-wormhole-connections')}</Button>
          </Box>
        </Grid>
      </Grid>
      <Modal
        open={open}
        onClose={handleClose}
        aria-labelledby="simple-modal-title"
        aria-describedby="simple-modal-description"
      >
        <div className={classes.content}>
          {content === 'HowThisWorks' && <HowThisWorks />}
          {content === 'Limitations' && <Limitations />}
          {content === 'UpdateGates' && <UpdateGates />}
          {content === 'Wormholes' && <Wormholes />}
        </div>
      </Modal>
    </div>
  )
}
