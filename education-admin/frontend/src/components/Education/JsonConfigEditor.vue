<template>
  <div class="json-config-editor">
    <el-input
      :model-value="modelValue"
      type="textarea"
      :rows="8"
      @update:model-value="$emit('update:modelValue', $event)"
    />
    <el-space
      ><el-button link type="primary" @click="format">校验并格式化</el-button
      ><span :class="valid ? 'valid' : 'invalid'">{{
        valid ? "JSON对象格式有效" : "请输入有效JSON对象"
      }}</span></el-space
    >
    <pre
      aria-label="JSON语法高亮预览"
    ><code><span v-for="(token,index) in tokens" :key="index" :class="token.type">{{token.text}}</span></code></pre>
  </div>
</template>
<script setup>
import { computed } from "vue";
const props = defineProps({ modelValue: { type: String, default: "{}" } }),
  emit = defineEmits(["update:modelValue"]);
const parsed = computed(() => {
  try {
    const value = JSON.parse(props.modelValue);
    return value && typeof value === "object" && !Array.isArray(value)
      ? value
      : null;
  } catch {
    return null;
  }
});
const valid = computed(() => parsed.value !== null);
function format() {
  if (valid.value)
    emit("update:modelValue", JSON.stringify(parsed.value, null, 2));
}
const tokens = computed(() => {
  const text = props.modelValue,
    output = [];
  const regex =
    /"(?:[^"\\]|\\.)*"(?=\s*:)|"(?:[^"\\]|\\.)*"|\b(?:true|false|null)\b|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g;
  let start = 0;
  for (const match of text.matchAll(regex)) {
    if (match.index > start)
      output.push({ text: text.slice(start, match.index), type: "" });
    const value = match[0];
    output.push({
      text: value,
      type: value.startsWith('"')
        ? /\s*:/.test(
            text.slice(
              match.index + value.length,
              match.index + value.length + 8,
            ),
          )
          ? "key"
          : "string"
        : /true|false|null/.test(value)
          ? "literal"
          : "number",
    });
    start = match.index + value.length;
  }
  output.push({ text: text.slice(start), type: "" });
  return output;
});
</script>
<style scoped>
.json-config-editor {
  width: 100%;
}
.json-config-editor :deep(textarea),
pre {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}
pre {
  background: #f6f8fa;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  white-space: pre-wrap;
  max-height: 240px;
  overflow: auto;
  color: #334155;
  line-height: 1.6;
}
.key {
  color: #8b36a8;
}
.string {
  color: #167047;
}
.number {
  color: #165eb6;
}
.literal {
  color: #b35211;
}
.valid {
  color: #16803a;
}
.invalid {
  color: #c53a3a;
}
</style>
