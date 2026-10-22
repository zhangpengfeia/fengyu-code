<script setup lang="ts">
import { ref } from 'vue'
import SceneBackdrop from './SceneBackdrop.vue'
import ExplorerHeader from './ExplorerHeader.vue'
import PlanetOrbit from './PlanetOrbit.vue'
import ExplorerControls from './ExplorerControls.vue'
import ExploreDialog from './ExploreDialog.vue'
import { usePlanetOrbit } from '../composables/usePlanetOrbit'

const { selected, page, playing, angle, visible, active, current, select, switchPage, step, toggleMotion } = usePlanetOrbit()
const exploreDialog = ref<InstanceType<typeof ExploreDialog>>()
</script>

<template>
  <main class="universe" @keydown.left.prevent="step(-1)" @keydown.right.prevent="step(1)">
    <div class="scene">
      <SceneBackdrop :playing="playing" />
      <ExplorerHeader :page="page" :playing="playing" @toggle-motion="toggleMotion" />
      <PlanetOrbit :planets="visible" :selected="selected" :angle="angle" @select="select" />
      <ExplorerControls
        :planets="visible"
        :selected="selected"
        :active-name="active.name"
        @select="select"
        @switch-page="switchPage"
        @explore="exploreDialog?.open()"
      />
    </div>
    <ExploreDialog ref="exploreDialog" :planet="active" />
    <span class="sr-only" aria-live="polite">当前选择：{{ active.name }}，第 {{ current + 1 }} 颗星球</span>
  </main>
</template>

<style scoped lang="less">
@import (reference) '../styles/variables.less';

.universe {
  --scene-height: calc(100svh - env(safe-area-inset-top, 0px) - env(safe-area-inset-bottom, 0px));
  min-height: 100svh;
  display: grid;
  place-items: center;
  overflow: clip;
  padding: env(safe-area-inset-top, 0px) env(safe-area-inset-right, 0px) env(safe-area-inset-bottom, 0px) env(safe-area-inset-left, 0px);
  background: radial-gradient(ellipse at 50% 45%, #1a3267, #09152f);
}

.scene {
  width: min(100%, calc(var(--scene-height) * 1.777778));
  aspect-ratio: 16 / 9;
  position: relative;
  overflow: clip;
  isolation: isolate;
  container-type: inline-size;
  background: @scene-background;
}

@media @mobile-portrait {
  .scene {
    width: min(100%, calc(var(--scene-height) * .8));
    aspect-ratio: 4 / 5;
  }
}

// Landscape uses the full available height; no forced screen rotation or page reload.
@media @mobile-landscape {
  .universe { width: 100%; }
}
</style>
