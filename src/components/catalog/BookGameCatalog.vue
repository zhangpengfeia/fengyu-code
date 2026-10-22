<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, type CSSProperties } from 'vue'
import { artwork } from '../../assets'
import { catalogBackIcon, type BookDefinition, type GameMode } from '../../config/books'
import { loadGroup } from '../../composables/useSceneAssets'
import LandscapeStage from '../LandscapeStage.vue'
import SceneLoading from '../SceneLoading.vue'
import CatalogBackdrop from './CatalogBackdrop.vue'
import GameCard from './GameCard.vue'
import GameModeSwitch from './GameModeSwitch.vue'

const props = defineProps<{ book: BookDefinition }>()
const emit = defineEmits<{ back: [] }>()
const selected = ref(0)
const mode = ref<GameMode>('training')
const layout = ref<'orbit' | 'grid'>('orbit')
const ready = ref(false)
const error = ref(false)
const progress = ref(0)
const message = ref('')
const active = computed(() => props.book.games[selected.value]!)
const theme = computed(() => ({
  '--book-background': props.book.theme.background,
  '--book-accent': props.book.theme.accent,
  '--book-glow': props.book.theme.glow,
}))
const slots = [
  { x: 50, y: 70, width: 26, z: 25 },
  { x: 78, y: 59, width: 21, z: 22 },
  { x: 90, y: 37, width: 17, z: 20 },
  { x: 92, y: 17, width: 12, z: 18 },
  { x: 79, y: 9, width: 8, z: 16 },
  { x: 24, y: 25, width: 10, z: 15 },
  { x: 12, y: 43, width: 13, z: 19 },
  { x: 17, y: 68, width: 20, z: 23 },
]
let disposed = false
let request = 0
async function prepare() {
  const generation = ++request
  ready.value = false
  error.value = false
  progress.value = 0
  const urls = [
    ...new Set(
      [
        artwork.profile,
        catalogBackIcon,
        props.book.planetImage,
        props.book.landscape,
        ...props.book.games.map((game) => game.thumbnail),
      ].filter((url): url is string => Boolean(url)),
    ),
  ]
  let loaded = 0
  try {
    await loadGroup(urls, 'high', () => {
      if (!disposed && generation === request)
        progress.value = Math.round((++loaded / urls.length) * 100)
    })
    if (!disposed && generation === request) ready.value = true
  } catch {
    if (!disposed && generation === request) error.value = true
  }
}
function select(index: number) {
  selected.value = index
  message.value = ''
}
function step(direction: number) {
  select((selected.value + direction + props.book.games.length) % props.book.games.length)
}
function cardStyle(index: number): CSSProperties | undefined {
  if (layout.value === 'grid') return undefined
  const slot = slots[(index - selected.value + props.book.games.length) % props.book.games.length]!
  return { left: `${slot.x}%`, top: `${slot.y}%`, width: `${slot.width}%`, zIndex: slot.z }
}
function launch() {
  const entry = active.value.entries[mode.value]
  if (!entry) {
    message.value = `${active.value.title}的${mode.value === 'training' ? '训练' : '测评'}内容正在准备中`
    return
  }
  window.location.assign(entry)
}
onMounted(prepare)
onBeforeUnmount(() => {
  disposed = true
  request++
})
</script>

<template>
  <LandscapeStage
    class="book-catalog"
    :style="theme"
    :data-book-id="book.id"
    @keydown.left.prevent="step(-1)"
    @keydown.right.prevent="step(1)"
    @keydown.esc="emit('back')"
  >
    <template v-if="ready">
      <CatalogBackdrop :book="book" />
      <img class="catalog-profile" :src="artwork.profile" alt="杨同学，大师" />
      <h1 class="book-badge">{{ book.name }}</h1>
      <h2 class="game-title">{{ active.title }}</h2>
      <div class="game-list" :class="layout" role="group" :aria-label="`${book.name}的8个游戏`">
        <GameCard
          v-for="(game, index) in book.games"
          :key="game.id"
          :game="game"
          :index="index"
          :selected="selected === index"
          :style="cardStyle(index)"
          @select="select(index)"
        />
      </div>
      <GameModeSwitch v-model="mode" @update:model-value="message = ''" />
      <button class="start-game" @click="launch">
        {{ mode === 'training' ? '开始训练' : '开始测评' }} <span>›</span>
      </button>
      <button class="catalog-back" aria-label="返回星球目录" @click="emit('back')">
        <img :src="catalogBackIcon" alt="" /><span>返回星球</span>
      </button>
      <div class="catalog-tools">
        <button aria-label="上一个游戏" @click="step(-1)">‹</button
        ><span>{{ String(selected + 1).padStart(2, '0') }} / 08</span
        ><button aria-label="下一个游戏" @click="step(1)">›</button
        ><button class="layout-toggle" @click="layout = layout === 'orbit' ? 'grid' : 'orbit'">
          {{ layout === 'orbit' ? '▦ 全部游戏' : '↔ 轨道展示' }}
        </button>
      </div>
      <p v-if="message" class="catalog-message" role="status">{{ message }}</p>
    </template>
    <template v-else
      ><SceneLoading :progress="progress" :error="error" @retry="prepare" /><button
        class="loading-back"
        @click="emit('back')"
      >
        返回星球目录
      </button></template
    >
  </LandscapeStage>
</template>

<style scoped lang="less">
.book-catalog {
  background: var(--book-background);
}
.catalog-profile {
  position: absolute;
  left: 2%;
  top: 4%;
  width: 18%;
  z-index: 30;
}
.book-badge {
  position: absolute;
  left: 50%;
  top: 13%;
  transform: translateX(-50%);
  margin: 0;
  border: 2px solid #ffffd8;
  border-radius: 14px;
  color: white;
  background: var(--book-glow);
  box-shadow: 0 0 0 4px #ffffff16;
  padding: 0.5cqw 2.7cqw;
  font-size: 2.6cqw;
  font-weight: 600;
  z-index: 30;
}
.game-title {
  position: absolute;
  top: 29%;
  left: 25%;
  width: 50%;
  text-align: center;
  margin: 0;
  font-size: 7cqw;
  line-height: 1.15;
  color: #fffdf1;
  text-shadow: 0 4px 5px #2a160b99;
  z-index: 14;
}
.game-list {
  position: absolute;
  inset: 0;
}
.game-list.grid {
  inset: 43% 10% 18%;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1cqw;
  z-index: 25;
  :deep(.game-card) {
    position: relative;
    left: auto;
    top: auto;
    width: 100%;
    transform: none;
    height: 100%;
    min-height: 0;
    aspect-ratio: auto;
  }
}
.catalog-back {
  position: absolute;
  left: 3%;
  bottom: 8%;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 0.6cqw;
  z-index: 35;
  font-size: clamp(10px, 1.2cqw, 16px);
  img {
    width: 4cqw;
    min-width: 24px;
  }
}
.catalog-tools {
  position: absolute;
  right: 3%;
  bottom: 3%;
  display: flex;
  align-items: center;
  gap: 0.5cqw;
  z-index: 35;
  font-size: clamp(10px, 1.2cqw, 16px);
  button {
    background: transparent;
    font-size: 2.6cqw;
    padding: 0 0.4cqw;
    min-height: 28px;
  }
  .layout-toggle {
    margin-left: 0.5cqw;
    font-size: clamp(10px, 1.15cqw, 16px);
  }
}
.start-game {
  position: absolute;
  bottom: 1.5%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 35;
  padding: 0.35cqw 2cqw;
  background: var(--book-accent);
  color: var(--book-background);
  font-size: clamp(11px, 1.3cqw, 18px);
  border-radius: 24px;
  font-weight: 700;
  span {
    margin-left: 1cqw;
  }
}
.catalog-message {
  position: absolute;
  top: 23%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 40;
  background: #07172be6;
  padding: 0.5cqw 1cqw;
  border-radius: 12px;
  font-size: clamp(11px, 1.25cqw, 16px);
  white-space: nowrap;
}
.loading-back {
  position: absolute;
  bottom: 12%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 55;
  padding: 8px 20px;
  border-radius: 20px;
  color: #1e3552;
  background: #e8eda7;
}
</style>
