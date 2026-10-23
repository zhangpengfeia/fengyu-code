import { onBeforeUnmount, onMounted, ref } from 'vue'

/** Pointer gestures use scene coordinates, including the mobile 90° rotation. */
export function useOrbitDrag(callbacks: {
  start: () => void
  move: (radians: number) => void
  end: () => void
}) {
  const dragging = ref(false)
  let gesture: {
    id: number
    surface: HTMLElement
    origin: number
    previous: number
    width: number
    rotated: boolean
  } | null = null
  let suppressClickUntil = 0

  function pointerDown(event: PointerEvent) {
    if (gesture || !event.isPrimary || event.button !== 0) return
    const surface = event.currentTarget as HTMLElement
    const rotated = getComputedStyle(surface).getPropertyValue('--scene-rotated').trim() === '1'
    const bounds = surface.getBoundingClientRect()
    const position = rotated ? event.clientY : event.clientX
    gesture = {
      id: event.pointerId,
      surface,
      origin: position,
      previous: position,
      width: Math.max(1, rotated ? bounds.height : bounds.width),
      rotated,
    }
  }

  function pointerMove(event: PointerEvent) {
    if (!gesture || event.pointerId !== gesture.id) return
    const position = gesture.rotated ? event.clientY : event.clientX
    if (!dragging.value) {
      if (Math.abs(position - gesture.origin) < 8) return
      dragging.value = true
      gesture.surface.setPointerCapture(event.pointerId)
      callbacks.start()
    }
    event.preventDefault()
    callbacks.move(((position - gesture.previous) / gesture.width) * Math.PI * 2)
    gesture.previous = position
  }

  function finish() {
    const previous = gesture
    gesture = null
    if (dragging.value) {
      dragging.value = false
      suppressClickUntil = Date.now() + 400
      callbacks.end()
    }
    if (previous?.surface.hasPointerCapture(previous.id))
      previous.surface.releasePointerCapture(previous.id)
  }

  function pointerEnd(event: PointerEvent) {
    if (event.pointerId === gesture?.id) finish()
  }

  function clickCapture(event: MouseEvent) {
    if (event.detail > 0 && Date.now() < suppressClickUntil) {
      event.preventDefault()
      event.stopPropagation()
      suppressClickUntil = 0
    }
  }

  onMounted(() => {
    window.addEventListener('pointerup', pointerEnd)
    window.addEventListener('pointercancel', pointerEnd)
    window.addEventListener('resize', finish)
    window.addEventListener('blur', finish)
  })
  onBeforeUnmount(() => {
    finish()
    window.removeEventListener('pointerup', pointerEnd)
    window.removeEventListener('pointercancel', pointerEnd)
    window.removeEventListener('resize', finish)
    window.removeEventListener('blur', finish)
  })

  return { dragging, pointerDown, pointerMove, pointerEnd, clickCapture }
}
