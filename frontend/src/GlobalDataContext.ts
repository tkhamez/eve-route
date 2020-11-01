import React from "react";

export type userType = {
  name: string,
  allianceName: string,
  allianceTicker: string,
  roles: Array<string>,
};

export const emptyUser = {
  name: '',
  allianceName: '',
  allianceTicker: '',
  roles: [''],
};

export const GlobalDataContext = React.createContext({
  domain: '',
  user: emptyUser,
  mapConnections: {
    code: '',
    ansiblexes: [],
    temporary: [],
  },
  logoutUser: function() {},
});
