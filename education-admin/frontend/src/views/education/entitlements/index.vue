<template>
  <div class="app-container education-page">
    <div class="edu-page-heading">
      <div>
        <span class="edu-eyebrow">资源授权</span>
        <h1>学生授权</h1>
        <p>学校资源池与学生个人授权共同决定学习范围，撤销后立即限制访问。</p>
      </div>
      <el-button
        v-hasPermi="['biz:entitlement:grant']"
        type="primary"
        icon="Plus"
        :disabled="!studentId"
        @click="open"
        >发放授权</el-button
      >
    </div>
    <el-card shadow="never" class="edu-card"
      ><el-form inline @submit.prevent="load"
        ><el-form-item label="学生"
          ><el-select
            v-model="studentId"
            filterable
            remote
            :remote-method="searchStudents"
            :loading="searching"
            placeholder="输入学生姓名或账号"
            style="width: 300px"
            @change="load"
            ><el-option
              v-for="s in students"
              :key="s.user_id"
              :value="String(s.user_id)"
              :label="`${s.nick_name} · ${s.student_no || s.user_name}`" /></el-select></el-form-item
        ><el-form-item
          ><el-button
            type="primary"
            icon="Search"
            :disabled="!studentId"
            @click="load"
            >查询</el-button
          ></el-form-item
        ></el-form
      >
      <el-table
        :data="rows"
        v-loading="loading"
        stripe
        empty-text="请选择学生查看授权记录"
        ><el-table-column
          prop="entitlement_id"
          label="授权ID"
          width="100"
        /><el-table-column label="授权范围" min-width="160"
          ><template #default="{ row }">{{
            row.scopes
              ?.map((s) =>
                s.game_id
                  ? `游戏 #${s.game_id}`
                  : s.courseware_id
                    ? `课件 #${s.courseware_id}`
                    : "范围授权",
              )
              .join("、")
          }}</template></el-table-column
        ><el-table-column label="来源" width="130"
          ><template #default="{ row }">{{
            row.source_type === "MANUAL" ? "管理员发放" : row.source_type
          }}</template></el-table-column
        ><el-table-column label="有效期" min-width="290"
          ><template #default="{ row }"
            >{{ time(row.start_time) }} 至 {{ time(row.end_time) }}</template
          ></el-table-column
        ><el-table-column label="状态" width="100"
          ><template #default="{ row }"
            ><el-tag
              :type="
                row.status === 'REVOKED'
                  ? 'danger'
                  : new Date(row.end_time) <= new Date()
                    ? 'info'
                    : 'success'
              "
              >{{
                row.status === "REVOKED"
                  ? "已撤销"
                  : new Date(row.end_time) <= new Date()
                    ? "已过期"
                    : "有效"
              }}</el-tag
            ></template
          ></el-table-column
        ><el-table-column
          prop="remark"
          label="发放 / 撤销说明"
          min-width="180"
        /><el-table-column label="操作" width="100"
          ><template #default="{ row }"
            ><el-button
              v-if="row.status === 'ACTIVE'"
              v-hasPermi="['biz:entitlement:grant']"
              link
              type="danger"
              @click="revokeOne(row)"
              >撤销</el-button
            ></template
          ></el-table-column
        ></el-table
      ></el-card
    >
    <el-dialog v-model="dialog" title="发放学习授权" width="600px"
      ><el-form ref="formRef" :model="form" label-width="110px"
        ><el-form-item label="授权类型"
          ><el-radio-group v-model="form.scope_type"
            ><el-radio value="COURSEWARE">课件</el-radio
            ><el-radio value="GAME">独立游戏</el-radio></el-radio-group
          ></el-form-item
        ><el-form-item label="指定资源" required
          ><el-select
            v-if="form.scope_type === 'COURSEWARE'"
            v-model="form.courseware_id"
            filterable
            remote
            :remote-method="searchCourseware"
            placeholder="搜索已发布课件"
            style="width: 100%"
            ><el-option
              v-for="r in courseware"
              :key="r.courseware_id"
              :value="String(r.courseware_id)"
              :label="r.courseware_name" /></el-select
          ><el-select
            v-else
            v-model="form.game_id"
            filterable
            style="width: 100%"
            ><el-option
              v-for="r in games"
              :key="r.game_id"
              :value="String(r.game_id)"
              :label="r.game_name" /></el-select></el-form-item
        ><el-form-item label="开始时间" required
          ><el-date-picker
            v-model="form.start_time"
            type="datetime" /></el-form-item
        ><el-form-item label="截止时间" required
          ><el-date-picker
            v-model="form.end_time"
            type="datetime" /></el-form-item
        ><el-form-item label="发放原因" required
          ><el-input
            v-model="form.remark"
            type="textarea"
            maxlength="500" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save"
          >确认发放</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  listUsers,
  listGrants,
  grant,
  revoke,
  listCatalog,
} from "@/api/education";
const studentId = ref(""),
  students = ref([]),
  rows = ref([]),
  loading = ref(false),
  searching = ref(false),
  dialog = ref(false),
  form = ref({}),
  saving = ref(false),
  courseware = ref([]),
  games = ref([]),
  formRef = ref();
const time = (v) => new Date(v).toLocaleString("zh-CN", { hour12: false });
async function searchStudents(keyword = "") {
  searching.value = true;
  try {
    students.value = (
      await listUsers({ page: 1, size: 100, role_key: "edu_student", keyword })
    ).rows;
  } finally {
    searching.value = false;
  }
}
async function load() {
  if (!studentId.value) return;
  loading.value = true;
  try {
    rows.value = (await listGrants(studentId.value)).data;
  } finally {
    loading.value = false;
  }
}
async function searchCourseware(keyword = "") {
  courseware.value = (
    await listCatalog("courseware", {
      page: 1,
      size: 100,
      status: "PUBLISHED",
      keyword,
    })
  ).rows;
}
async function open() {
  form.value = {
    scope_type: "COURSEWARE",
    start_time: new Date(),
    end_time: new Date(Date.now() + 180 * 86400000),
    remark: "",
  };
  dialog.value = true;
  await searchCourseware();
  games.value = (
    await listCatalog("games", { page: 1, size: 100, status: "PUBLISHED" })
  ).rows.filter((g) => g.access_policy === "STUDENT");
}
async function save() {
  if (
    !form.value.remark ||
    !(form.value.scope_type === "GAME"
      ? form.value.game_id
      : form.value.courseware_id) ||
    !form.value.start_time ||
    !form.value.end_time
  ) {
    ElMessage.warning("请完整填写资源、有效期和原因");
    return;
  }
  saving.value = true;
  try {
    await grant({
      ...form.value,
      student_user_id: studentId.value,
      start_time: new Date(form.value.start_time).toISOString(),
      end_time: new Date(form.value.end_time).toISOString(),
    });
    ElMessage.success("授权已发放");
    dialog.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}
async function revokeOne(row) {
  try {
    const r = await ElMessageBox.prompt(
      "请输入撤销原因，撤销后游戏入口也会失效。",
      "撤销授权",
      { inputValidator: (v) => !!v?.trim() || "请填写原因" },
    );
    await revoke(row.entitlement_id, r.value);
    ElMessage.success("已撤销");
    await load();
  } catch (e) {
    if (e !== "cancel" && e !== "close") console.error(e);
  }
}
onMounted(() => searchStudents());
</script>
