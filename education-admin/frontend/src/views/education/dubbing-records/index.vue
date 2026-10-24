<template>
  <div class="app-container">
    <h2>学生配音记录</h2>
    <el-form inline @submit.prevent="search">
      <el-form-item label="班级"
        ><el-select v-model="query.class_id" clearable style="width: 180px"
          ><el-option
            v-for="c in classes"
            :key="c.class_id"
            :value="String(c.class_id)"
            :label="c.class_name" /></el-select
      ></el-form-item>
      <el-form-item label="册次"
        ><el-input-number v-model="query.volume" :min="1" :max="12"
      /></el-form-item>
      <el-form-item label="类型"
        ><el-select v-model="query.dubbing_type" clearable style="width: 150px"
          ><el-option value="1" label="课件配音" /><el-option
            value="2"
            label="测评配音" /></el-select
      ></el-form-item>
      <el-button type="primary" @click="search">查询</el-button
      ><el-button v-hasPermi="['biz:dubbing:export']" @click="exportRecords"
        >导出Excel</el-button
      >
    </el-form>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="student_id" label="学生ID" /><el-table-column
        prop="title"
        label="素材"
      /><el-table-column prop="volume" label="册次" />
      <el-table-column label="类型"
        ><template #default="{ row }">{{
          row.dubbing_type === "1" ? "课件配音" : "测评配音"
        }}</template></el-table-column
      >
      <el-table-column label="得分"
        ><template #default="{ row }">{{
          row.dubbing_type === "2"
            ? `${row.star_num}星`
            : `${row.total_score}分`
        }}</template></el-table-column
      >
      <el-table-column
        prop="answer_time"
        label="答题时间"
        min-width="170"
      /><el-table-column label="详情"
        ><template #default="{ row }"
          ><el-button link @click="detail = row">查看</el-button></template
        ></el-table-column
      >
    </el-table>
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="query.page"
      v-model:limit="query.size"
      @pagination="load"
    />
    <el-dialog
      :model-value="!!detail"
      :title="detail?.dubbing_type === '2' ? '测评配音详情' : '课件配音详情'"
      @close="detail = null"
    >
      <template v-if="detail"
        ><el-descriptions :column="2" border
          ><el-descriptions-item
            v-for="(label, key) in dimensions"
            :key="key"
            :label="label"
            >{{ detail[key] }}</el-descriptions-item
          ></el-descriptions
        ><audio
          :src="detail.audio_url"
          controls
          style="margin-top: 20px; width: 100%"
      /></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { learningGet } from "@/api/learning";
import { options } from "@/api/education";
const proxy = getCurrentInstance().proxy;
function exportRecords() {
  proxy.download("/biz/learning/records/export", { ...query }, "配音记录.xlsx");
}
const query = reactive({
  page: 1,
  size: 20,
  class_id: "",
  volume: undefined,
  dubbing_type: "",
});
const rows = ref([]),
  total = ref(0),
  loading = ref(false),
  classes = ref([]),
  detail = ref(null);
const dimensions = {
  accuracy_score: "准确度 / 20",
  fluency_score: "流畅度 / 20",
  completeness_score: "完整性 / 15",
  phonology_score: "声韵 / 15",
  intonation_score: "语调 / 15",
  overall_score: "整体印象 / 15",
  total_score: "总分 / 100",
};
async function load() {
  loading.value = true;
  try {
    const r = await learningGet("records", query);
    rows.value = r.rows;
    total.value = Number(r.total);
  } finally {
    loading.value = false;
  }
}
function search() {
  query.page = 1;
  load();
}
onMounted(async () => {
  classes.value = (await options()).data.classes;
  await load();
});
</script>
