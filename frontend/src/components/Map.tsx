import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { makeStyles } from '@material-ui/core/styles';
import { MapData } from '../types';
import { Waypoint } from '../response';

const useStyles = makeStyles((theme) => ({
  map: {
    backgroundColor: theme.palette.grey[900],
    borderRadius: '4px',
  },
}));

type Props = {
  waypoints: Array<Waypoint>,
}

// SVG viewBox
let minX = 0;
let minY = 0;
let width = 0;
let height = 0;

export default function Map(props: Props) {
  const classes = useStyles();
  const [mapData, setMapData] = useState<MapData>();
  const [svgLoaded, setSvgLoaded] = useState(false);

  const svgOnLoad = () => {
    setSvgLoaded(true);
  };

  useEffect(() => {
      axios.get<MapData>('/map.json').then(result => {
        setMapData(result.data);
      }).catch((e) => {
        console.log(e); // TODO show error
      });
  }, []); // only executed once!

  useEffect(() => {
    const addMapData = () => {
      const svg = getSvg();
      const systems = svg.getElementById('systems');
      const connections = svg.getElementById('connections');
      if (!mapData || connections === null || systems === null) {
        return;
      }

      const systemRadius = 7;
      minX = mapData.min.x - systemRadius;
      minY = mapData.min.y - systemRadius;
      width = mapData.max.x + (mapData.min.x * -1) + (systemRadius*2);
      height = mapData.max.y + (mapData.min.y * -1) + (systemRadius*2);
      svg.activeElement?.setAttribute("viewBox", `${minX} ${minY} ${width} ${height}`);

      mapData.systems.forEach(system => {
        let nodeClass;
        if (system.security <= 0) {
          nodeClass = 'null-sec';
        } else if (system.security < 0.5) {
          nodeClass = 'low-sec';
        } else {
          nodeClass = 'high-sec';
        }
        const newSystem = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        newSystem.setAttribute('cx', system.position.x.toString());
        newSystem.setAttribute('cy', system.position.y.toString());
        newSystem.setAttribute('r', systemRadius.toString());
        newSystem.setAttribute('class', nodeClass);
        systems.appendChild(newSystem);
      });

      mapData.connections.forEach(connection => {
        const newLine = createLine(connection.x1, connection.y1, connection.x2, connection.y2, 'connection');
        connections.appendChild(newLine);
      });
    };

    if (mapData && svgLoaded) {
        addMapData();
    }
  }, [mapData, svgLoaded]);

  useEffect(() => {
    if (!mapData || !svgLoaded) {
      return;
    }

    const svg = getSvg();
    const connections = svg.getElementById('connections');
    if (connections == null) {
      return;
    }

    // remove old route
    Array.from(connections.getElementsByClassName('route')).forEach((el) => {
      connections.removeChild(el);
    });
    svg.activeElement?.setAttribute("viewBox", `${minX} ${minY} ${width} ${height}`);

    if (props.waypoints.length === 0) {
      return;
    }

    // find coordinates
    let routeMinX = width;
    let routeMaxX = minX;
    let routeMinY = height;
    let routeMaxY = minY;
    let connection: Array<{ x: number, y: number, type: string|null }> = [];
    const route: Array<typeof connection> = [];
    props.waypoints.forEach(waypoint => {
      mapData.systems.forEach(system => {
        if (system.id === waypoint.systemId) {
          routeMinX = Math.min(routeMinX, system.position.x);
          routeMaxX = Math.max(routeMaxX, system.position.x);
          routeMinY = Math.min(routeMinY, system.position.y);
          routeMaxY = Math.max(routeMaxY, system.position.y);
          if (connection.length < 2) {
            connection.push({
              x: system.position.x,
              y: system.position.y,
              type: waypoint.connectionType,
            });
          }
          if (connection.length === 2) {
            route.push(connection);
            connection = [];
            connection.push({
              x: system.position.x,
              y: system.position.y,
              type: waypoint.connectionType,
            });
          }
        }
      });
    });

    // add route
    route.forEach(connection => {
      let classes = 'route';
      if (connection[0].type === 'Ansiblex') {
        classes += ' ansiblex';
      }
      const newLine = createLine(connection[0].x, connection[0].y, connection[1].x, connection[1].y, classes);
      connections.appendChild(newLine);
    });

    // zoom in - TODO keep original w/h ration?
    routeMinX -= width / 12;
    routeMinY -= height / 12;
    let routeWidth = routeMaxX - routeMinX + (width / 12);
    let routeHeight = routeMaxY - routeMinY + (height / 12);
    if (routeMinX < minX) {
      routeMinX = minX;
    }
    if (routeMinY < minY) {
      routeMinY = minY;
    }
    if (routeWidth > width) {
      routeWidth = width;
    }
    if (routeHeight > height) {
      routeHeight = height;
    }
    svg.activeElement?.setAttribute("viewBox", `${routeMinX} ${routeMinY} ${routeWidth} ${routeHeight}`);

  }, [mapData, props.waypoints, svgLoaded]);

  return (
    <div className={classes.map}>
      <object id="map" type="image/svg+xml" data="/map.svg" onLoad={svgOnLoad}>SVG</object>
    </div>
  )
}

function getSvg(): Document {
  const obj: any = document.getElementById('map');
  return obj.contentDocument;
}

function createLine(x1: number, y1: number, x2: number, y2: number, classes: string) {
  const newLine = document.createElementNS('http://www.w3.org/2000/svg', 'line');
  newLine.setAttribute('x1', x1.toString());
  newLine.setAttribute('y1', y1.toString());
  newLine.setAttribute('x2', x2.toString());
  newLine.setAttribute('y2', y2.toString());
  newLine.setAttribute('class', classes);
  return newLine;
}
