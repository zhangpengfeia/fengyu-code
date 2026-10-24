<template>
  <div class="app-container">
    <h2>激活码批次管理</h2>
    <el-button
      v-hasPermi="['biz:activation:edit']"
      type="primary"
      @click="dialog = true"
      >生成激活码</el-button
    >
    <el-table v-loading="loading" :data="rows" style="margin-top: 20px">
      <el-table-column prop="batch_id" label="批次ID" /><el-table-column
        prop="batch_name"
        label="批次名称"
      /><el-table-column label="权限类型"
        ><template #default="{ row }">{{
          types[row.permission_type]
        }}</template></el-table-column
      ><el-table-column prop="total_num" label="数量" /><el-table-column
        prop="used_num"
        label="已使用"
      /><el-table-column prop="valid_days" label="权限天数" /><el-table-column
        prop="expire_time"
        label="批次到期"
        min-width="180"
      /><el-table-column label="操作"
        ><template #default="{ row }"
          ><el-button
            v-hasPermi="['biz:activation:export']"
            link
            type="primary"
            @click="exportCodes(row)"
            >导出Excel</el-button
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
    <el-dialog v-model="dialog" title="生成激活码" width="650px">
      <el-form label-width="120px">
        <el-form-item label="批次名称" required
          ><el-input v-model="form.batch_name" maxlength="128"
        /></el-form-item>
        <el-form-item label="权限类型"
          ><el-select v-model="form.permission_type"
            ><el-option
              v-for="(label, value) in types"
              :key="value"
              :value="value"
              :label="label" /></el-select
        ></el-form-item>
        <el-form-item v-if="form.permission_type === '2'" label="权限范围JSON"
          ><el-input
            v-model="form.permission_range"
            type="textarea"
            :rows="5"
          />
          <p>填写volumes、subjects、stages数组。</p></el-form-item
        >
        <el-form-item label="生成数量"
          ><el-input-number v-model="form.total_num" :min="1" :max="10000"
        /></el-form-item>
        <el-form-item label="权限天数"
          ><el-input-number v-model="form.valid_days" :min="1" :max="3650"
        /></el-form-item>
        <el-form-item label="批次到期" required
          ><el-date-picker v-model="form.expire_time" type="datetime"
        /></el-form-item>
        <el-form-item label="绑定学校ID"
          ><el-input
            v-model="form.school_id"
            placeholder="选填，须先开启学校绑定参数"
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button :loading="saving" type="primary" @click="save"
          >生成</el-button
        ></template
      >
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { ElMessage } from "element-plus";
import { learningGet, learningPost } from "@/api/learning";
const { proxy } = getCurrentInstance();
const types = { 1: "学期型", 2: "指定范围型", 3: "全量型" };
const rows = ref([]),
  total = ref(0),
  loading = ref(false),
  saving = ref(false),
  dialog = ref(false);
const query = reactive({ page: 1, size: 20 });
const form = reactive({
  batch_name: "",
  permission_type: "1",
  permission_range: '{"volumes":[1],"subjects":["MATH"],"stages":["ANY"]}',
  total_num: 100,
  valid_days: 180,
  expire_time: new Date(Date.now() + 180 * 86400000),
  school_id: "",
});
async function load() {
  loading.value = true;
  try {
    const r = await learningGet("activation/batches", query);
    rows.value = r.rows;
    total.value = Number(r.total);
  } finally {
    loading.value = false;
  }
}
async function save() {
  if (!form.expire_time) {
    ElMessage.error("请选择批次到期时间");
    return;
  }
  saving.value = true;
  try {
    JSON.parse(form.permission_range);
    await learningPost("activation/batches", {
      ...form,
      school_id: form.school_id || null,
      expire_time: new Date(form.expire_time).toISOString(),
    });
    dialog.value = false;
    ElMessage.success("生成成功");
    await load();
  } catch (e) {
    if (e instanceof SyntaxError) ElMessage.error("权限范围JSON格式无效");
  } finally {
    saving.value = false;
  }
}
function exportCodes(row) {
  proxy.download(
    `/biz/learning/activation/batches/${row.batch_id}/export`,
    {},
    `激活码_${row.batch_id}.xlsx`,
  );
}
onMounted(load);
</script>
