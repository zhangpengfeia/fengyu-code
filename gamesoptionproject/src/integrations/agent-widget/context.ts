import { readonly, ref } from 'vue'
export interface PlanetSelection {
  volume: number
  slot: number
  mode: 'training' | 'assessment'
}
const selection = ref<PlanetSelection>({ volume: 3, slot: 0, mode: 'assessment' })
export function usePlanetAgentSelection() {
  return readonly(selection)
}
export function updatePlanetAgentSelection(value: PlanetSelection) {
  if (
    value.volume !== selection.value.volume ||
    value.slot !== selection.value.slot ||
    value.mode !== selection.value.mode
  )
    selection.value = value
}
