/**
 * Counterparts of backend data classes located in src/data/response.kt - Only for the IDE atm.
 */

// eslint-disable-next-line no-unused-vars
const data = {
  ResponseAuthUser: {
    characterId: 0,
    characterName: "",
    allianceId: 0, // nullable
  },

  ResponseGates: {
    message: "",
    ansiblexes: [this.Ansiblex],
  },

  ResponseGatesUpdated: {
    message: "",
    allianceId: 0, // nullable
    updated: new Date(0), // nullable
  },

  ResponseRouteCalculate: {
    message: "",
    route: [this.Waypoint],
  },

  ResponseRouteSet: {
    message: "",
  },

  Ansiblex: {
    id: 0.0,
    name: "",
    solarSystemId: 0,
  },

  Waypoint: {
    systemId: 0,
    systemName: "",
    systemSecurity: 0.0,
    connectionType: this.RouteType.Stargate, // nullable
    ansiblexId: 0.0, // nullable
    ansiblexName: "", // nullable
  },

  RouteType: {
    Stargate: "Stargate",
    Ansiblex: "Ansiblex",
  },
};
