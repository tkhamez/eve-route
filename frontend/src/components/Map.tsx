import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import axios from 'axios';
import { makeStyles } from '@material-ui/core/styles';
import RotateLeftIcon from '@material-ui/icons/RotateLeft';
import ZoomInIcon from '@material-ui/icons/ZoomIn';
import ZoomOutIcon from '@material-ui/icons/ZoomOut';
import { MapData } from '../types';
import { Waypoint } from '../response';

const useStyles = makeStyles((theme) => ({
  map: {
    position: 'relative',
    backgroundColor: theme.palette.grey[900],
    borderRadius: '4px',
    fontSize: 0,
  },
  icons: {
    position: 'absolute',
    right: 0,
    cursor: 'pointer',
    color: 'grey',
    backgroundColor: theme.palette.grey[900],
    opacity: 0.9,
  },
  icon: {
    '&:hover': {
      color: 'lightgrey',
    }
  }
}));

type Props = {
  waypoints: Array<Waypoint>,
}

export default function Map(props: Props) {
  const { t } = useTranslation();
  const classes = useStyles();
  const [mapData, setMapData] = useState<MapData>();
  const [svgLoaded, setSvgLoaded] = useState(false);

  /**
   * Load JSON.
   */
  useEffect(() => {
      axios.get<MapData>('/map.json').then(result => {
        setMapData(result.data);
      }).catch((e) => {
        console.log(e); // TODO show error message?
      });
  }, []); // only executed once!

  /**
   * Configure map and add data.
   */
  useEffect(() => {
    if (!mapData || !svgLoaded) {
      return
    }

    const systems = SVG.getElement('systems');
    const connections = SVG.getElement('connections');

    const systemRadius = 5;
    SVG.setViewBox(mapData, systemRadius);
    SVG.makeDraggable();
    SVG.setupMouseZoom();

    mapData.systems.forEach(system => {
      let nodeClass  = 'system ';
      if (system.security <= 0) {
        nodeClass += 'null-sec';
      } else if (system.security < 0.5) {
        nodeClass += 'low-sec';
      } else {
        nodeClass += 'high-sec';
      }
      const newSystem = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      newSystem.setAttribute('cx', system.position.x.toString());
      newSystem.setAttribute('cy', system.position.y.toString());
      newSystem.setAttribute('r', systemRadius.toString());
      newSystem.setAttribute('class', nodeClass);
      systems.appendChild(newSystem);
    });

    mapData.connections.forEach(connection => {
      SVG.addLine(connections, connection.x1, connection.y1, connection.x2, connection.y2, 'connection');
    });
  }, [mapData, svgLoaded]);

  /**
   * Add route to map.
   */
  useEffect(() => {
    if (!mapData || !svgLoaded) {
      return;
    }

    const route = SVG.getElement('route');

    // remove old route
    while (route.lastChild) {
      route.removeChild(route.lastChild);
    }

    if (props.waypoints.length === 0) {
      SVG.reset();
      return;
    }

    // find coordinates
    let routeMinX = Number.MAX_SAFE_INTEGER;
    let routeMaxX = Number.MIN_SAFE_INTEGER;
    let routeMinY = Number.MAX_SAFE_INTEGER;
    let routeMaxY = Number.MIN_SAFE_INTEGER;
    let connection: Array<{ x: number, y: number, type: string|null }> = [];
    const routeData: Array<typeof connection> = [];
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
            routeData.push(connection);
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
    routeData.forEach(connection => {
      let classes = 'route';
      if (connection[0].type === 'Ansiblex') {
        classes += ' ansiblex';
      }
      SVG.addLine(route, connection[0].x, connection[0].y, connection[1].x, connection[1].y, classes);
    });

    SVG.zoomIn(routeMinX, routeMaxX, routeMinY, routeMaxY);

  }, [mapData, props.waypoints, svgLoaded]);

  const svgOnLoad = () => {
    setSvgLoaded(true);
  };

  return (
    <div className={classes.map}>
      <div className={classes.icons}>
        <ZoomInIcon className={classes.icon} onClick={() => SVG.zoom(1.25)} />
        <ZoomOutIcon className={classes.icon} onClick={() => SVG.zoom(0.75)} />
        <RotateLeftIcon className={classes.icon} onClick={() => SVG.reset()} />
      </div>
      <object id="map" type="image/svg+xml" data="/map.svg" onLoad={svgOnLoad}>{t('map.svg-error')}</object>
    </div>
  )
}

const SVG = (() => {
  const getDocument = () => {
    const obj: any = document.getElementById('map');
    return obj.contentDocument;
  };

  const getElementById = (id: string): Element => {
    return getDocument().getElementById(id)
  };

  const applyMatrix = () => {
    getElementById('map').setAttributeNS(null, 'transform', 'matrix(' +  transformMatrix.join(' ') + ')');
  };

  /**
   * transform matrix:
   * [a c e] [x]
   * [b d f] [y]
   * [0 0 1] [1]
   */
  let transformMatrix = [1.0, 0.0, 0.0, 1.0, 0.0, 0.0];
  //                     a    b    c    d    e    f

  const viewBox = {
    x: 0.0, // = minX
    y: 0.0, // = minY
    width: 0.0,
    height: 0.0,
  };

  const max = { x: 0, y: 0 };

  const center = { x: 0, y: 0 };

  return {
    getActiveElement(): SVGGraphicsElement {
      return getDocument().activeElement;
    },

    getElement(id: string): Element {
      return getElementById(id);
    },

    setViewBox(mapData: MapData, systemRadius: number) {
      viewBox.x = mapData.min.x - systemRadius;
      viewBox.y = mapData.min.y - systemRadius;
      viewBox.width = mapData.max.x + (mapData.min.x * -1) + (systemRadius*2);
      viewBox.height = mapData.max.y + (mapData.min.y * -1) + (systemRadius*2);
      max.x = viewBox.x + viewBox.width;
      max.y = viewBox.y + viewBox.height;
      center.x = viewBox.x + (viewBox.width / 2);
      center.y = viewBox.y + (viewBox.height / 2);

      this.getActiveElement().setAttribute('viewBox', `${viewBox.x} ${viewBox.y} ${viewBox.width} ${viewBox.height}`);

      const draggable = this.getElement('draggable');
      draggable.setAttribute('x', viewBox.x.toString());
      draggable.setAttribute('y', viewBox.y.toString());
      draggable.setAttribute('width', viewBox.width.toString());
      draggable.setAttribute('height', viewBox.height.toString());
    },

    makeDraggable() {
      const svgElement = this.getActiveElement();
      svgElement.addEventListener('mousedown', startDrag);
      svgElement.addEventListener('mousemove', drag);
      svgElement.addEventListener('mouseup', endDrag);
      svgElement.addEventListener('mouseleave', endDrag);

      const svg = this;
      let dragging = false;
      let startX = 0;
      let startY = 0;

      function startDrag(evt: MouseEvent) {
        const target = evt.target as Element;
        if (target.classList.contains('draggable')) {
          const coordinates = getMousePosition(evt);
          startX = coordinates.x;
          startY = coordinates.y;
          dragging = true;
        }
      }

      function drag(evt: MouseEvent) {
        if (dragging) {
          evt.preventDefault();
          const coordinates = getMousePosition(evt);
          svg.pan(coordinates.x - startX, coordinates.y - startY);
          startX = coordinates.x;
          startY = coordinates.y;
        }
      }

      function endDrag() {
        dragging = false;
      }

      /**
       * see http://www.petercollingridge.co.uk/tutorials/svg/interactive/dragging/
       */
      function getMousePosition(evt: MouseEvent) {
        const CTM = svgElement.getScreenCTM() as DOMMatrix;
        return {
          x: (evt.clientX - CTM.e) / CTM.a,
          y: (evt.clientY - CTM.f) / CTM.d
        };
      }
    },

    setupMouseZoom() {
      const svg = this;
      const svgElement = this.getActiveElement();
      svgElement.addEventListener('wheel', (evt: WheelEvent) => {
        evt.preventDefault();
        if (evt.deltaY > 0) {
          svg.zoom(evt.deltaY / 3 * 1.1);
        } else {
          svg.zoom(evt.deltaY / -3 * 0.9);
        }
      });
    },

    addLine(parent: Element, x1: number, y1: number, x2: number, y2: number, classes: string) {
      const newLine = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      newLine.setAttribute('x1', x1.toString());
      newLine.setAttribute('y1', y1.toString());
      newLine.setAttribute('x2', x2.toString());
      newLine.setAttribute('y2', y2.toString());
      newLine.setAttribute('class', classes);
      parent.appendChild(newLine);
    },

    pan(dx: number, dy: number) {
      transformMatrix[4] += dx;
      transformMatrix[5] += dy;
      applyMatrix();
    },

    /**
     * see http://www.petercollingridge.co.uk/tutorials/svg/interactive/pan-and-zoom/
     */
    zoom(scale: number) {
      // zoom (to/from ~0,0)
      for (let i = 0; i < 6; i++) {
        transformMatrix[i] *= scale;
      }

      // center
      transformMatrix[4] += (1 - scale) * center.x;
      transformMatrix[5] += (1 - scale) * center.y;

      applyMatrix();
    },

    reset() {
      transformMatrix = [1.0, 0.0, 0.0, 1.0, 0.0, 0.0];
      applyMatrix();
    },

    zoomIn(minX: number, maxX: number, minY: number, maxY: number) {
      // reset
      transformMatrix = [1.0, 0.0, 0.0, 1.0, 0.0, 0.0];

      const width = maxX - minX;
      const height = maxY - minY;
      const centerX = minX + (width/2);
      const centerY = minY + (height/2);

      SVG.pan(center.x - centerX, center.y - centerY);

      const widthMod = viewBox.width / width;
      const heightMod = viewBox.height / height;
      const mod = widthMod < heightMod ? widthMod : heightMod;
      SVG.zoom(mod * 0.8);
    },
  };
})();
