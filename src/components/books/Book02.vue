<script setup lang="ts">
import BookGameCatalog from '../catalog/BookGameCatalog.vue'
import { books } from '../../config/books'

defineEmits<{ back: [] }>()
</script>

<template>
  <BookGameCatalog :book="books[1]!" @back="$emit('back')" />
</template>
