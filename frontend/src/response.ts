/**
 * Counterparts of backend data classes located in src/data/response.kt.
 */

/**
 * Contains a translation key from the "responseCode" group.
 */
export type ResponseMessage = {
  code: string|null,
  param: string|null,
}

export type ResponseAuthUser = {
  characterId: bigint,
  characterName: string,
  allianceId: bigint|null,
}

export type ResponseGates = {
  code: string|null,
  ansiblexes: Array<Ansiblex>,
}

export type ResponseGatesUpdated = {
  code: string|null,
  allianceId: bigint|null,
  updated: Date|null,
}

export type ResponseSystems = {
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
