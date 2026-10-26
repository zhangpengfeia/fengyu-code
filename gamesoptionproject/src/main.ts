import { createApp } from 'vue'
import App from './App.vue'
import './styles/global.less'
async function start() {
  const root = document.getElementById('app')!
  root.textContent = '正在验证教学资源权限…'
  try {
    // A development-only escape hatch never appears in production builds.
    if (
      import.meta.env.DEV &&
      (import.meta.env.VITE_STANDALONE_DEV === 'true' || import.meta.env.MODE === 'mock')
    ) {
      createApp(App).mount(root)
      return
    }
    const response = await fetch('/play/planet/session', {
      credentials: 'same-origin',
      cache: 'no-store',
    })
    if (!response.ok || !(await response.json()).authorized) throw new Error('Forbidden')
    createApp(App).mount(root)
  } catch {
    root.replaceChildren()
    const heading = document.createElement('h1')
    heading.textContent = '没有权限访问该游戏'
    const message = document.createElement('p')
    message.textContent = '请登录教育平台，通过已授权的游戏菜单进入。'
    root.append(heading, message)
    Object.assign(root.style, {
      padding: '15vh 24px',
      textAlign: 'center',
      color: '#fff',
      fontFamily: 'Arial, sans-serif',
    })
  }
}
void start()
