import type { Component } from 'vue'
import Book01 from './Book01.vue'
import Book02 from './Book02.vue'
import Book03 from './Book03.vue'
import Book04 from './Book04.vue'
import Book05 from './Book05.vue'
import Book06 from './Book06.vue'
import Book07 from './Book07.vue'
import Book08 from './Book08.vue'
import Book09 from './Book09.vue'
import Book10 from './Book10.vue'
import Book11 from './Book11.vue'
import Book12 from './Book12.vue'

export const bookComponents: Record<number, Component> = {
  1: Book01,
  2: Book02,
  3: Book03,
  4: Book04,
  5: Book05,
  6: Book06,
  7: Book07,
  8: Book08,
  9: Book09,
  10: Book10,
  11: Book11,
  12: Book12,
}
