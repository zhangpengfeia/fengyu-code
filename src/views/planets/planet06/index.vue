<script setup lang="ts">
import BookGameCatalog from '../../../components/catalog/BookGameCatalog.vue'
import { books } from '../../../config/books'

defineEmits<{ back: [] }>()
</script>

<template>
  <BookGameCatalog :book="books[5]!" @back="$emit('back')" />
</template>
