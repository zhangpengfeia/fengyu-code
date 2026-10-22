<script setup lang="ts">
import BookGameCatalog from '../catalog/BookGameCatalog.vue'
import { books } from '../../config/books'

defineEmits<{ back: [] }>()
</script>

<template>
  <BookGameCatalog :book="books[0]!" @back="$emit('back')" />
</template>
