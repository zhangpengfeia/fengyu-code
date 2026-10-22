<script setup lang="ts">
import type { Planet } from '../../../assets'

defineProps<{
  planets: Planet[]
  selected: number
  activeName: string
  displayType: 'orbit' | 'grid'
}>()
defineEmits<{ select: [id: number]; toggleDisplay: []; explore: [] }>()
</script>

<template>
  <button class="explore" @click="$emit('explore')">探索{{ activeName }} <span>›</span></button>
  <footer class="bottom-bar">
    <span class="hint">
      <span class="mouse-icon">☝</span> 点击星球放大 <span class="hint-divider">/</span>
      {{ displayType === 'orbit' ? '左右滑动旋转' : '← → 切换' }}
    </span>
    <nav aria-label="快捷选择星球">
      <button
        v-for="planet in planets"
        :key="planet.id"
        :class="{ active: selected === planet.id }"
        :aria-label="`切换至${planet.name}`"
        :aria-current="selected === planet.id ? 'true' : undefined"
        @click="$emit('select', planet.id)"
      >
        {{ String(planet.id).padStart(2, '0') }}
      </button>
    </nav>
    <button
      class="switch-display"
      aria-label="切换类型展示"
      :aria-pressed="displayType === 'grid'"
      :title="displayType === 'orbit' ? '切换为网格展示' : '切换为轨道展示'"
      @click="$emit('toggleDisplay')"
    >
      <span aria-hidden="true">⇄</span>切换类型展示
    </button>
  </footer>
</template>

<style scoped lang="less">
@import (reference) '../../../styles/variables.less';

.explore {
  position: absolute;
  left: 50%;
  bottom: 8.6%;
  transform: translateX(-50%);
  z-index: 39;
  background: linear-gradient(#438d28, #17530b);
  border: 2px solid #fffde8;
  border-radius: 40px;
  padding: 0.55cqw 2cqw;
  font-size: 1.8cqw;
  font-weight: 800;
  white-space: nowrap;
  box-shadow:
    0 3px 0 #12340d,
    0 0 16px #c4ff8944;
  transition: filter 0.2s;

  &:hover {
    filter: brightness(1.2);
  }
  span {
    padding-left: 8px;
  }
}

.bottom-bar {
  position: absolute;
  bottom: 2.5%;
  left: 3%;
  right: 3%;
  display: grid;
  grid-template-columns: 24% minmax(0, 1fr) 20%;
  gap: 0.5cqw;
  align-items: center;
  z-index: 40;
}

.hint {
  color: #aabfdf;
  font-size: 0.95cqw;
  display: flex;
  align-items: center;
  gap: 8px;

  .mouse-icon {
    font-size: 1.4cqw;
    color: #e1edff;
  }
  &-divider {
    margin: 0 5px;
    opacity: 0.4;
  }
}

nav {
  display: flex;
  gap: 0.35cqw;
  justify-content: center;

  button {
    .round-control(2.65cqw);
    flex-shrink: 0;
    border: 1px solid #718ec04d;
    background: #132854;
    color: #a9bfe2;
    font-size: 1cqw;
    transition:
      background 0.25s,
      color 0.25s;

    &:hover {
      background: #315384;
    }
    &.active {
      background: #f4e8a2;
      color: #243451;
      border-color: #fff2b2;
      box-shadow: 0 0 14px #fff5bb26;
    }
  }
}

.switch-display {
  display: flex;
  justify-self: end;
  align-items: center;
  gap: 0.6cqw;
  background: transparent;
  font-size: 1cqw;
  color: #dfeaff;
  white-space: nowrap;

  span {
    font-size: 3cqw;
    line-height: 1;
  }
}

@media @compact-scene {
  .explore {
    bottom: 14%;
    min-height: 32px;
    padding: 3px 16px;
    font-size: clamp(13px, 2cqw, 18px);
  }
  .bottom-bar {
    bottom: 3.5%;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 1cqw;
    height: 32px;
  }
  .hint {
    display: none;
  }
  nav {
    gap: 0.4cqw;

    button {
      .round-control(clamp(18px, 2.8cqw, 28px));
      font-size: clamp(9px, 1.25cqw, 12px);
    }
  }
  .switch-display {
    min-height: 32px;
    gap: 0.4cqw;
    font-size: clamp(10px, 1.4cqw, 12px);
    span {
      font-size: clamp(21px, 3cqw, 28px);
    }
  }
}
</style>
