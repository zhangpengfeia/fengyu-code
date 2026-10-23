import type { BookDefinition, GameMode } from '../config/books'

export type GameSlot = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8

/** Shared game UI receives the owning book and current mode. */
export interface GameViewProps {
  book: BookDefinition
  mode?: GameMode
}
