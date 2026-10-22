/** All geometry uses the same 1200 × 675 design coordinates. */
export const SCENE = {
  width: 1200,
  height: 675,
  earth: { left: 300, top: 56, width: 600 },
  // Separate ellipses spread the foreground arcs out like the reference artwork.
  orbits: [
    { centerX: 600, centerY: 310, radiusX: 340, radiusY: 85, tilt: -14, planetSize: 220 },
    { centerX: 600, centerY: 330, radiusX: 385, radiusY: 110, tilt: -14, planetSize: 210 },
    { centerX: 600, centerY: 350, radiusX: 430, radiusY: 125, tilt: -14, planetSize: 190 },
  ],
} as const

// Each page has two planets on each track, separated by half a revolution.
export const PLANET_SLOTS = [
  { track: 1, phase: -1.15 },
  { track: 1, phase: Math.PI - 1.15 },
  { track: 2, phase: 0 },
  { track: 2, phase: Math.PI },
  { track: 0, phase: 1.05 },
  { track: 0, phase: Math.PI + 1.05 },
] as const

export function orbitPoint(angle: number, track = 0) {
  const { centerX, centerY, radiusX, radiusY, tilt } = SCENE.orbits[track]!
  const radians = (tilt * Math.PI) / 180
  const x = Math.sin(angle) * radiusX
  const y = Math.cos(angle) * radiusY
  return {
    x: centerX + x * Math.cos(radians) - y * Math.sin(radians),
    y: centerY + x * Math.sin(radians) + y * Math.cos(radians),
    depth: (Math.cos(angle) + 1) / 2,
    behindEarth: Math.cos(angle) < 0,
  }
}

export function planetPosition(index: number, angle: number) {
  const slot = PLANET_SLOTS[index]!
  return {
    ...orbitPoint(angle + slot.phase, slot.track),
    track: slot.track,
    size: SCENE.orbits[slot.track]!.planetSize,
  }
}

export function orbitArc(start: number, end: number, track = 0) {
  return Array.from({ length: 145 }, (_, index) => {
    const point = orbitPoint(start + ((end - start) * index) / 144, track)
    return `${index === 0 ? 'M' : 'L'}${point.x.toFixed(3)},${point.y.toFixed(3)}`
  }).join(' ')
}
