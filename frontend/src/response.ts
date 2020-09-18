/**
 * Counterparts of backend data classes located in src/data/response.kt.
 */

/**
 * Contains a translation key from the "responseCode" group.
 */
export type ResponseMessage = {
  code: string|null,
  success: boolean|null,
  param: string|null,
}

export type ResponseAuthUser = {
  name: string,
  allianceName: string,
  allianceTicker: string,
}

export type ResponseGates = {
  code: string|null,
  ansiblexes: Array<Ansiblex>,
}

export type ResponseSystems = {
  systems: Array<System>,
}

export type ResponseConnectedSystems = {
  connections: Array<ConnectedSystems>,
}

export type ResponseMapConnections = {
  code: string|null,
  ansiblexes: Array<ConnectedSystems>,
  temporary: Array<ConnectedSystems>,
}

export type ResponseGatesUpdated = {
  code: string|null,
  allianceId: bigint|null,
  updated: string|null, // yyyy-MM-dd'T'HH:mm:ss'Z'
}

export type ResponseTemporaryConnections = {
  code: string|null,
  temporaryConnections: Array<TemporaryConnection>,
}

export type ResponseSystemNames = {
  systems: Array<string>,
}

export type ResponseRouteFind = {
  code: string|null,
  route: Array<Waypoint>,
}

export type ResponseRouteLocation = {
  code: string|null,
  solarSystemId: bigint|null,
  solarSystemName: string|null,
}

export type Ansiblex = {
  //_id: string,
  id: number,
  name: string,
  solarSystemId: bigint,
  //allianceId: bigint,
}

export type Waypoint = {
  systemId: bigint,
  systemName: string,
  targetSystem: string,
  wormhole: boolean,
  systemSecurity: number,
  connectionType: RouteType|null,
  ansiblexId: number|null,
  ansiblexName: string|null,
}

export enum RouteType {
  Stargate = 'Stargate',
  Ansiblex = 'Ansiblex',
  Temporary = 'Temporary',
}

export type TemporaryConnection = {
  system1Id: bigint,
  system1Name: string,
  system2Id: bigint,
  system2Name: string,
  characterId: bigint,
  created: string // yyyy-MM-dd'T'HH:mm:ss'Z'
}

export type System = {
  id: bigint,
  name: string,
}

export type ConnectedSystems = {
  system1: string,
  system2: string,
}
