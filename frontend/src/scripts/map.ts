import { MapData } from "../types";
const fs = require('fs');

fs.writeFileSync('public/map.json', JSON.stringify(readJson()));
console.info('Wrote public/map.json.');

function readJson() {
  const data: MapData = {
    min: { x: 0, y: 0 },
    max: { x: 0, y: 0 },
    systems: [],
    connections: [],
  };

  const mod = 100000000000000;
  const dataPath = "../esi-data/json/universe/";

  const regionDenyList = [
    //'A821-A', 'J7HZ-F', 'UUA-F4', // unreachable normal regions
    'A-R00001', 'A-R00002', 'A-R00003', // Wormholes
    'ADR01', 'ADR02', 'ADR03', 'ADR04', 'ADR05', // Abyssal Deadspace
    'PR-01', // unknown
  ];
  const regionDenyListSubString = '-R00'; // Wormholes

  const regionsData = JSON.parse(fs.readFileSync(dataPath+"regions/regions.json", "utf8"));
  for (const regionData of regionsData) {
    if (
      regionDenyList.indexOf(regionData.name) !== -1 ||
      regionData.name.indexOf(regionDenyListSubString) !== -1
    ) {
      continue;
    }

    const systemsData = fs.readFileSync(dataPath+"systems/" + regionData.name + "-systems.json", "utf8");
    for (const systemData of JSON.parse(systemsData)) {
      let security;
      if (systemData.securityStatus > 0 && systemData.securityStatus < 0.05) {
        security = 0.1;
      } else if (systemData.securityStatus <= 0.0) {
        security = Math.round(systemData.securityStatus * 100) / 100;
      } else {
        security = Math.round(systemData.securityStatus * 10) / 10;
      }
      data.systems.push({
        id: systemData.id,
        name: systemData.name,
        security: security,
        position: { x: Math.round(systemData.position.x/mod), y: Math.round(systemData.position.z/mod) * -1}
      });
      data.min.x = Math.min(data.min.x, Math.round(systemData.position.x/mod));
      data.max.x = Math.max(data.max.x, Math.round(systemData.position.x/mod));
      data.min.y = Math.min(data.min.y, Math.round(systemData.position.z/mod) * -1);
      data.max.y = Math.max(data.max.y, Math.round(systemData.position.z/mod) * -1);
    }

    const stargatesDataJson = fs.readFileSync(dataPath+"stargates/" + regionData.name + "-stargates.json", "utf8" );
    const stargatesData = JSON.parse(stargatesDataJson);
    const uniqueEdges = [];
    for (const stargateData of stargatesData) {
      let uniqueEdge;
      if (stargateData.systemId < stargateData.destination.systemId) {
        uniqueEdge = `${stargateData.systemId}-${stargateData.destination.systemId}`;
      } else {
        uniqueEdge = `${stargateData.destination.systemId}-${stargateData.systemId}`;
      }
      if (uniqueEdges.indexOf(uniqueEdge) !== -1) {
        continue;
      }
      uniqueEdges.push(uniqueEdge);
      let origin;
      let destination;
      for (const system of data.systems) {
        if (system.id === stargateData.systemId) {
          origin = { x: system.position.x, y: system.position.y };
        }
        if (system.id === stargateData.destination.systemId) {
          destination = { x: system.position.x, y: system.position.y };
        }
        if (origin && destination) {
          break;
        }
      }
      if (origin && destination) {
        data.connections.push({ x1: origin.x, y1: origin.y, x2: destination.x, y2: destination.y });
      }
    }
  }

  return data;
}
