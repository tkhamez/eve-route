import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from "react-i18next";
import { Grid, Typography } from '@material-ui/core';
import AdminList from "../components/AdminList";
import AdminImport from "../components/AdminImport";
import { GlobalDataContext } from "../GlobalDataContext";

type Props = {
  connectionChanged: Function,
}

const Admin = (props: Props) => {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const [disabled, setDisabled] = useState(true);

  useEffect(() => {
    if (globalData.user.roles.indexOf('import') !== -1) {
      setDisabled(false);
    }
  }, [globalData]);

  return (
    <div>
      {disabled &&
        <Typography variant="body2" color={"textSecondary"}><br/>{t('adminImport.missingRole')}</Typography>
      }
      <Grid container spacing={2} className='card'>
        <AdminImport disabled={disabled} connectionChanged={props.connectionChanged} />
        <AdminList disabled={disabled} />
      </Grid>
    </div>
  );
};

export default Admin;
