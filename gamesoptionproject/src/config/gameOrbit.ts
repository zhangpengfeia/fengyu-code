const slots = [
  { x: 50, y: 70, width: 26, z: 25 },
  { x: 78, y: 59, width: 21, z: 22 },
  { x: 90, y: 37, width: 17, z: 20 },
  { x: 92, y: 17, width: 12, z: 18 },
  { x: 79, y: 9, width: 8, z: 16 },
  { x: 24, y: 25, width: 10, z: 15 },
  { x: 12, y: 43, width: 13, z: 19 },
  { x: 17, y: 68, width: 20, z: 23 },
]

export function wrapGameIndex(index: number, count: number) {
  return ((index % count) + count) % count
}

/** Interpolate between the eight perspective slots while dragging, including wraparound. */
export function gameOrbitPosition(position: number) {
  const wrapped = wrapGameIndex(position, slots.length)
  const index = Math.floor(wrapped)
  const from = slots[index]!
  const to = slots[(index + 1) % slots.length]!
  const progress = wrapped - index
  return {
    x: from.x + (to.x - from.x) * progress,
    y: from.y + (to.y - from.y) * progress,
    width: from.width + (to.width - from.width) * progress,
    z: from.z + (to.z - from.z) * progress,
  }
}
