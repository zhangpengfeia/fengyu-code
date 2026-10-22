import type { Component } from 'vue'
import Planet01 from './planet01/index.vue'
import Planet02 from './planet02/index.vue'
import Planet03 from './planet03/index.vue'
import Planet04 from './planet04/index.vue'
import Planet05 from './planet05/index.vue'
import Planet06 from './planet06/index.vue'
import Planet07 from './planet07/index.vue'
import Planet08 from './planet08/index.vue'
import Planet09 from './planet09/index.vue'
import Planet10 from './planet10/index.vue'
import Planet11 from './planet11/index.vue'
import Planet12 from './planet12/index.vue'

export const planetViews: Record<number, Component> = {
  1: Planet01,
  2: Planet02,
  3: Planet03,
  4: Planet04,
  5: Planet05,
  6: Planet06,
  7: Planet07,
  8: Planet08,
  9: Planet09,
  10: Planet10,
  11: Planet11,
  12: Planet12,
}
