<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { init, use, type EChartsType } from "echarts/core";
import { RadarChart } from "echarts/charts";
import { RadarComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import type { AbilityValue } from "../types";

use([RadarChart, RadarComponent, TooltipComponent, CanvasRenderer]);
const props = defineProps<{ values: AbilityValue[] }>();
const container = ref<HTMLDivElement>();
let chart: EChartsType | undefined;
let observer: ResizeObserver | undefined;

function render() {
  chart?.setOption({
    tooltip: { trigger: "item", confine: true },
    radar: {
      indicator: props.values.map((item) => ({ name: item.name, max: 100 })),
      radius: "64%",
      center: ["50%", "51%"],
      axisName: { color: "#7e8ba7", fontSize: 11 },
      splitNumber: 5,
      axisLine: { lineStyle: { color: "#dfe7fa" } },
      splitLine: { lineStyle: { color: "#e1e8f7" } },
      splitArea: { areaStyle: { color: ["#fff", "#f7f9ff"] } },
    },
    series: [
      {
        type: "radar",
        symbol: "circle",
        symbolSize: 5,
        lineStyle: { color: "#4a73ff", width: 2 },
        itemStyle: { color: "#4a73ff" },
        areaStyle: { color: "#79a1ff", opacity: 0.3 },
        data: [
          {
            value: props.values.map((item) => item.value),
            name: "示例能力分布",
          },
        ],
      },
    ],
  });
}
onMounted(() => {
  if (!container.value) return;
  chart = init(container.value);
  render();
  observer = new ResizeObserver(() => chart?.resize());
  observer.observe(container.value);
});
watch(() => props.values, render, { deep: true });
onBeforeUnmount(() => {
  observer?.disconnect();
  chart?.dispose();
});
</script>

<template>
  <div
    ref="container"
    class="radar-chart"
    role="img"
    :aria-label="`示例能力分布：${values.map((item) => `${item.name}${item.value}分`).join('，')}`"
  />
</template>

<style scoped>
.radar-chart {
  width: 100%;
  height: 265px;
  min-width: 0;
}
</style>
