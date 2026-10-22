<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import SceneBackdrop from './components/SceneBackdrop.vue'
import ExplorerHeader from './components/ExplorerHeader.vue'
import PlanetOrbit from './components/PlanetOrbit.vue'
import PlanetGrid from './components/PlanetGrid.vue'
import ExplorerControls from './components/ExplorerControls.vue'
import LandscapeStage from '../../components/scene/LandscapeStage.vue'
import { usePlanetOrbit } from '../../composables/usePlanetOrbit'
import { useSceneAssets } from '../../composables/useSceneAssets'
import SceneLoading from '../../components/scene/SceneLoading.vue'

const props = withDefaults(defineProps<{ active?: boolean; initialBook?: number }>(), {
  active: true,
  initialBook: 3,
})
const emit = defineEmits<{ enter: [bookId: number] }>()
const animationEnabled = ref(false)
const displayType = ref<'orbit' | 'grid'>('orbit')
const { selected, angle, visible, active, current, select, step } = usePlanetOrbit(
  animationEnabled,
  props.initialBook,
)
const { ready, error, progress, retry } = useSceneAssets()
watchEffect(() => {
  animationEnabled.value = ready.value && props.active && displayType.value === 'orbit'
})
</script>

<template>
  <LandscapeStage @keydown.left.prevent="step(-1)" @keydown.right.prevent="step(1)">
    <template v-if="ready">
      <SceneBackdrop />
      <ExplorerHeader :display-type="displayType" />
      <PlanetOrbit
        v-if="displayType === 'orbit'"
        :planets="visible"
        :selected="selected"
        :angle="angle"
        @select="select"
      />
      <PlanetGrid v-else :planets="visible" :selected="selected" @select="select" />
      <ExplorerControls
        :planets="visible"
        :selected="selected"
        :active-name="active.name"
        :display-type="displayType"
        @select="select"
        @toggle-display="displayType = displayType === 'orbit' ? 'grid' : 'orbit'"
        @explore="emit('enter', active.id)"
      />
    </template>
    <SceneLoading v-else :progress="progress" :error="error" @retry="retry" />
    <template #overlay>
      <span class="sr-only" aria-live="polite"
        >当前选择：{{ active.name }}，第 {{ current + 1 }} 颗星球</span
      >
    </template>
  </LandscapeStage>
</template>
