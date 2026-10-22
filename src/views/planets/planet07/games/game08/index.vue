<script setup lang="ts">
import GamePlaceholder from '../../../../../components/game/GamePlaceholder.vue'
import { books, type GameMode } from '../../../../../config/books'

defineProps<{ mode?: GameMode }>()
defineEmits<{ back: [] }>()
</script>

<template>
  <GamePlaceholder :book="books[6]!" :game-index="7" :mode="mode" @back="$emit('back')" />
</template>
