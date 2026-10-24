<template>
  <div class="app-container">
    <h2>{{ isExam ? "测评抽题配置" : "配音素材库" }}</h2>
    <el-form inline @submit.prevent="search">
      <el-form-item label="册次"
        ><el-input-number v-model="query.volume" :min="1" :max="12" clearable
      /></el-form-item>
      <el-form-item v-if="!isExam" label="标题"
        ><el-input v-model="query.keyword" clearable
      /></el-form-item>
      <el-form-item
        ><el-button type="primary" @click="search">查询</el-button
        ><el-button @click="reset">重置</el-button></el-form-item
      >
      <el-form-item v-if="!isExam"
        ><el-button v-hasPermi="['biz:dubbing:edit']" @click="edit()"
          >新增素材</el-button
        ></el-form-item
      >
    </el-form>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="volume" label="册次" width="80" />
      <template v-if="isExam">
        <el-table-column label="抽题范围"
          ><template #default="{ row }">{{
            scopes[row.extract_scope]
          }}</template></el-table-column
        >
        <el-table-column prop="extract_num" label="抽题数量" />
        <el-table-column label="抽题方式"
          ><template #default="{ row }">{{
            modes[row.extract_mode]
          }}</template></el-table-column
        >
      </template>
      <template v-else>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="用途"
          ><template #default="{ row }">{{
            row.material_tag
              .split(",")
              .map((t) => tags[t])
              .join("、")
          }}</template></el-table-column
        >
      </template>
      <el-table-column label="状态"
        ><template #default="{ row }">{{
          row.status === "0" ? "启用" : "停用"
        }}</template></el-table-column
      >
      <el-table-column label="操作"
        ><template #default="{ row }"
          ><el-button
            v-hasPermi="['biz:dubbing:edit']"
            link
            type="primary"
            @click="edit(row)"
            >编辑</el-button
          ><el-button
            v-if="!isExam"
            v-hasPermi="['biz:dubbing:edit']"
            link
            type="danger"
            @click="remove(row)"
            >删除</el-button
          ></template
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
      v-model="dialog"
      :title="isExam ? '测评配置' : '配音素材'"
      width="680px"
      destroy-on-close
    >
      <el-form label-width="100px" @submit.prevent="save">
        <el-form-item label="册次"
          ><el-input-number
            v-model="form.volume"
            :min="1"
            :max="12"
            :disabled="isExam"
        /></el-form-item>
        <template v-if="isExam">
          <el-form-item label="抽题范围"
            ><el-select v-model="form.extract_scope"
              ><el-option
                v-for="(label, value) in scopes"
                :key="value"
                :value="value"
                :label="label" /></el-select
          ></el-form-item>
          <el-form-item label="抽题数量"
            ><el-input-number v-model="form.extract_num" :min="1" :max="100"
          /></el-form-item>
          <el-form-item label="抽题方式"
            ><el-select v-model="form.extract_mode"
              ><el-option
                v-for="(label, value) in modes"
                :key="value"
                :value="value"
                :label="label" /></el-select
          ></el-form-item>
          <el-form-item label="评分规则">与课件一致</el-form-item>
        </template>
        <template v-else>
          <el-form-item label="标题" required
            ><el-input v-model="form.title" maxlength="128"
          /></el-form-item>
          <el-form-item label="用途标签" required
            ><el-checkbox-group v-model="selectedTags"
              ><el-checkbox
                v-for="(label, value) in tags"
                :key="value"
                :value="value"
                >{{ label }}</el-checkbox
              ></el-checkbox-group
            ></el-form-item
          >
          <el-form-item label="视频地址" required
            ><el-input
              v-model="form.video_url"
              placeholder="HTTPS或上传文件路径"
          /></el-form-item>
          <el-form-item label="上传视频"
            ><file-upload
              v-model="form.video_url"
              :limit="1"
              :file-type="['mp4', 'webm']"
              :file-size="100"
          /></el-form-item>
          <el-form-item label="素材文本" required
            ><el-input
              v-model="form.material_text"
              type="textarea"
              :rows="6"
              maxlength="20000"
          /></el-form-item>
        </template>
        <el-form-item label="状态"
          ><el-radio-group v-model="form.status"
            ><el-radio value="0">启用</el-radio
            ><el-radio value="1">停用</el-radio></el-radio-group
          ></el-form-item
        >
      </el-form>
      <template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save"
          >保存</el-button
        ></template
      >
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { learningGet, learningSave, learningDelete } from "@/api/learning";
const props = defineProps({ kind: { type: String, required: true } });
const isExam = computed(() => props.kind === "exam-config");
const tags = { "01": "课件配套", "02": "测评专用", "03": "通用" };
const scopes = { 1: "仅课件配套", 2: "仅测评专用", 3: "全部" };
const modes = { 1: "完全随机", 2: "排除已做随机" };
const query = reactive({ page: 1, size: 20, volume: undefined, keyword: "" });
const rows = ref([]),
  total = ref(0),
  loading = ref(false),
  saving = ref(false),
  dialog = ref(false),
  form = ref({}),
  selectedTags = ref([]);
async function load() {
  loading.value = true;
  try {
    const r = await learningGet(`dubbing/${props.kind}`, query);
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
function reset() {
  Object.assign(query, { page: 1, volume: undefined, keyword: "" });
  load();
}
function edit(row) {
  form.value = row
    ? { ...row }
    : {
        volume: 1,
        status: "0",
        material_text: "",
        material_tag: "01",
        score_rule: "1",
      };
  selectedTags.value = (form.value.material_tag || "").split(",");
  dialog.value = true;
}
async function save() {
  saving.value = true;
  try {
    const data = { ...form.value };
    if (!isExam.value) data.material_tag = selectedTags.value.join(",");
    await learningSave(props.kind, data.material_id || data.config_id, data);
    dialog.value = false;
    ElMessage.success("保存成功");
    await load();
  } finally {
    saving.value = false;
  }
}
async function remove(row) {
  await ElMessageBox.confirm(
    "确认删除该素材？历史配音记录将保留。",
    "删除素材",
  );
  await learningDelete(row.material_id);
  await load();
}
onMounted(load);
</script>
