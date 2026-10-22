import { computed, onBeforeUnmount, onMounted, ref, type Ref } from 'vue'
import { planets } from '../assets'
import { PLANET_SLOTS } from '../config/scene'

const ORBIT_SPEED = 0.045

/** Owns selection and animation for all twelve planets. */
export function usePlanetOrbit(animationEnabled: Ref<boolean>, initialBook = 3) {
  const selected = ref(initialBook)
  let focusing = false
  const angle = ref(-PLANET_SLOTS[initialBook - 1]!.phase)
  const visible = planets
  const active = computed(() => planets.find((planet) => planet.id === selected.value)!)
  const current = computed(() => visible.findIndex((planet) => planet.id === selected.value))

  let targetAngle = angle.value
  let media: MediaQueryList | undefined
  let frame = 0
  let previous = 0

  function select(id: number) {
    const planetIndex = planets.findIndex((planet) => planet.id === id)
    if (planetIndex === -1) return

    selected.value = id
    const desired = -PLANET_SLOTS[planetIndex]!.phase
    // Always take the shorter arc, including the last-to-first book transition.
    targetAngle =
      angle.value + Math.atan2(Math.sin(desired - angle.value), Math.cos(desired - angle.value))
    focusing = true
    if (media?.matches) angle.value = targetAngle
  }

  function step(direction: number) {
    select(((selected.value - 1 + direction + planets.length) % planets.length) + 1)
  }

  function animate(now: number) {
    const delta = previous ? Math.min((now - previous) / 1000, 0.05) : 0
    previous = now

    if (animationEnabled.value && !media?.matches) {
      if (focusing) {
        angle.value += (targetAngle - angle.value) * (1 - Math.exp(-delta * 5))
        if (Math.abs(targetAngle - angle.value) < 0.001) {
          angle.value = targetAngle
          focusing = false
        }
      } else {
        angle.value += delta * ORBIT_SPEED
      }
    }
    frame = requestAnimationFrame(animate)
  }

  function onMotionChange() {
    if (media?.matches) {
      if (focusing) angle.value = targetAngle
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
    angle,
    visible,
    active,
    current,
    select,
    step,
  }
}
