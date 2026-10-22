import { computed, onBeforeUnmount, onMounted, ref, watch, type Ref } from 'vue'
import { artwork, planets } from '../assets'

// Keep decoded images alive across page changes; failed requests can be retried.
const decoded = new Map<string, Promise<HTMLImageElement>>()

function decodeImage(url: string, priority: 'high' | 'low') {
  const cached = decoded.get(url)
  if (cached) return cached
  const promise = new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image()
    image.decoding = 'async'
    image.fetchPriority = priority
    const timeout = window.setTimeout(() => finish(new Error('Image load timed out')), 20_000)
    function finish(error?: unknown) {
      window.clearTimeout(timeout)
      image.onload = image.onerror = null
      if (error) reject(error)
      else resolve(image)
    }
    image.onerror = () => finish(new Error('Image load failed'))
    image.onload = () => {
      if (typeof image.decode === 'function') image.decode().then(() => finish(), finish)
      else finish()
    }
    image.src = url
  })
  decoded.set(url, promise)
  void promise.catch(() => {
    if (decoded.get(url) === promise) decoded.delete(url)
  })
  return promise
}

async function loadGroup(urls: string[], priority: 'high' | 'low', onLoaded = () => {}) {
  const queue = [...urls]
  // Bound network/decode concurrency rather than starting every image at once.
  await Promise.all(
    Array.from({ length: Math.min(priority === 'high' ? 3 : 2, queue.length) }, async () => {
      while (queue.length) {
        await decodeImage(queue.shift()!, priority)
        onLoaded()
      }
    }),
  )
}

export function useSceneAssets(page: Ref<number>) {
  const loading = ref(true)
  const error = ref(false)
  const completed = ref(0)
  const total = ref(10)
  const progress = computed(() => Math.round((completed.value / total.value) * 100))
  const ready = computed(() => !loading.value && !error.value)
  let generation = 0

  async function load() {
    const request = ++generation
    loading.value = true
    error.value = false
    completed.value = 0
    const pageImages = planets.slice(page.value * 6, page.value * 6 + 6).map((p) => p.image)
    const urls = [...Object.values(artwork), ...pageImages]
    total.value = urls.length
    try {
      await loadGroup(urls, 'high', () => {
        if (request === generation) completed.value++
      })
      if (request !== generation) return
      loading.value = false
      // Prefetch only after the visible scene is ready. Errors retry on navigation.
      void loadGroup(
        planets.filter((p) => !pageImages.includes(p.image)).map((p) => p.image),
        'low',
      ).catch(() => {})
    } catch {
      if (request !== generation) return
      loading.value = false
      error.value = true
    }
  }

  onMounted(() => watch(page, load, { immediate: true, flush: 'sync' }))
  onBeforeUnmount(() => generation++)
  return { loading, error, progress, ready, retry: load }
}
