/** All geometry uses the same 1200 × 675 design coordinates. */
export const SCENE = {
  width: 1200,
  height: 675,
  // Preserve the asset's aspect ratio and leave room above and below the globe.
  earth: { left: 280, top: 32, width: 680 },
  planetSize: 300,
  orbit: { centerX: 570, centerY: 320, radiusX: 540, radiusY: 180, tilt: -14 },
} as const

export function orbitPoint(angle: number, radiusScale = 1) {
  const { centerX, centerY, radiusX, radiusY, tilt } = SCENE.orbit
  const radians = (tilt * Math.PI) / 180
  const x = Math.sin(angle) * radiusX * radiusScale
  const y = Math.cos(angle) * radiusY * radiusScale
  return {
    x: centerX + x * Math.cos(radians) - y * Math.sin(radians),
    y: centerY + x * Math.sin(radians) + y * Math.cos(radians),
    depth: (Math.cos(angle) + 1) / 2,
    behindEarth: Math.cos(angle) < 0,
  }
}

export function orbitArc(start: number, end: number, radiusScale = 1) {
  return Array.from({ length: 73 }, (_, index) => {
    const point = orbitPoint(start + ((end - start) * index) / 72, radiusScale)
    return `${index === 0 ? 'M' : 'L'}${point.x.toFixed(3)},${point.y.toFixed(3)}`
  }).join(' ')
}
