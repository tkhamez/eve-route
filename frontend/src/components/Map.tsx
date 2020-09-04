import React, { useCallback, useEffect, useState } from 'react';
import axios from "axios";
import { makeStyles } from "@material-ui/core/styles";

type MapData = {
  min: { x: number, y: number },
  max: { x: number, y: number },
  systems: Array<{ id: bigint, name: string, security: number, position: { x: number, y: number } }>,
  connections: Array<{ x1: number, y1: number, x2: number, y2: number }>,
}

const useStyles = makeStyles((theme) => ({
  map: {
    backgroundColor: theme.palette.grey[900],
    borderRadius: '4px',
  },
}));

export default function Map() {
  const classes = useStyles();
  const [mapData, setMapData] = useState<MapData>();
  const [svgLoaded, setSvgLoaded] = useState(false);

  const addMapData = useCallback(() => {
    const obj: any = document.getElementById('map');
    const svg: Document = obj.contentDocument;

    const systems = svg.getElementById("systems");
    const connections = svg.getElementById("connections");
    if (!mapData || connections === null || systems === null) {
      return;
    }

    const systemRadius = 7;

    const minX = mapData.min.x - systemRadius;
    const minY = mapData.min.y - systemRadius;
    const width = mapData.max.x + (mapData.min.x * -1) + (systemRadius*2);
    const height = mapData.max.y + (mapData.min.y * -1) + (systemRadius*2);
    svg.activeElement?.setAttribute("viewBox", `${minX} ${minY} ${width} ${height}`);

    mapData.systems.forEach(system => {
      let nodeClass;
      if (system.security <= 0) {
        nodeClass = "null-sec";
      } else if (system.security < 0.5) {
        nodeClass = "low-sec";
      } else {
        nodeClass = "high-sec";
      }
      const newSystem = document.createElementNS("http://www.w3.org/2000/svg", "circle");
      newSystem.setAttribute("cx", system.position.x.toString());
      newSystem.setAttribute("cy", system.position.y.toString());
      newSystem.setAttribute("r", systemRadius.toString());
      newSystem.setAttribute("class", nodeClass);
      systems.appendChild(newSystem);
    });

    mapData.connections.forEach(connection => {
      const newLine = document.createElementNS("http://www.w3.org/2000/svg", "line");
      newLine.setAttribute("x1", connection.x1.toString());
      newLine.setAttribute("y1", connection.y1.toString());
      newLine.setAttribute("x2", connection.x2.toString());
      newLine.setAttribute("y2", connection.y2.toString());
      newLine.setAttribute("class", "connection");
      connections.appendChild(newLine);
    });
  }, [mapData]);

  const svgOnLoad = () => {
    setSvgLoaded(true);
  };

  useEffect(() => {
      axios.get<MapData>('/map.json').then(result => {
        setMapData(result.data);
      }).catch((e) => {
        console.log(e);
      });
  }, []); // only executed once!

  useEffect(() => {
    if (mapData && svgLoaded) {
        addMapData();
    }
  }, [addMapData, mapData, svgLoaded]);

  return (
    <div className={classes.map}>
      <object id="map" type="image/svg+xml" data="/map.svg" onLoad={svgOnLoad}>SVG</object>
    </div>
  )
}
