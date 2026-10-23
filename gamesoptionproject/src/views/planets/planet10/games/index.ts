import { defineAsyncComponent, type Component } from 'vue'
import type { GameSlot } from '../../../../types/game'

export const gameViews: Record<GameSlot, Component> = {
  1: defineAsyncComponent(() => import('./game01/index.vue')),
  2: defineAsyncComponent(() => import('./game02/index.vue')),
  3: defineAsyncComponent(() => import('./game03/index.vue')),
  4: defineAsyncComponent(() => import('./game04/index.vue')),
  5: defineAsyncComponent(() => import('./game05/index.vue')),
  6: defineAsyncComponent(() => import('./game06/index.vue')),
  7: defineAsyncComponent(() => import('./game07/index.vue')),
  8: defineAsyncComponent(() => import('./game08/index.vue')),
}
