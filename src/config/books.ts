import { planets } from '../assets'

export type GameMode = 'training' | 'assessment'
export interface BookGame {
  id: string
  title: string
  thumbnail?: string
  // Configure real game URLs separately for each mode when games are available.
  entries: Partial<Record<GameMode, string>>
}
export interface BookDefinition {
  id: number
  name: string
  planetImage: string
  landscape?: string
  thumbnail?: string
  theme: { background: string; accent: string; glow: string }
  games: BookGame[]
}

const slices = import.meta.glob<string>('../assets/catalog/*.webp', {
  eager: true,
  query: '?url',
  import: 'default',
})
const slice = (name: string) => slices[`../assets/catalog/${name}.webp`]!
export const catalogBackIcon = slice('back')
const colors = [
  ['#54231b', '#ffb188', '#ff7954'],
  ['#111e58', '#a8bdff', '#6c92ff'],
  ['#183e17', '#dcff85', '#70c84e'],
  ['#4e3210', '#ffe09a', '#ffc151'],
  ['#5e3307', '#ffec86', '#f0a32e'],
  ['#451e17', '#ffc29f', '#df6939'],
  ['#123452', '#b1e8ff', '#5bb7f0'],
  ['#123e39', '#acffe6', '#36c4b2'],
  ['#211c52', '#c2b7ff', '#8f80df'],
  ['#3a164c', '#ffc1ff', '#dd68dc'],
  ['#262050', '#bfffe8', '#94cfdf'],
  ['#1d2249', '#c6c4ff', '#8b90d0'],
]

// Every book owns eight independent game IDs, titles, thumbnails and mode URLs.
export const books: BookDefinition[] = planets.map((planet, index) => {
  const supplied = planet.id === 3 || planet.id === 5
  const prefix = `book-${String(planet.id).padStart(2, '0')}`
  const [background, accent, glow] = colors[index]!
  const thumbnail = supplied ? slice(`${prefix}-game`) : undefined
  return {
    id: planet.id,
    name: planet.name,
    planetImage: supplied ? slice(`${prefix}-planet`) : planet.image,
    landscape: supplied ? slice(`${prefix}-landscape`) : undefined,
    thumbnail,
    theme: { background: background!, accent: accent!, glow: glow! },
    games: Array.from({ length: 8 }, (_, gameIndex) => ({
      id: `${prefix}-game-${String(gameIndex + 1).padStart(2, '0')}`,
      title:
        supplied && gameIndex === 0 ? '抢石子' : `游戏 ${String(gameIndex + 1).padStart(2, '0')}`,
      thumbnail,
      entries: {},
    })),
  }
})
