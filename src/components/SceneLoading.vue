<script setup lang="ts">
defineProps<{ progress: number; error: boolean }>()
defineEmits<{ retry: [] }>()
</script>

<template>
  <div class="scene-loading" role="status" aria-live="polite">
    <p>{{ error ? '图片加载失败，请检查网络后重试' : '正在准备进入星球...' }}</p>
    <template v-if="!error">
      <progress :value="progress" max="100" aria-label="图片加载进度" />
      <span>{{ progress }}%</span>
    </template>
    <button v-else @click="$emit('retry')">重新加载</button>
  </div>
</template>

<style scoped lang="less">
.scene-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #122454;
  color: #d3e4ff;
  z-index: 50;

  p {
    margin: 0;
    font-size: clamp(14px, 2cqw, 20px);
  }
  progress {
    width: min(240px, 50%);
    height: 8px;
    accent-color: #a8e294;
  }
  span {
    font-size: 14px;
  }
  button {
    padding: 10px 24px;
    border-radius: 24px;
    background: #e8eda7;
    color: #1e3552;
  }
}
</style>
