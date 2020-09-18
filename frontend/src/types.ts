/**
 * Types shared by several modules.
 */

export type MapData = {
  min: { x: number, y: number },
  max: { x: number, y: number },
  systems: Array<System>,
  connections: Array<{ x1: number, y1: number, x2: number, y2: number }>,
}

export type System = {
  id: bigint,
  name: string,
  security: number,
  position: { x: number, y: number }
}
