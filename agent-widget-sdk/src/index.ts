import {
  WIDGET_PROTOCOL,
  isMockGameContext,
  isWidgetMessage,
  widgetOrigin,
  type MockGameContext,
  type WidgetMessage,
} from './protocol'
export type { MockGameContext } from './protocol'
export interface WidgetOptions {
  container: HTMLElement
  url: string
  context: MockGameContext
  onMockRound?: () => void
  onAccepted?: (event: { requestId?: string; evaluationId?: string }) => void
}
/** Browser-only SDK. The host owns game data; the iframe owns chat and authentication. */
export function createAgentWidget(options: WidgetOptions) {
  let context = options.context
  if (!isMockGameContext(context)) throw new Error('星球传入的模拟上下文无效。')
  const origin = widgetOrigin(options.url, window.location.href)
  const channelId = crypto.randomUUID()
  const url = new URL(options.url, window.location.href)
  url.searchParams.set('parent_origin', window.location.origin)
  url.searchParams.set('channel_id', channelId)
  const root = document.createElement('div')
  root.style.cssText = 'position:absolute;inset:0;pointer-events:none;z-index:100'
  const shadow = root.attachShadow({ mode: 'open' })
  shadow.innerHTML = `<style>
    *{box-sizing:border-box}button{font:inherit;cursor:pointer}button:focus-visible{outline:3px solid #8fa7ff;outline-offset:3px}
    .launcher{position:absolute;right:18px;bottom:18px;border:1px solid #dce5ff;background:#fff;border-radius:16px;padding:10px 16px;color:#305bff;box-shadow:0 5px 22px #15285c33;pointer-events:auto;display:flex;align-items:center;gap:9px;font:600 13px system-ui,sans-serif}.symbol{display:grid;place-items:center;width:30px;height:30px;border-radius:11px;background:#e9f7ff;color:#1ca8b3;font-size:22px}
    .panel{position:absolute;right:18px;bottom:18px;width:min(460px,calc(100% - 36px));height:min(620px,calc(100% - 36px));border:1px solid #dce5ff;background:#fff;border-radius:18px;overflow:hidden;box-shadow:0 10px 50px #10244c44;pointer-events:auto;display:flex;flex-direction:column;color:#24355a;font:13px system-ui,sans-serif}.panel[hidden],.launcher[hidden]{display:none}.header{display:flex;align-items:center;gap:10px;padding:11px 14px;background:#eef3ff;flex-shrink:0}.header strong{font-size:14px}.tag{font-size:10px;color:#a57622;background:#fff7e8;padding:4px 6px;border-radius:5px}.close{margin-left:auto;border:0;color:#64749c;background:transparent;font-size:18px;padding:4px 7px}.status{padding:7px 14px;color:#7482a0;font-size:10px;border-bottom:1px solid #edf0f8;flex-shrink:0}.frame{width:100%;flex:1;min-height:0;border:0;background:#fafbff}.round{border:0;border-top:1px solid #edf0f8;background:#fff;color:#305bff;font-size:11px;min-height:36px;flex-shrink:0}
    @media(max-height:460px){.panel{right:10px;bottom:10px;height:calc(100% - 20px);width:min(460px,calc(100% - 20px))}.header{padding:7px 12px}.launcher{right:12px;bottom:12px;padding:7px 12px}.status{padding:4px 12px}}
  </style><button class="launcher" type="button" aria-label="打开智能学伴"><span class="symbol" aria-hidden="true">✦</span>智能学伴</button><section class="panel" hidden aria-label="星球智能学伴"><header class="header"><strong>智能学伴</strong><span class="tag">模拟联调</span><button class="close" type="button" aria-label="关闭智能学伴">×</button></header><div class="status" role="status">正在等待学伴连接…</div><iframe class="frame" title="星球智能学伴应用" referrerpolicy="strict-origin-when-cross-origin" sandbox="allow-scripts allow-same-origin allow-forms"></iframe></section>`
  const launcher = shadow.querySelector<HTMLButtonElement>('.launcher')!
  const panel = shadow.querySelector<HTMLElement>('.panel')!
  const status = shadow.querySelector<HTMLElement>('.status')!
  const frame = shadow.querySelector<HTMLIFrameElement>('iframe')!
  // Keep the browsing context in light DOM for browser accessibility/automation;
  // the named slot retains isolated host controls and the panel's flex layout.
  const frameSlot = document.createElement('slot')
  frameSlot.name = 'agent-frame'
  frame.replaceWith(frameSlot)
  frame.slot = 'agent-frame'
  frame.style.cssText = 'width:100%;flex:1;min-height:0;border:0;background:#fafbff'
  root.append(frame)
  let loaded = false
  let ready = false
  let disposed = false
  function post() {
    if (ready && !disposed)
      frame.contentWindow?.postMessage(
        { protocol: WIDGET_PROTOCOL, channelId, type: 'context', context } satisfies WidgetMessage,
        origin,
      )
  }
  function close() {
    panel.hidden = true
    launcher.hidden = false
    launcher.focus()
  }
  function open() {
    panel.hidden = false
    launcher.hidden = true
    if (!loaded) {
      loaded = true
      frame.src = url.href
    } else post()
    shadow.querySelector<HTMLButtonElement>('.close')!.focus()
  }
  function receive(event: MessageEvent) {
    if (
      event.origin !== origin ||
      event.source !== frame.contentWindow ||
      !isWidgetMessage(event.data, channelId)
    )
      return
    const message = event.data
    if (message.type === 'ready') {
      ready = true
      post()
    }
    if (message.type === 'accepted' && message.requestId === context.requestId) {
      status.textContent = `已接收星球数据：第${context.game.volume}册 · ${context.game.name}`
      options.onAccepted?.({ requestId: message.requestId, evaluationId: message.evaluationId })
    }
    if (message.type === 'error')
      status.textContent = message.message || '学伴暂时无法连接，请关闭后重新打开。'
    if (message.type === 'close') close()
  }
  launcher.addEventListener('click', open)
  shadow.querySelector('.close')!.addEventListener('click', close)
  if (options.onMockRound) {
    const round = document.createElement('button')
    round.type = 'button'
    round.className = 'round'
    round.textContent = '发送新一局模拟成绩'
    round.addEventListener('click', options.onMockRound)
    panel.append(round)
  }
  window.addEventListener('message', receive)
  options.container.append(root)
  return {
    open,
    close,
    updateContext(value: MockGameContext) {
      if (!isMockGameContext(value)) throw new Error('星球传入的模拟上下文无效。')
      context = value
      status.textContent = '正在传入星球上下文…'
      post()
    },
    destroy() {
      disposed = true
      window.removeEventListener('message', receive)
      frame.src = 'about:blank'
      root.remove()
    },
  }
}
