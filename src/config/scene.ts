/** All geometry uses the same 1200 × 675 design coordinates. */
export const SCENE = {
  width: 1200,
  height: 675,
  earth: { left: 280, top: 32, width: 680 },
  // Wide, tilted ellipses follow the reference composition, including off-stage arcs.
  orbits: [
    { centerX: 600, centerY: 300, radiusX: 445.835, radiusY: 120.0325, tilt: -16, planetSize: 190 },
    {
      centerX: 600,
      centerY: 300,
      radiusX: 531.5725,
      radiusY: 154.3275,
      tilt: -16,
      planetSize: 190,
    },
    { centerX: 600, centerY: 330, radiusX: 617.31, radiusY: 188.6225, tilt: -16, planetSize: 190 },
  ],
} as const

// All twelve books share one scene: four evenly spaced planets on each track.
export const PLANET_SLOTS = [
  { track: 0, phase: -1.15 },
  { track: 0, phase: Math.PI - 1.15 },
  { track: 1, phase: 0 },
  { track: 1, phase: Math.PI },
  { track: 2, phase: 0.55 },
  { track: 2, phase: Math.PI + 0.55 },
  { track: 0, phase: Math.PI / 2 - 1.15 },
  { track: 0, phase: Math.PI * 1.5 - 1.15 },
  { track: 1, phase: Math.PI / 2 },
  { track: 1, phase: Math.PI * 1.5 },
  { track: 2, phase: Math.PI / 2 + 0.55 },
  { track: 2, phase: Math.PI * 1.5 + 0.55 },
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

/** Enlarge near the front center; selecting a distant planet does not enlarge it there. */
export function planetScale(depth: number, selected: boolean) {
  const center = Math.max(0, depth * 2 - 1) ** 8
  return 0.42 + depth * 0.26 + center * (selected ? 0.72 : 0.2)
}
