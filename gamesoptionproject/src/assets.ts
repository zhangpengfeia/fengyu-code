/** Optimized derivatives; Vite adds content hashes for long-lived browser caching. */
const imagesByPath = import.meta.glob<string>('./assets/optimized/*.webp', {
  eager: true,
  query: '?url',
  import: 'default',
})

function imageUrl(name: string): string {
  const url = imagesByPath[`./assets/optimized/${name}.webp`]
  if (!url) throw new Error(`Missing optimized image: ${name}`)
  return url
}

export const artwork = {
  background: imageUrl('scene-1'),
  earth: imageUrl('scene-14'),
  title: imageUrl('scene-0'),
  profile: imageUrl('scene-10'),
}
const numerals = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十', '十一', '十二']
const images = [
  'planet-9',
  'planet-8',
  'scene-5',
  'planet-4',
  'planet-1',
  'planet-20',
  'planet-0',
  'planet-7',
  'planet-3',
  'planet-5',
  'planet-19',
  'planet-6',
]
export interface Planet {
  id: number
  name: string
  image: string
  visualScale: number
}

export const planets: Planet[] = numerals.map((numeral, index) => ({
  id: index + 1,
  name: `第${numeral}册`,
  image: imageUrl(images[index]!),
  // The forest slice includes a wide baked glow; normalize its visible globe diameter.
  visualScale: index === 2 ? 1.18 : 1,
}))
