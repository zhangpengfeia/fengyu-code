<template>
  <main class="universe">
    <div class="landscape-viewport">
      <div class="scene"><slot /></div>
    </div>
    <slot name="overlay" />
  </main>
</template>

<style scoped lang="less">
@import (reference) '../../styles/variables.less';

.universe {
  --viewport-height: 100svh;
  --available-width: calc(100vw - env(safe-area-inset-left, 0px) - env(safe-area-inset-right, 0px));
  --available-height: calc(
    var(--viewport-height) - env(safe-area-inset-top, 0px) - env(safe-area-inset-bottom, 0px)
  );
  position: relative;
  width: 100%;
  height: var(--viewport-height);
  overflow: clip;
  background: radial-gradient(ellipse at 50% 45%, #1a3267, #09152f);
}

@supports (height: 100dvh) {
  .universe {
    --viewport-height: 100dvh;
  }
}

.landscape-viewport {
  --landscape-height: var(--available-height);
  position: absolute;
  left: env(safe-area-inset-left, 0px);
  top: env(safe-area-inset-top, 0px);
  width: var(--available-width);
  height: var(--available-height);
  display: grid;
  place-items: center;
}

.scene {
  width: min(100%, calc(var(--landscape-height) * 1.777778));
  aspect-ratio: 16 / 9;
  position: relative;
  overflow: clip;
  isolation: isolate;
  container-type: inline-size;
  background: @scene-background;
}

// Rotate the content, so landscape also works in portrait-locked mobile WebViews.
@media @mobile-portrait {
  .landscape-viewport {
    --landscape-height: var(--available-width);
    left: calc(env(safe-area-inset-left, 0px) + var(--available-width) / 2);
    top: calc(env(safe-area-inset-top, 0px) + var(--available-height) / 2);
    width: var(--available-height);
    height: var(--available-width);
    transform: translate(-50%, -50%) rotate(90deg);
  }
}
</style>
