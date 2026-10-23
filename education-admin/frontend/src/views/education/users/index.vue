<template>
  <div class="app-container education-page">
    <div class="edu-page-heading">
      <div>
        <span class="edu-eyebrow">组织与账号</span>
        <h1>用户管理</h1>
        <p>统一维护老师、学生与学校管理员，按归属范围管理账号。</p>
      </div>
      <el-button
        v-hasPermi="['biz:user:add']"
        type="primary"
        icon="Plus"
        @click="edit()"
        >新增用户</el-button
      >
    </div>
    <el-card shadow="never" class="edu-card"
      ><el-form :inline="true" @submit.prevent="search"
        ><el-form-item label="姓名 / 账号 / 手机"
          ><el-input
            v-model="query.keyword"
            clearable
            @keyup.enter="search" /></el-form-item
        ><el-form-item label="身份"
          ><el-select v-model="query.role_key" clearable style="width: 150px"
            ><el-option
              v-for="role in roleChoices"
              :key="role"
              :label="labels[role]"
              :value="role" /></el-select></el-form-item
        ><el-form-item label="学校"
          ><el-select
            v-model="query.school_id"
            clearable
            filterable
            style="width: 180px"
            ><el-option
              v-for="s in opts.schools"
              :key="s.school_id"
              :value="String(s.school_id)"
              :label="s.school_name" /></el-select></el-form-item
        ><el-form-item
          ><el-button type="primary" icon="Search" @click="search"
            >查询</el-button
          ><el-button @click="reset">重置</el-button></el-form-item
        ></el-form
      >
      <div class="edu-table-caption">
        <b>用户列表</b><span>共 {{ total }} 位用户</span>
      </div>
      <el-table v-loading="loading" :data="rows" stripe
        ><el-table-column
          prop="user_id"
          label="ID"
          width="90"
        /><el-table-column
          prop="nick_name"
          label="姓名"
          min-width="130"
        /><el-table-column
          prop="user_name"
          label="登录账号"
          min-width="180"
        /><el-table-column label="身份" min-width="150"
          ><template #default="{ row }"
            ><el-tag
              v-for="r in row.role_keys"
              :key="r"
              effect="plain"
              style="margin-right: 4px"
              >{{ labels[r] || r }}</el-tag
            ></template
          ></el-table-column
        ><el-table-column
          prop="phonenumber"
          label="手机号"
          width="145"
        /><el-table-column label="学校 / 班级" min-width="190"
          ><template #default="{ row }"
            >{{ schoolName(row.dept_id) }}
            <div class="edu-muted">{{ className(row.class_id) }}</div></template
          ></el-table-column
        ><el-table-column label="状态" width="90"
          ><template #default="{ row }"
            ><el-tag :type="row.status === '0' ? 'success' : 'danger'">{{
              row.status === "0" ? "正常" : "冻结"
            }}</el-tag></template
          ></el-table-column
        ><el-table-column label="操作" width="210" fixed="right"
          ><template #default="{ row }"
            ><el-button
              v-hasPermi="['biz:user:query']"
              link
              type="primary"
              @click="showDetail(row)"
              >详情</el-button
            ><el-button
              v-if="editable(row)"
              v-hasPermi="['biz:user:edit']"
              link
              type="primary"
              @click="edit(row)"
              >编辑</el-button
            ><el-button
              v-if="editable(row)"
              v-hasPermi="['biz:user:resetPwd']"
              link
              type="warning"
              @click="resetPwd(row)"
              >重置密码</el-button
            ></template
          ></el-table-column
        ></el-table
      >
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="query.page"
        v-model:limit="query.size"
        @pagination="load"
    /></el-card>
    <el-dialog
      v-model="dialog"
      :title="formId ? '编辑用户' : '新增用户'"
      width="620px"
      destroy-on-close
      ><el-form ref="formRef" :model="form" label-width="110px" :rules="rules">
        <el-form-item label="用户身份" prop="role_key"
          ><el-select v-model="form.role_key" :disabled="!!formId"
            ><el-option
              v-for="role in roleChoices"
              :key="role"
              :value="role"
              :label="labels[role]" /></el-select
        ></el-form-item>
        <el-form-item label="姓名" prop="nick_name"
          ><el-input v-model="form.nick_name" maxlength="30"
        /></el-form-item>
        <el-form-item
          v-if="form.role_key !== 'edu_student'"
          label="登录账号"
          prop="user_name"
          ><el-input
            v-model="form.user_name"
            :disabled="!!formId"
            maxlength="30"
          />
          <div class="edu-field-hint">
            以字母开头，3至30位字母、数字、下划线或短横线
          </div></el-form-item
        >
        <el-form-item label="学校" prop="school_id"
          ><el-select v-model="form.school_id" :disabled="!!formId" filterable
            ><el-option
              v-for="s in opts.schools"
              :key="s.school_id"
              :label="s.school_name"
              :value="String(s.school_id)" /></el-select
        ></el-form-item>
        <template v-if="form.role_key === 'edu_student'"
          ><el-form-item label="学号" prop="student_no"
            ><el-input v-model="form.student_no" maxlength="32" />
            <div class="edu-field-hint">
              学号在学校内唯一，系统自动生成独立内部账号
            </div></el-form-item
          ><el-form-item label="班级" prop="class_id"
            ><el-select v-model="form.class_id" filterable
              ><el-option
                v-for="c in classChoices"
                :key="c.class_id"
                :value="String(c.class_id)"
                :label="c.class_name" /></el-select></el-form-item
        ></template>
        <el-form-item v-if="form.role_key === 'edu_teacher'" label="任教班级"
          ><el-select v-model="form.class_ids" multiple filterable
            ><el-option
              v-for="c in classChoices"
              :key="c.class_id"
              :value="String(c.class_id)"
              :label="c.class_name" /></el-select
        ></el-form-item>
        <el-form-item label="手机号"
          ><el-input
            v-model="form.phone"
            maxlength="11"
            placeholder="可选，用户验证后才能用于登录"
        /></el-form-item>
        <el-form-item label="状态"
          ><el-radio-group v-model="form.status"
            ><el-radio value="0">正常</el-radio
            ><el-radio value="1">冻结</el-radio></el-radio-group
          ></el-form-item
        >
        <el-alert
          v-if="!formId"
          type="info"
          :closable="false"
          title="创建后生成一次性展示的随机初始密码，用户首次登录必须修改。" /></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save"
          >保存</el-button
        ></template
      ></el-dialog
    >
    <el-dialog v-model="detailDialog" title="用户详情" width="620px"
      ><el-descriptions v-if="detail" :column="1" border
        ><el-descriptions-item label="姓名">{{
          detail.nick_name
        }}</el-descriptions-item
        ><el-descriptions-item label="登录账号">{{
          detail.user_name
        }}</el-descriptions-item
        ><el-descriptions-item label="手机号">{{
          detail.phone || "未绑定"
        }}</el-descriptions-item
        ><el-descriptions-item label="学校">{{
          schoolName(detail.school_id)
        }}</el-descriptions-item
        ><el-descriptions-item label="学号">{{
          detail.student_no || "—"
        }}</el-descriptions-item
        ><el-descriptions-item label="班级">{{
          className(detail.class_id)
        }}</el-descriptions-item
        ><el-descriptions-item label="任教班级">{{
          detail.teaching
            ?.filter((t) => !t.end_time)
            .map((t) => className(t.class_id))
            .join("、") || "—"
        }}</el-descriptions-item></el-descriptions
      ></el-dialog
    >
    <el-dialog
      v-model="credentialDialog"
      title="请妥善交付初始凭据"
      width="540px"
      :close-on-click-modal="false"
      ><el-alert
        type="warning"
        :closable="false"
        title="随机初始密码仅本次展示，请通过授权渠道交付。首次登录需修改。"
      /><el-descriptions :column="1" border class="edu-gap"
        ><el-descriptions-item label="账号">{{
          credential.user_name || credential.nick_name
        }}</el-descriptions-item
        ><el-descriptions-item label="初始密码"
          ><code style="user-select: all">{{
            credential.temporary_password
          }}</code></el-descriptions-item
        ></el-descriptions
      ><template #footer
        ><el-button
          type="primary"
          @click="
            credentialDialog = false;
            credential = {};
          "
          >已记录</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  listUsers,
  getUser,
  saveUser,
  resetPassword,
  options,
  labels,
} from "@/api/education";
import useUserStore from "@/store/modules/user";
const store = useUserStore(),
  opts = reactive({ schools: [], classes: [] }),
  query = reactive({
    page: 1,
    size: 20,
    keyword: "",
    role_key: "",
    school_id: "",
  });
const roleChoices = computed(() => opts.roles || []);
const rows = ref([]),
  total = ref(0),
  loading = ref(false),
  dialog = ref(false),
  form = ref({}),
  formId = ref(null),
  formRef = ref(),
  saving = ref(false),
  detail = ref(),
  detailDialog = ref(false),
  credential = ref({}),
  credentialDialog = ref(false);
const rules = {
  nick_name: [{ required: true, message: "请输入姓名" }],
  role_key: [{ required: true, message: "请选择身份" }],
  school_id: [{ required: true, message: "请选择学校" }],
  student_no: [{ required: true, message: "请输入学号" }],
  class_id: [{ required: true, message: "请选择班级" }],
  user_name: [{ required: true, message: "请输入账号" }],
};
const editable = (row) => row.can_edit === true;
const classChoices = computed(() =>
  opts.classes.filter(
    (c) =>
      String(c.school_id) === String(form.value.school_id) &&
      c.status === "ACTIVE",
  ),
);
const schoolName = (id) =>
  opts.schools.find((s) => String(s.school_id) === String(id))?.school_name ||
  "—";
const className = (id) =>
  opts.classes.find((c) => String(c.class_id) === String(id))?.class_name ||
  "—";
async function load() {
  loading.value = true;
  try {
    const r = await listUsers(query);
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
  Object.assign(query, { page: 1, keyword: "", role_key: "", school_id: "" });
  load();
}
async function edit(row) {
  formId.value = row?.user_id || null;
  form.value = {
    role_key: "edu_student",
    status: "0",
    phone: "",
    class_ids: [],
    school_id:
      opts.schools.length === 1 ? String(opts.schools[0].school_id) : null,
  };
  if (row) {
    const r = await getUser(row.user_id);
    form.value = {
      ...r.data,
      role_key: r.data.role_keys[0],
      class_ids: r.data.teaching
        .filter((t) => !t.end_time)
        .map((t) => String(t.class_id)),
      school_id: String(r.data.school_id),
      class_id: r.data.class_id ? String(r.data.class_id) : null,
    };
  }
  dialog.value = true;
}
async function showDetail(row) {
  detail.value = (await getUser(row.user_id)).data;
  detailDialog.value = true;
}
async function save() {
  await formRef.value.validate();
  saving.value = true;
  try {
    const r = await saveUser(formId.value, form.value);
    dialog.value = false;
    if (r.data.temporary_password) {
      credential.value = r.data;
      credentialDialog.value = true;
    }
    ElMessage.success("保存成功");
    await load();
  } finally {
    saving.value = false;
  }
}
async function resetPwd(row) {
  try {
    await ElMessageBox.confirm(
      `重置${row.nick_name}的密码后，已有登录和游戏会话将失效，是否继续？`,
      "重置密码",
      { type: "warning" },
    );
    const r = await resetPassword(row.user_id);
    credential.value = { ...r.data, user_name: row.user_name };
    credentialDialog.value = true;
  } catch (e) {
    if (e !== "cancel" && e !== "close") console.error(e);
  }
}
onMounted(async () => {
  const r = await options();
  Object.assign(opts, r.data);
  await load();
});
</script>
