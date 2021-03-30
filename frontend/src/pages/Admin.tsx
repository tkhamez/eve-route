import React from 'react';
import { Grid } from '@material-ui/core';
import AdminList from "../components/AdminList";
import AdminImport from "../components/AdminImport";

type Props = {
  connectionChanged: Function,
}

const Admin = (props: Props) => {

  return (
    <Grid container spacing={2} className='card'>
      <AdminImport connectionChanged={props.connectionChanged} />
      <AdminList />
    </Grid>
  );
};

export default Admin;
