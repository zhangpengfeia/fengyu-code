import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { planets } from '../assets'

const PAGE_SIZE = 6
const ANGLE_STEP = (Math.PI * 2) / PAGE_SIZE
const ORBIT_SPEED = 0.045

/** Owns selection, paging and animation; UI components only receive state/events. */
export function usePlanetOrbit() {
  const selected = ref(3)
  const page = ref(0)
  const playing = ref(true)
  const focused = ref(false)
  const angle = ref(-2 * ANGLE_STEP)
  const visible = computed(() =>
    planets.slice(page.value * PAGE_SIZE, (page.value + 1) * PAGE_SIZE),
  )
  const active = computed(() => planets.find((planet) => planet.id === selected.value)!)
  const current = computed(() => visible.value.findIndex((planet) => planet.id === selected.value))

  let targetAngle = angle.value
  let media: MediaQueryList | undefined
  let frame = 0
  let previous = 0

  function select(id: number) {
    const planetIndex = planets.findIndex((planet) => planet.id === id)
    if (planetIndex === -1) return

    page.value = Math.floor(planetIndex / PAGE_SIZE)
    selected.value = id
    const desired = -(planetIndex % PAGE_SIZE) * ANGLE_STEP
    // Always move along the shorter arc, including when crossing between pages.
    targetAngle =
      angle.value + Math.atan2(Math.sin(desired - angle.value), Math.cos(desired - angle.value))
    focused.value = true
    if (media?.matches) angle.value = targetAngle
  }

  function switchPage() {
    const nextPage = (page.value + 1) % Math.ceil(planets.length / PAGE_SIZE)
    select(nextPage * PAGE_SIZE + 3)
  }

  function step(direction: number) {
    select(((selected.value - 1 + direction + planets.length) % planets.length) + 1)
  }

  function toggleMotion() {
    playing.value = !playing.value
    if (playing.value) focused.value = false
  }

  function animate(now: number) {
    const delta = previous ? Math.min((now - previous) / 1000, 0.05) : 0
    previous = now

    if (focused.value && Math.abs(targetAngle - angle.value) > 0.0001) {
      angle.value += (targetAngle - angle.value) * (1 - Math.exp(-delta * 5))
    } else if (!focused.value && playing.value && !media?.matches) {
      angle.value += delta * ORBIT_SPEED
    }
    frame = requestAnimationFrame(animate)
  }

  function onMotionChange() {
    if (media?.matches) {
      playing.value = false
      if (focused.value) angle.value = targetAngle
    }
  }

  onMounted(() => {
    media = window.matchMedia('(prefers-reduced-motion: reduce)')
    onMotionChange()
    media.addEventListener('change', onMotionChange)
    frame = requestAnimationFrame(animate)
  })

  onBeforeUnmount(() => {
    cancelAnimationFrame(frame)
    media?.removeEventListener('change', onMotionChange)
  })

  return {
    selected,
    page,
    playing,
    angle,
    visible,
    active,
    current,
    select,
    switchPage,
    step,
    toggleMotion,
  }
}
