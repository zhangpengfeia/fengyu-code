<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { createAgentWidget } from '@fengyu/agent-widget-sdk'
import { usePlanetAgentSelection } from './context'
import { createPlanetMockContext } from './mock'
import { createPlanetLiveContext } from './live'
const container = ref<HTMLElement>()
const selection = usePlanetAgentSelection()
const mock = import.meta.env.DEV && import.meta.env.MODE === 'mock'
const enabled = mock || import.meta.env.PROD
function currentContext() {
  return mock
    ? createPlanetMockContext(selection.value, round)
    : createPlanetLiveContext(selection.value)
}
let widget: ReturnType<typeof createAgentWidget> | undefined
let round = 0
function publish() {
  widget?.updateContext(currentContext())
}
onMounted(() => {
  if (!enabled || !container.value) return
  widget = createAgentWidget({
    container: container.value,
    url:
      import.meta.env.VITE_AGENT_WIDGET_URL ||
      (mock ? 'http://127.0.0.1:5176/widget' : '/student/widget'),
    context: currentContext(),
    onMockRound: mock
      ? () => {
          round++
          publish()
        }
      : undefined,
  })
})
watch(selection, () => {
  round = 0
  publish()
})
onBeforeUnmount(() => widget?.destroy())
</script>
<template><div v-if="enabled" ref="container" class="agent-viewport" /></template>
<style scoped lang="less">
@import (reference) '../../styles/variables.less';
.agent-viewport {
  --widget-height: 100svh;
  --available-width: calc(100vw - env(safe-area-inset-left, 0px) - env(safe-area-inset-right, 0px));
  --available-height: calc(
    var(--widget-height) - env(safe-area-inset-top, 0px) - env(safe-area-inset-bottom, 0px)
  );
  position: fixed;
  left: env(safe-area-inset-left, 0px);
  top: env(safe-area-inset-top, 0px);
  width: var(--available-width);
  height: var(--available-height);
  z-index: 200;
  pointer-events: none;
}
@supports (height: 100dvh) {
  .agent-viewport {
    --widget-height: 100dvh;
  }
}
@media @mobile-portrait {
  .agent-viewport {
    left: calc(env(safe-area-inset-left, 0px) + var(--available-width) / 2);
    top: calc(env(safe-area-inset-top, 0px) + var(--available-height) / 2);
    width: var(--available-height);
    height: var(--available-width);
    transform: translate(-50%, -50%) rotate(90deg);
  }
}
</style>
