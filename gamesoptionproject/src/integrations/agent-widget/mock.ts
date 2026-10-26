import type { MockGameContext } from '@fengyu/agent-widget-sdk'
import type { PlanetSelection } from './context'
export const mockGames = [
  { code: 'eight-pieces', name: '八子归位' },
  { code: 'arrow-maze', name: '箭头迷宫' },
  { code: 'number-maze', name: '数字迷宫' },
  { code: 'balloon', name: '打气球' },
  { code: 'stone-transfer', name: '石头搬运' },
  { code: 'addition', name: '加法游戏' },
  { code: 'fish', name: '鱼的游戏' },
  { code: 'four-seas', name: '纵横四海' },
] as const
/** Explicit mock samples, never used as authoritative game scores. */
export function createPlanetMockContext(selection: PlanetSelection, round = 0): MockGameContext {
  const game = mockGames[selection.slot % mockGames.length]!
  return {
    mode: 'mock',
    requestId: crypto.randomUUID(),
    student: { id: 'mock-student-001', name: '联调同学', schoolId: '11000' },
    game: {
      ...game,
      volume: selection.volume,
      mode: selection.mode === 'training' ? 'TRAINING' : 'ASSESSMENT',
      difficulty: 'NOVICE',
    },
    result: {
      durationMs: 83000 + round * 1000,
      actualSteps: 36 + selection.slot * 2 + round,
      optimalSteps: 28,
      badge: (['BRONZE', 'SILVER', 'GOLD'] as const)[round % 3]!,
      passed: true,
    },
  }
}
