<template>
  <div class="app-container">
    <h2>课堂PK与积分</h2>
    <el-form inline>
      <el-form-item label="任教班级"
        ><el-select
          v-model="query.class_id"
          style="width: 200px"
          @change="refresh"
          ><el-option
            v-for="c in classes"
            :key="c.class_id"
            :value="String(c.class_id)"
            :label="c.class_name" /></el-select
      ></el-form-item>
      <el-form-item label="当前课件"
        ><el-select
          v-model="query.courseware_id"
          filterable
          remote
          :remote-method="findCourses"
          style="width: 240px"
          @change="refresh"
          ><el-option
            v-for="c in courses"
            :key="c.courseware_id"
            :value="String(c.courseware_id)"
            :label="c.courseware_name" /></el-select
      ></el-form-item>
      <el-form-item><el-button @click="refresh">刷新</el-button></el-form-item>
    </el-form>
    <el-alert
      v-if="!query.class_id || !query.courseware_id"
      title="请选择班级和课件。各班级、各课件独立计分。"
      :closable="false"
    />
    <template v-else>
      <el-collapse
        ><el-collapse-item title="实时积分榜" name="rank"
          ><el-table :data="ranking"
            ><el-table-column type="index" label="排名" /><el-table-column
              prop="nick_name"
              label="学生" /><el-table-column
              prop="total_score"
              label="当前课件积分" /></el-table></el-collapse-item
      ></el-collapse>
      <el-tabs v-model="tab">
        <el-tab-pane label="课堂表现" name="performance">
          <el-button
            v-hasPermi="['biz:score:edit']"
            :loading="busy"
            @click="allPlus"
            >全体 +1</el-button
          >
          <el-table :data="students"
            ><el-table-column prop="nick_name" label="学生" /><el-table-column
              v-for="dimension in performanceDimensions"
              :key="dimension"
              :label="dimension"
              ><template #default="{ row }"
                ><el-button
                  v-hasPermi="['biz:score:edit']"
                  :disabled="busy"
                  @click="adjust(row, dimension, 1)"
                  >+1</el-button
                ><el-button
                  v-hasPermi="['biz:score:edit']"
                  :disabled="busy"
                  @click="adjust(row, dimension, -1)"
                  >−1</el-button
                ></template
              ></el-table-column
            ></el-table
          >
        </el-tab-pane>
        <el-tab-pane label="对战" name="battle">
          <el-radio-group v-model="query.mode" @change="modeChanged"
            ><el-radio-button value="PERSONAL">个人PK</el-radio-button
            ><el-radio-button value="GROUP"
              >小组PK</el-radio-button
            ></el-radio-group
          >
          <el-select
            v-if="
              query.mode === 'GROUP' && selectedCourse?.subject_code === 'MATH'
            "
            v-model="query.stage"
            style="width: 140px; margin-left: 12px"
            @change="loadRounds"
            ><el-option value="BASIC" label="新手" /><el-option
              value="LEVEL2"
              label="能手" /><el-option value="LEVEL3" label="大师"
          /></el-select>
          <el-button
            v-hasPermi="['biz:pk:edit']"
            type="primary"
            :disabled="busy"
            style="margin-left: 12px"
            @click="match"
            >整体随机匹配</el-button
          ><el-button
            v-hasPermi="['biz:pk:edit']"
            :disabled="busy"
            @click="clear"
            >清空本轮数据</el-button
          >
          <el-card
            v-for="round in rounds"
            :key="round.round_id"
            style="margin-top: 16px"
            ><template #header
              >第 {{ round.round_no }} 轮 ·
              {{
                round.status === "CLEARED"
                  ? "已清空"
                  : round.status === "COMPLETED"
                    ? "已完成"
                    : "进行中"
              }}</template
            >
            <el-table :data="round.matches"
              ><el-table-column label="左方"
                ><template #default="{ row }">{{
                  participant(row.left_id)
                }}</template></el-table-column
              ><el-table-column label="右方"
                ><template #default="{ row }">{{
                  row.right_id ? participant(row.right_id) : "轮空晋级"
                }}</template></el-table-column
              ><el-table-column label="获胜者"
                ><template #default="{ row }">{{
                  row.winner_id ? participant(row.winner_id) : "待确定"
                }}</template></el-table-column
              ><el-table-column label="操作" min-width="230"
                ><template #default="{ row }"
                  ><template v-if="row.right_id && round.status !== 'CLEARED'"
                    ><el-button
                      v-hasPermi="['biz:pk:edit']"
                      link
                      :disabled="busy"
                      @click="result(row, row.left_id)"
                      >左方胜</el-button
                    ><el-button
                      v-hasPermi="['biz:pk:edit']"
                      link
                      :disabled="busy"
                      @click="result(row, row.right_id)"
                      >右方胜</el-button
                    ><el-button
                      v-if="row.winner_id"
                      v-hasPermi="['biz:pk:edit']"
                      link
                      type="danger"
                      :disabled="busy"
                      @click="result(row, null)"
                      >撤销</el-button
                    ></template
                  ></template
                ></el-table-column
              ></el-table
            >
          </el-card>
        </el-tab-pane>
        <el-tab-pane label="小组管理" name="groups">
          <el-input-number v-model="groupSize" :min="2" :max="6" /><el-button
            v-hasPermi="['biz:pk:edit']"
            :disabled="busy"
            style="margin-left: 12px"
            @click="regroup"
            >随机重新分组</el-button
          ><el-button v-hasPermi="['biz:pk:edit']" @click="addGroup"
            >新增小组</el-button
          >
          <el-table :data="groups"
            ><el-table-column prop="group_name" label="小组" /><el-table-column
              label="操作"
              ><template #default="{ row }"
                ><el-button
                  v-hasPermi="['biz:pk:edit']"
                  link
                  type="danger"
                  @click="removeGroup(row)"
                  >删除并转移成员</el-button
                ></template
              ></el-table-column
            ><el-table-column label="成员"
              ><template #default="{ row }">{{
                row.members.map((m) => m.nick_name).join("、")
              }}</template></el-table-column
            ></el-table
          >
          <el-form inline style="margin-top: 16px"
            ><el-form-item label="调整学生"
              ><el-select v-model="moveStudent" style="width: 160px"
                ><el-option
                  v-for="st in students"
                  :key="st.user_id"
                  :value="String(st.user_id)"
                  :label="st.nick_name" /></el-select></el-form-item
            ><el-form-item label="目标小组"
              ><el-select v-model="moveGroup" style="width: 160px"
                ><el-option
                  v-for="g in groups"
                  :key="g.group_id"
                  :value="String(g.group_id)"
                  :label="g.group_name" /></el-select></el-form-item
            ><el-button
              v-hasPermi="['biz:pk:edit']"
              :disabled="busy || !moveStudent || !moveGroup"
              @click="move"
              >调整</el-button
            ></el-form
          >
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from "vue";
import { ElMessageBox } from "element-plus";
import { options, listCatalog, listUsers } from "@/api/education";
import { learningGet, learningPost } from "@/api/learning";
const { biz_pk_dimension } =
  getCurrentInstance().proxy.useDict("biz_pk_dimension");
const performanceDimensions = computed(() =>
  biz_pk_dimension.value.map((d) => d.label),
);
const query = reactive({
  class_id: "",
  courseware_id: "",
  mode: "PERSONAL",
  stage: "BASIC",
});
const classes = ref([]),
  courses = ref([]),
  students = ref([]),
  ranking = ref([]),
  groups = ref([]),
  rounds = ref([]),
  tab = ref("performance"),
  busy = ref(false),
  groupSize = ref(4),
  moveStudent = ref(""),
  moveGroup = ref("");
const selectedCourse = computed(() =>
  courses.value.find((c) => String(c.courseware_id) === query.courseware_id),
);
function participant(id) {
  return query.mode === "GROUP"
    ? groups.value.find((g) => String(g.group_id) === String(id))?.group_name ||
        `历史小组 ${id}`
    : students.value.find((s) => String(s.user_id) === String(id))?.nick_name ||
        `学生 ${id}`;
}
async function findCourses(keyword = "") {
  courses.value = (
    await listCatalog("courseware", {
      page: 1,
      size: 100,
      keyword,
      status: "PUBLISHED",
    })
  ).rows;
}
async function loadRounds() {
  if (query.class_id && query.courseware_id)
    rounds.value = (await learningGet("pk", query)).data;
}
async function refresh() {
  if (!query.class_id || !query.courseware_id) return;
  const all = [];
  let page = 1;
  while (true) {
    const r = await listUsers({
      class_id: query.class_id,
      role_key: "edu_student",
      page,
      size: 100,
    });
    all.push(...r.rows);
    if (all.length >= Number(r.total)) break;
    page++;
  }
  students.value = all;
  groups.value = (
    await learningGet("groups", { class_id: query.class_id })
  ).data;
  ranking.value = (await learningGet("ranking", query)).data;
  await loadRounds();
}
async function action(fn) {
  busy.value = true;
  try {
    await fn();
    await refresh();
  } finally {
    busy.value = false;
  }
}
async function adjust(row, dimension, score) {
  await action(() =>
    learningPost("scores", {
      student_id: row.user_id,
      courseware_id: query.courseware_id,
      stage: "BASIC",
      score,
      source: dimension,
      event_key: crypto.randomUUID(),
    }),
  );
}
async function allPlus() {
  await ElMessageBox.confirm("为当前班级全体学生在此课件加1分？", "全体加分");
  await action(() => learningPost("scores/all-plus", query));
}
function modeChanged() {
  query.stage = "BASIC";
  loadRounds();
}
async function match() {
  await action(() => learningPost("pk/match", query));
}
async function result(row, winner) {
  await action(() =>
    learningPost(`pk/${row.match_id}/result`, { winner_id: winner }),
  );
}
async function clear() {
  await ElMessageBox.confirm(
    "清空赛程，已加积分保留。确认继续？",
    "清空本轮数据",
  );
  await action(() => learningPost("pk/clear", query));
}
async function regroup() {
  await ElMessageBox.confirm(
    "重新分组后旧分组保留为历史。确认继续？",
    "随机分组",
  );
  await action(() =>
    learningPost("groups/regroup", {
      class_id: query.class_id,
      size: groupSize.value,
    }),
  );
}
async function addGroup() {
  const { value } = await ElMessageBox.prompt("请输入小组名称", "新增小组");
  await action(() =>
    learningPost("groups/add", { class_id: query.class_id, group_name: value }),
  );
}
async function removeGroup(row) {
  const { value } = await ElMessageBox.prompt(
    "请输入接收成员的另一个当前小组ID：" +
      groups.value
        .filter((g) => g.group_id !== row.group_id)
        .map((g) => `${g.group_id} ${g.group_name}`)
        .join("、"),
    "删除小组",
  );
  await action(() =>
    learningPost("groups/remove", {
      class_id: query.class_id,
      group_id: row.group_id,
      target_group_id: value,
    }),
  );
}
async function move() {
  await action(() =>
    learningPost("groups/move", {
      class_id: query.class_id,
      student_id: moveStudent.value,
      group_id: moveGroup.value,
    }),
  );
}
onMounted(async () => {
  classes.value = (await options()).data.classes;
  await findCourses();
});
</script>
