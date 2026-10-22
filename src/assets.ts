/** PNGs rendered from the supplied PDF slices. See public/assets/manifest.json for sources. */
export const artwork = {
  background: '/assets/scene-1.png',
  earth: '/assets/scene-14.png',
  title: '/assets/scene-0.png',
  profile: '/assets/scene-10.png',
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
}

export const planets: Planet[] = numerals.map((numeral, index) => ({
  id: index + 1,
  name: `第${numeral}册`,
  image: `/assets/${images[index]}.png`,
}))
