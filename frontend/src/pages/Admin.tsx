import React, { useCallback, useContext, useEffect, useState } from 'react';
import { useTranslation } from "react-i18next";
import { Grid, Typography } from '@material-ui/core';
import AdminList from "../components/AdminList";
import AdminImport from "../components/AdminImport";
import { GlobalDataContext } from "../GlobalDataContext";
import axios from "axios";
import { Ansiblex, ResponseGates } from "../response";

type Props = {
  connectionChanged: Function,
}

const Admin = (props: Props) => {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const [disabled, setDisabled] = useState(true);
  const [gatesPerRegion, setGatesPerRegion] = useState<Array<Ansiblex[]>>([]);

  useEffect(() => {
    if (globalData.user.roles.indexOf('import') !== -1) {
      setDisabled(false);
    }
  }, [globalData]);

  const ansiblexesChanged = useCallback(() => {
    setGatesPerRegion([]);
    axios.get<ResponseGates>(`${globalData.domain}/api/gates/fetch`).then(response => {
      if (response.data.code) {
        // show the error message t(`responseCode.${response.data.code}`) somehow?
      } else {
        let lastRegionName = '';
        let gates: Array<Ansiblex> = [];
        let gatesGrouped: Array<Ansiblex[]> = [];
        response.data.ansiblexes.forEach((gate) => {
          if (lastRegionName !== gate.regionName) {
            lastRegionName = gate.regionName;
            if (gates.length > 0) {
              gatesGrouped.push(gates);
            }
            gates = [];
          }
          gates.push(gate);
        });
        if (gates.length > 0) {
          gatesGrouped.push(gates);
        }
        setGatesPerRegion(gatesGrouped);
      }
    }).catch(() => {
      // show an error message somehow?
    });
  }, [globalData.domain]);

  useEffect(() => {
    ansiblexesChanged()
  }, [ansiblexesChanged]);

  return (
    <div>
      {disabled &&
        <Typography variant="body2" color={"textSecondary"}><br/>{t('adminImport.missingRole')}</Typography>
      }
      <Grid container spacing={2} className='card'>
        <AdminImport disabled={disabled}
                     connectionChanged={props.connectionChanged}
                     ansiblexesChanged={ansiblexesChanged} />
        <AdminList disabled={disabled}
                   connectionChanged={props.connectionChanged}
                   ansiblexesChanged={ansiblexesChanged}
                   gatesPerRegion={gatesPerRegion} />
      </Grid>
    </div>
  );
};

export default Admin;
