export const WIDGET_PROTOCOL = 'fengyu.agent.widget.v1' as const
export interface MockGameContext {
  mode: 'mock'
  requestId: string
  student: { id: string; name: string; schoolId: string }
  game: { code: string; name: string; volume: number; mode: 'ASSESSMENT' | 'TRAINING'; difficulty: 'NOVICE' | 'SKILLED' | 'MASTER' }
  result: { durationMs: number; actualSteps: number; optimalSteps: number | null; badge: 'NONE' | 'BRONZE' | 'SILVER' | 'GOLD'; passed: boolean }
}
export type WidgetMessage = {
  protocol: typeof WIDGET_PROTOCOL
  channelId: string
  type: 'ready' | 'context' | 'accepted' | 'error' | 'close'
  context?: MockGameContext
  message?: string
  requestId?: string
  evaluationId?: string
}
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const record = (value: unknown): value is Record<string, unknown> => !!value && typeof value === 'object' && !Array.isArray(value)
export function isWidgetMessage(value: unknown, channelId: string): value is WidgetMessage {
  return record(value) && value.protocol === WIDGET_PROTOCOL && value.channelId === channelId && ['ready', 'context', 'accepted', 'error', 'close'].includes(String(value.type))
}
export function isMockGameContext(value: unknown): value is MockGameContext {
  if (!record(value) || value.mode !== 'mock' || !uuid.test(String(value.requestId))) return false
  const { student, game, result } = value
  if (!record(student) || !record(game) || !record(result)) return false
  const text = (v: unknown, max: number) => typeof v === 'string' && v.trim().length > 0 && v.length <= max
  const integer = (v: unknown, max: number) => typeof v === 'number' && Number.isInteger(v) && v >= 0 && v <= max
  return text(student.id, 100) && text(student.name, 100) && text(student.schoolId, 100)
    && typeof game.code === 'string' && /^[a-z][a-z0-9-]{0,63}$/.test(game.code) && text(game.name, 100)
    && integer(game.volume, 100) && game.volume >= 1
    && ['ASSESSMENT', 'TRAINING'].includes(String(game.mode)) && ['NOVICE', 'SKILLED', 'MASTER'].includes(String(game.difficulty))
    && integer(result.durationMs, 86400000) && integer(result.actualSteps, 1000000)
    && (result.optimalSteps === null || (integer(result.optimalSteps, 1000000) && result.optimalSteps > 0))
    && typeof result.passed === 'boolean' && ['NONE', 'BRONZE', 'SILVER', 'GOLD'].includes(String(result.badge))
    && (result.passed || result.badge === 'NONE')
}
export function widgetOrigin(url: string, base: string): string {
  const target = new URL(url, base)
  if (!['http:', 'https:'].includes(target.protocol) || target.username || target.password) throw new Error('学伴地址必须是有效的网页地址。')
  return target.origin
}
