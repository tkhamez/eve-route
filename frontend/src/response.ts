/**
 * Counterparts of backend data classes located in src/data/response.kt.
 */

export type ResponseAuthUser = {
  characterId: bigint,
  characterName: string,
  allianceId: bigint|null,
}

export type ResponseGates = {
  message: string,
  ansiblexes: Array<Ansiblex>,
}

export type ResponseGatesUpdated = {
  message: string,
  allianceId: bigint|null,
  updated: Date|null,
}

export type ResponseRouteSystems = {
  systems: Array<string>,
}

export type ResponseRouteFind = {
  message: string,
  route: Array<Waypoint>,
}

export type ResponseRouteSet = {
  message: string,
}

export type Ansiblex = {
  id: number,
  name: string,
  solarSystemId: bigint,
}

export type Waypoint = {
  systemId: bigint,
  systemName: string,
  systemSecurity: number,
  connectionType: RouteType["Stargate"]|RouteType['Ansiblex']|null,
  ansiblexId: number|null,
  ansiblexName: string|null,
}

export type RouteType = {
  Stargate: "Stargate",
  Ansiblex: "Ansiblex",
}
