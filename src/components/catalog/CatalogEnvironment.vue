<script setup lang="ts">
import type { BookDefinition } from '../../config/books'

defineProps<{ book: BookDefinition }>()
</script>

<template>
  <div class="catalog-environment" aria-hidden="true">
    <img v-if="book.landscape" :src="book.landscape" alt="" decoding="async" />
  </div>
</template>

<style scoped lang="less">
.catalog-environment {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: var(--book-background);

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
    object-position: center;
    // The exported artwork already carries its intended translucent background.
  }
}
</style>
