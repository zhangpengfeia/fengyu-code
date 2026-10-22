<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import PlanetExplorer from './PlanetExplorer.vue'
import { bookComponents } from './books'

function readBook() {
  const match = /^#book\/(\d+)$/.exec(window.location.hash)
  const id = match ? Number(match[1]) : null
  return id && id >= 1 && id <= 12 ? id : null
}
const currentBook = ref<number | null>(readBook())
const initialBook = currentBook.value ?? 3
function syncLocation() {
  currentBook.value = readBook()
}
function enter(bookId: number) {
  window.history.pushState({ bookCatalog: true }, '', `#book/${bookId}`)
  currentBook.value = bookId
}
function back() {
  if (window.history.state?.bookCatalog) window.history.back()
  else {
    window.history.replaceState(null, '', window.location.pathname + window.location.search)
    currentBook.value = null
  }
}
window.addEventListener('popstate', syncLocation)
window.addEventListener('hashchange', syncLocation)
onBeforeUnmount(() => {
  window.removeEventListener('popstate', syncLocation)
  window.removeEventListener('hashchange', syncLocation)
})
</script>

<template>
  <PlanetExplorer
    v-show="currentBook === null"
    :active="currentBook === null"
    :initial-book="initialBook"
    @enter="enter"
  />
  <component v-if="currentBook" :is="bookComponents[currentBook]" :key="currentBook" @back="back" />
</template>
