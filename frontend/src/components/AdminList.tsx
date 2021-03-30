import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from "react-i18next";
import axios from "axios";
import { ResponseGates } from "../response";
import { GlobalDataContext } from "../GlobalDataContext";
import { Grid } from "@material-ui/core";

const AdminList = () => {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const [gatesResult, setGatesResult] = useState<Array<string>|null>(null);

  useEffect(() => {
    axios.get<ResponseGates>(`${globalData.domain}/api/gates/fetch`).then(response => {
      let gates = [];
      if (response.data.code) {
        gates.push(t(`responseCode.${response.data.code}`));
      } else {
        for (let i = 0; i < response.data.ansiblexes.length; i++) {
          gates.push(response.data.ansiblexes[i].name);
        }
      }
      setGatesResult(gates);
    }).catch(() => {
      setGatesResult([t('app.error')]);
    });
  }, [globalData, t]);

  return (
    <Grid item md={6} xs={12}>
      <h3>{t('adminGates.headline')}</h3>
      {gatesResult !== null &&
      <ul>
        {gatesResult.map((value, index) => {
          return <li key={index}>{value}<br/></li>
        })}
      </ul>
      }
    </Grid>
  );
};

export default AdminList;
