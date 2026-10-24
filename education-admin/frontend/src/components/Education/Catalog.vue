<template>
  <div class="app-container education-page">
    <div class="edu-page-heading">
      <div>
        <span class="edu-eyebrow">教学工作台 / {{ config.title }}</span>
        <h1>{{ config.title }}</h1>
        <p>{{ config.subtitle }}</p>
      </div>
      <el-button
        v-hasPermi="[`biz:${config.module}:add`]"
        type="primary"
        icon="Plus"
        @click="edit()"
        >新增{{
          kind === "games" ? "游戏" : kind === "courseware" ? "课件" : "记录"
        }}</el-button
      >
    </div>
    <template v-if="kind === 'schools'"
      ><el-alert
        v-for="notice in expiryNotices"
        :key="notice.notice_id"
        :title="notice.notice_title"
        :description="notice.notice_content"
        type="warning"
        :closable="false"
        style="margin-bottom: 12px"
    /></template>
    <el-card shadow="never" class="edu-card"
      ><el-form :inline="true" @submit.prevent="search">
        <template v-if="kind === 'courseware'">
          <el-form-item label="学科"
            ><el-select
              v-model="query.subject_code"
              clearable
              style="width: 130px"
              ><el-option
                v-for="(label, value) in subjects"
                :key="value"
                :value="value"
                :label="label" /></el-select
          ></el-form-item>
          <el-form-item label="册次"
            ><el-input-number v-model="query.volume" :min="1" :max="12"
          /></el-form-item>
          <el-form-item label="阶次"
            ><el-select
              v-model="query.stage_code"
              clearable
              style="width: 130px"
              ><el-option
                v-for="d in biz_course_stage"
                :key="d.value"
                :value="d.value"
                :label="d.label" /></el-select
          ></el-form-item>
          <el-form-item label="能力维度"
            ><el-select v-model="query.ability" clearable style="width: 160px"
              ><el-option
                v-for="d in biz_math_ability"
                :key="d.value"
                :value="d.value"
                :label="d.label" /></el-select
          ></el-form-item>
        </template>
        <el-form-item label="关键词"
          ><el-input
            v-model="query.keyword"
            :placeholder="`搜索${config.title}名称`"
            clearable
            @keyup.enter="search" /></el-form-item
        ><el-form-item
          v-if="['games', 'courseware'].includes(kind)"
          label="状态"
          ><el-select
            v-model="query.status"
            clearable
            placeholder="全部状态"
            style="width: 150px"
            ><el-option
              v-for="(label, value) in {
                DRAFT: '草稿',
                PUBLISHED: '已发布',
                DISABLED: '已停用',
              }"
              :key="value"
              :label="label"
              :value="value" /></el-select></el-form-item
        ><el-form-item v-if="kind === 'games'" label="接入方式"
          ><el-select
            v-model="query.source_type"
            clearable
            placeholder="全部类型"
            style="width: 130px"
            ><el-option value="INTERNAL" label="内部游戏" /><el-option
              value="EXTERNAL"
              label="外部引用" /></el-select></el-form-item
        ><el-form-item v-if="kind === 'games'" label="分类"
          ><el-select
            v-model="query.category_code"
            clearable
            placeholder="全部分类"
            style="width: 130px"
            ><el-option
              v-for="(label, value) in config.fields.find(
                (f) => f.key === 'category_code',
              ).options"
              :key="value"
              :value="value"
              :label="label" /></el-select></el-form-item
        ><el-form-item
          ><el-button type="primary" icon="Search" @click="search"
            >查询</el-button
          ><el-button icon="Refresh" @click="reset"
            >重置</el-button
          ></el-form-item
        >
      </el-form>
      <el-button
        v-if="kind === 'classes'"
        v-hasPermi="['biz:class:edit']"
        :disabled="!selected.length"
        type="warning"
        @click="archiveClasses"
        >毕业归档所选班级</el-button
      >
      <div class="edu-table-caption">
        <b>{{ config.title }}列表</b><span>共 {{ total }} 条记录</span
        ><el-button link icon="Refresh" @click="load">刷新</el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        empty-text="当前筛选条件下暂无可用资源"
        :row-key="config.id"
        @selection-change="(v) => (selected = v)"
      >
        <el-table-column
          v-if="kind === 'classes'"
          type="selection"
          :selectable="(row) => row.status === 'ACTIVE'"
        />
        <el-table-column :prop="config.id" label="ID" width="85" />
        <el-table-column
          :label="kind === 'pool' ? '学科范围' : '名称'"
          min-width="220"
          ><template #default="{ row }"
            ><div class="edu-resource-cell">
              <div
                v-if="['games', 'courseware'].includes(kind)"
                :class="['edu-resource-icon', kind]"
              >
                {{ kind === "games" ? "◈" : "▤" }}
              </div>
              <div>
                <b>{{ subjects[row[config.name]] || row[config.name] }}</b>
                <div class="edu-muted">
                  {{
                    row.game_code ||
                    row.school_code ||
                    row.academic_year ||
                    row.description
                  }}
                </div>
              </div>
            </div></template
          ></el-table-column
        >
        <el-table-column v-if="kind === 'games'" label="接入方式" width="115"
          ><template #default="{ row }"
            ><el-tag effect="plain">{{
              labels[row.source_type]
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column v-if="kind === 'games'" label="访问策略" width="150"
          ><template #default="{ row }">{{
            labels[row.access_policy]
          }}</template></el-table-column
        >
        <el-table-column
          v-if="['courseware', 'pool'].includes(kind)"
          label="学科 / 册次"
          width="160"
          ><template #default="{ row }"
            >{{ subjects[row.subject_code] || "全部学科" }} ·
            {{ row.volume ? `第${row.volume}册` : "全部册次" }}</template
          ></el-table-column
        >
        <el-table-column
          v-if="['courseware', 'classes', 'schools', 'pool'].includes(kind)"
          label="学校"
          min-width="160"
          ><template #default="{ row }">{{
            schoolName(row.school_id)
          }}</template></el-table-column
        >
        <el-table-column
          v-if="kind === 'classes'"
          prop="grade"
          label="年级"
          width="90"
        />
        <el-table-column
          v-if="['schools', 'pool'].includes(kind)"
          label="到期时间"
          min-width="180"
          ><template #default="{ row }">{{
            formatTime(row.expire_time || row.end_time)
          }}</template></el-table-column
        >
        <el-table-column v-if="kind !== 'pool'" label="状态" width="105"
          ><template #default="{ row }"
            ><el-tag
              :type="statusType(row.status || row.school_status)"
              effect="light"
              >{{ labels[row.status || row.school_status] }}</el-tag
            ></template
          ></el-table-column
        >
        <el-table-column
          label="操作"
          :width="['games', 'courseware'].includes(kind) ? 300 : 120"
          fixed="right"
          ><template #default="{ row }">
            <el-button
              v-if="kind === 'games'"
              v-hasPermi="['biz:game:preview']"
              link
              type="primary"
              icon="View"
              @click="previewGame(row.game_id)"
              >预览</el-button
            >
            <el-button
              v-if="kind === 'courseware'"
              v-hasPermi="['biz:courseware:preview']"
              link
              type="primary"
              icon="View"
              @click="previewCourseware(row)"
              >预览</el-button
            >
            <el-button
              v-if="kind === 'courseware' && !row.school_id"
              v-hasPermi="['biz:courseware:add']"
              link
              type="primary"
              @click="customize(row)"
              >学校定制</el-button
            >
            <el-button
              v-hasPermi="[`biz:${config.module}:edit`]"
              link
              type="primary"
              icon="Edit"
              @click="edit(row)"
              >编辑</el-button
            >
            <el-button
              v-if="['games', 'courseware', 'pool'].includes(kind)"
              v-hasPermi="[`biz:${config.module}:remove`]"
              link
              type="danger"
              @click="remove(row)"
              >删除</el-button
            >
          </template></el-table-column
        >
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="query.page"
        v-model:limit="query.size"
        @pagination="load"
      />
    </el-card>
    <el-alert v-if="previewLink" type="info" :closable="false" class="edu-gap"
      ><template #title
        >预览入口已生成，若新窗口未打开，请<el-link
          :href="previewLink"
          target="_blank"
          rel="noopener noreferrer"
          type="primary"
          >点击进入游戏</el-link
        >。内部游戏入口60秒内有效。</template
      ></el-alert
    >
    <el-dialog
      v-model="dialog"
      :title="`${formId ? '编辑' : '新增'}${config.title}`"
      width="680px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        label-width="115px"
        :rules="rules"
        @submit.prevent="save"
        ><el-form-item
          v-for="field in config.fields"
          :key="field.key"
          :label="field.label"
          :prop="field.key"
        >
          <el-select
            v-if="field.type === 'select'"
            v-model="form[field.key]"
            :disabled="field.key === 'subject_code' && !!props.subject"
            style="width: 100%"
            ><el-option
              v-for="(label, value) in field.key === 'subject_code'
                ? subjects
                : field.options"
              :key="value"
              :label="label"
              :value="value"
          /></el-select>
          <el-select
            v-else-if="field.type === 'school'"
            v-model="form[field.key]"
            filterable
            clearable
            style="width: 100%"
            @clear="form[field.key] = null"
            ><el-option
              v-for="s in schoolOptions"
              :key="s.school_id"
              :label="s.school_name"
              :value="String(s.school_id)"
          /></el-select>
          <el-select
            v-else-if="field.type === 'games'"
            v-model="form.game_ids"
            multiple
            filterable
            remote
            :remote-method="searchGameOptions"
            style="width: 100%"
            ><el-option
              v-for="g in gameOptions"
              :key="g.game_id"
              :label="g.game_name"
              :value="String(g.game_id)"
          /></el-select>
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form[field.key]"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            style="width: 100%"
          />
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="form[field.key]"
            :min="field.min"
            :max="field.max"
            :precision="0"
          />
          <el-input
            v-else
            v-model="form[field.key]"
            :type="field.type === 'textarea' ? 'textarea' : 'text'"
            :rows="3"
            :maxlength="field.type === 'textarea' ? 4000 : 1024"
            :disabled="
              !!formId && ['game_code', 'school_id'].includes(field.key)
            "
          />
          <el-space v-if="kind === 'schools' && field.key === 'expire_time'"
            ><el-input-number
              v-model="expiryDays"
              :min="1"
              :max="3650"
            /><el-button
              @click="
                form.expire_time = new Date(Date.now() + expiryDays * 86400000)
              "
              >按天数计算</el-button
            ></el-space
          >
          <div v-if="field.hint" class="edu-field-hint">{{ field.hint }}</div>
        </el-form-item>
        <template v-if="kind === 'courseware' && form.extension">
          <el-divider>学科扩展</el-divider>
          <el-form-item label="封面"
            ><image-upload v-model="form.cover_url" :limit="1"
          /></el-form-item>
          <template v-if="form.subject_code === 'MATH'">
            <el-form-item label="能力维度" required
              ><el-select v-model="form.extension.ability"
                ><el-option
                  v-for="d in biz_math_ability"
                  :key="d.value"
                  :value="d.value"
                  :label="d.label" /></el-select
            ></el-form-item>
            <el-form-item label="讨论图片"
              ><image-upload v-model="form.extension.discuss_img" :limit="1"
            /></el-form-item>
            <el-form-item label="讨论内容"
              ><editor
                v-model="form.extension.discuss_content"
                :min-height="160"
            /></el-form-item>
            <el-form-item label="介绍视频"
              ><file-upload
                v-model="form.extension.intro_video"
                :limit="1"
                :file-type="['mp4', 'webm']"
                :file-size="100"
            /></el-form-item>
            <el-form-item label="互动游戏地址"
              ><el-input
                v-model="form.extension.game_url"
                placeholder="HTTPS地址"
            /></el-form-item>
            <el-form-item label="教师用书PDF"
              ><file-upload
                v-model="form.extension.teacher_book"
                :limit="1"
                :file-type="['pdf']"
                :file-size="50"
            /></el-form-item>
          </template>
          <template v-if="form.subject_code === 'READING'">
            <el-form-item label="配音素材"
              ><el-select
                v-model="form.extension.dubbing_material_id"
                filterable
                clearable
                remote
                :remote-method="searchMaterials"
                @visible-change="(v) => v && searchMaterials('')"
                ><el-option
                  v-for="m in materials"
                  :key="m.material_id"
                  :value="m.material_id"
                  :label="m.title" /></el-select
            ></el-form-item>
            <el-form-item label="课件流程JSON" required
              ><JsonConfigEditor v-model="form.extension.config_json"
            /></el-form-item>
          </template>
        </template>
      </el-form>
      <template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save"
          >保存</el-button
        ></template
      >
    </el-dialog>
    <el-dialog v-model="coursewareDialog" title="课件预览" width="760px"
      ><template v-if="preview"
        ><div class="edu-preview-heading">
          <span
            >第 {{ preview.volume }} 册 ·
            {{ subjects[preview.subject_code] }}</span
          >
          <h2>{{ preview.courseware_name }}</h2>
        </div>
        <p style="white-space: pre-wrap">
          {{ preview.description || "暂无课件说明" }}
        </p>
        <el-image
          v-if="preview.cover_url"
          :src="preview.cover_url"
          fit="contain"
          style="max-width: 100%; max-height: 220px" />
        <template v-if="preview.subject_code === 'MATH' && preview.extension">
          <p v-if="preview.extension.ability">
            能力维度：{{
              biz_math_ability.find(
                (d) => d.value === preview.extension.ability,
              )?.label || preview.extension.ability
            }}
          </p>
          <el-image
            v-if="preview.extension.discuss_img"
            :src="preview.extension.discuss_img"
            fit="contain"
            style="max-height: 300px"
          />
          <editor
            v-if="preview.extension.discuss_content"
            :model-value="preview.extension.discuss_content"
            :read-only="true"
            :min-height="120"
          />
          <video
            v-if="preview.extension.intro_video"
            :src="preview.extension.intro_video"
            controls
            preload="metadata"
            style="width: 100%; margin-top: 12px"
          />
          <p v-if="preview.extension.teacher_book">
            <el-link
              :href="preview.extension.teacher_book"
              target="_blank"
              rel="noopener noreferrer"
              type="primary"
              >查看教师用书PDF</el-link
            >
          </p>
        </template>
        <template v-if="preview.subject_code === 'READING' && preview.extension"
          ><h3>课件流程</h3>
          <pre style="white-space: pre-wrap">{{
            preview.extension.config_json
          }}</pre>
        </template>
        <h3>互动游戏</h3>
        <el-space wrap
          ><el-button
            v-for="gid in preview.game_ids"
            :key="gid"
            type="primary"
            plain
            icon="VideoPlay"
            @click="previewGame(gid)"
            >{{
              gameOptions.find((g) => String(g.game_id) === String(gid))
                ?.game_name || `游戏 ${gid}`
            }}</el-button
          ></el-space
        ><el-empty
          v-if="!preview.game_ids?.length"
          description="暂未关联互动游戏" /></template
    ></el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import JsonConfigEditor from "./JsonConfigEditor.vue";
import request from "@/utils/request";
import { learningPost, learningGet } from "@/api/learning";
import { configs } from "./catalog-config";
import {
  listCatalog,
  getCatalog,
  saveCatalog,
  deleteCatalog,
  options,
  launchGame,
  labels,
  subjects as defaultSubjects,
  statusType,
} from "@/api/education";
const { biz_math_ability, biz_course_stage, biz_subject } =
  getCurrentInstance().proxy.useDict(
    "biz_math_ability",
    "biz_course_stage",
    "biz_subject",
  );
const subjects = computed(() =>
  biz_subject.value.length
    ? Object.fromEntries(biz_subject.value.map((d) => [d.value, d.label]))
    : defaultSubjects,
);
const expiryNotices = ref([]);
const materials = ref([]),
  selected = ref([]),
  expiryDays = ref(180);
const props = defineProps({
  kind: { type: String, required: true },
  subject: { type: String, default: "" },
});
const kind = props.kind,
  config = {
    ...configs[kind],
    ...(props.subject
      ? { title: props.subject === "MATH" ? "数学课件" : "阅读课件" }
      : {}),
  };
const rows = ref([]),
  total = ref(0),
  loading = ref(false),
  saving = ref(false),
  dialog = ref(false),
  formRef = ref(),
  form = ref({}),
  formId = ref(null),
  schoolOptions = ref([]),
  gameOptions = ref([]),
  previewLink = ref(""),
  preview = ref(null),
  coursewareDialog = ref(false);
const query = reactive({
  page: 1,
  size: 20,
  keyword: "",
  status: "",
  source_type: "",
  category_code: "",
  subject_code: props.subject,
  volume: undefined,
  stage_code: "",
  ability: "",
});
const rules = computed(() =>
  Object.fromEntries(
    config.fields
      .filter((f) => f.required)
      .map((f) => [
        f.key,
        [{ required: true, message: `请填写${f.label}`, trigger: "blur" }],
      ]),
  ),
);
const formatTime = (t) =>
  t ? new Date(t).toLocaleString("zh-CN", { hour12: false }) : "—";
const schoolName = (id) =>
  id
    ? schoolOptions.value.find((s) => String(s.school_id) === String(id))
        ?.school_name || `学校 ${id}`
    : "全局模板";
async function load() {
  loading.value = true;
  try {
    const r = await listCatalog(kind, {
      ...query,
      ...(props.subject ? { subject_code: props.subject } : {}),
    });
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
  Object.assign(query, {
    page: 1,
    keyword: "",
    status: "",
    source_type: "",
    category_code: "",
    subject_code: props.subject,
    volume: undefined,
    stage_code: "",
    ability: "",
  });
  load();
}
async function edit(row) {
  formId.value = row?.[config.id] || null;
  form.value = { ...config.defaults };
  if (row) {
    const r = await getCatalog(kind, formId.value, true);
    form.value = { ...r.data };
    if (form.value.game_ids)
      form.value.game_ids = form.value.game_ids.map(String);
  } else if (schoolOptions.value.length === 1)
    form.value.school_id = String(schoolOptions.value[0].school_id);
  if (form.value.school_id) form.value.school_id = String(form.value.school_id);
  if (props.subject) form.value.subject_code = props.subject;
  if (kind === "courseware")
    form.value.extension = {
      stage: form.value.stage_code || "BASIC",
      ability: "",
      config_json: "{}",
      ...(form.value.extension || {}),
    };
  dialog.value = true;
}
async function save() {
  await formRef.value.validate();
  saving.value = true;
  try {
    const data = { ...form.value };
    if (kind === "courseware" && data.subject_code === "MATH")
      data.extension = { ...data.extension, stage: data.stage_code };
    if (kind === "courseware" && data.subject_code === "READING") {
      try {
        const parsed = JSON.parse(data.extension.config_json);
        if (!parsed || Array.isArray(parsed) || typeof parsed !== "object")
          throw new Error();
      } catch {
        ElMessage.error("课件流程须为有效JSON对象");
        return;
      }
    }
    for (const f of config.fields)
      if (f.type === "date" && data[f.key])
        data[f.key] = new Date(data[f.key]).toISOString();
    await saveCatalog(kind, formId.value, data);
    ElMessage.success("保存成功");
    dialog.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}
async function remove(row) {
  try {
    await ElMessageBox.confirm(
      "删除后将保留历史引用，确认删除该记录？",
      "确认删除",
      { type: "warning" },
    );
    await deleteCatalog(kind, row[config.id]);
    ElMessage.success("已删除");
    await load();
  } catch (e) {
    if (e !== "cancel" && e !== "close") console.error(e);
  }
}
async function searchGameOptions(keyword = "") {
  gameOptions.value = (
    await listCatalog("games", { page: 1, size: 100, keyword })
  ).rows;
}
async function previewGame(id) {
  const win = window.open("about:blank", "_blank");
  if (win) win.opener = null;
  try {
    const r = await launchGame(id);
    previewLink.value = r.data.url;
    if (r.data.protection === "ENTRY_ONLY") ElMessage.info(r.data.message);
    if (win) win.location.href = r.data.url;
  } catch (e) {
    if (win) win.close();
  }
}
async function previewCourseware(row) {
  const r = await getCatalog(kind, row.courseware_id);
  preview.value = r.data;
  coursewareDialog.value = true;
}
async function customize(row) {
  const { value } = await ElMessageBox.prompt(
    "请输入有权限管理的学校ID",
    "建立学校定制",
  );
  await request({
    url: `/biz/courseware/${row.courseware_id}/customize`,
    method: "post",
    data: { school_id: value },
  });
  ElMessage.success("已建立学校定制草稿");
  await load();
}
async function archiveClasses() {
  await ElMessageBox.confirm(
    "归档后所选班级的学生账号会停用，历史数据保留。确认继续？",
    "毕业归档",
  );
  await learningPost(
    "classes/archive",
    selected.value.map((c) => c.class_id),
  );
  ElMessage.success("归档完成");
  await load();
}
async function searchMaterials(keyword = "") {
  const r = await learningGet("dubbing/materials", {
    volume: form.value.volume,
    status: "0",
    keyword,
    size: 100,
  });
  materials.value = r.rows.filter((m) =>
    m.material_tag.split(",").some((t) => ["01", "03"].includes(t)),
  );
}
function formatJson() {
  try {
    form.value.extension.config_json = JSON.stringify(
      JSON.parse(form.value.extension.config_json),
      null,
      2,
    );
    ElMessage.success("JSON格式有效");
  } catch {
    ElMessage.error("JSON格式无效");
  }
}
onMounted(async () => {
  if (kind === "schools")
    expiryNotices.value = (await learningGet("notices")).data;
  await load();
  const o = await options();
  schoolOptions.value = o.data.schools;
  if (kind === "courseware") {
    const r = await listCatalog("games", { page: 1, size: 100 });
    gameOptions.value = r.rows;
  }
});
</script>
