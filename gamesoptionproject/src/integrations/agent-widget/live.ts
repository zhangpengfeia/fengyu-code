import type { LiveGameContext } from '@fengyu/agent-widget-sdk'
import { books } from '../../config/books'
import type { PlanetSelection } from './context'
/** Use the visible catalog; do not map placeholder games to unrelated Agent rules. */
export function createPlanetLiveContext(selection: PlanetSelection): LiveGameContext {
  const book = books.find((item) => item.id === selection.volume)
  const game = book?.games[selection.slot]
  return {
    mode: 'live',
    requestId: crypto.randomUUID(),
    game: {
      ...(game?.agentCode ? { code: game.agentCode } : {}),
      name: game?.title || '星球学习',
      volume: selection.volume,
      mode: selection.mode === 'training' ? 'TRAINING' : 'ASSESSMENT',
      difficulty: 'NOVICE',
    },
  }
}
