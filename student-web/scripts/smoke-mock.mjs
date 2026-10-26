import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";

const origin = process.env.STUDENT_MOCK_ORIGIN || "http://127.0.0.1:5176";
const target = new URL(origin);
assert(["127.0.0.1", "localhost"].includes(target.hostname), "仅允许验证本地 mock 服务");
const results = [];
async function request(path, options = {}) {
  const response = await fetch(new URL(path, origin), {
    ...options,
    headers: { "Content-Type": "application/json", ...options.headers },
  });
  const text = await response.text();
  const value = JSON.parse(text);
  assert(response.ok, `${path}: ${response.status} ${text}`);
  return value;
}
const captcha = await request("/education-api/captchaImage");
assert.equal(captcha.data.captchaEnabled, false);
const login = await request("/education-api/biz/auth/login", {
  method: "POST", body: JSON.stringify({ mode: "STUDENT", username: "demo", password: "demo123", school_id: "11000" }),
});
const education = { Authorization: `Bearer ${login.data.token}` };
const me = await request("/education-api/biz/auth/me", { headers: education });
assert.equal(me.data.name, "联调同学");
const options = await request("/education-api/biz/options", { headers: education });
assert.equal(options.data.classes.length, 1);
results.push("真实前端代理 → Mock 登录与学生资料");
const courses = await request("/education-api/biz/catalog/courseware", { headers: education });
assert(courses.rows.length >= 2);
const course = await request(`/education-api/biz/catalog/courseware/${courses.rows[0].courseware_id}`, { headers: education });
const catalog = await request("/education-api/biz/catalog/games", { headers: education });
const gameId = course.data.game_ids[0];
assert(catalog.rows.some(game => game.game_id === gameId));
const launch = await request(`/education-api/biz/games/${gameId}/launch`, { method: "POST", headers: education });
assert.equal(launch.data.url, `/mock-game/${gameId}`);
const ranking = await request(`/education-api/biz/learning/ranking?class_id=${options.data.classes[0].class_id}`, { headers: education });
assert(ranking.data.length > 0);
const exchange = await request("/education-api/biz/auth/agent-token", { method: "POST", headers: education });
const agent = { Authorization: `Bearer ${exchange.data.access_token}` };
const games = await request("/agent-api/api/v1/games", { headers: agent });
assert(games.games.some(game => game.code === "eight-pieces"));
results.push("课程、游戏目录与后端令牌桥接");
const body = { request_id: randomUUID(), question: "你好" };
const answer = await request("/agent-api/api/v1/chat", { method: "POST", headers: agent, body: JSON.stringify(body) });
assert.equal(answer.scene, "GENERAL_CHAT");
assert.equal(answer.model, "mock-local");
const repeat = await request("/agent-api/api/v1/chat", { method: "POST", headers: agent, body: JSON.stringify(body) });
assert.deepEqual(repeat, answer);
const history = await request(`/agent-api/api/v1/conversations/${answer.conversation_id}/messages`, { headers: agent });
assert(history.turns.some(turn => turn.answer === answer.answer));
const gameAnswer = await request("/agent-api/api/v1/chat", {
  method: "POST", headers: agent, body: JSON.stringify({ request_id: randomUUID(), conversation_id: answer.conversation_id, question: "八子归位怎么玩？" }),
});
assert.equal(gameAnswer.scene, "GAME_QA");
assert.equal(gameAnswer.game_rule_version, "1.0.0");
results.push("普通问答、按需游戏规则、会话与幂等");
const reports = await request("/agent-api/api/v1/evaluations", { headers: agent });
assert(reports.length >= 2 && reports.every(report => report.report?.generation_mode === "MOCK"));
const ended = await request("/agent-api/dev/mock/game-finish", {
  method: "POST", headers: agent, body: JSON.stringify({ request_id: randomUUID(), game_code: "eight-pieces" }),
});
let report;
for (let index = 0; index < 60; index++) {
  report = await request(`/agent-api/api/v1/evaluations/${ended.job_id}`, { headers: agent });
  if (report.status === "COMPLETED") break;
  assert.notEqual(report.status, "FAILED");
  await new Promise(resolve => setTimeout(resolve, 100));
}
assert.equal(report.status, "COMPLETED");
assert.equal(report.report.generation_mode, "MOCK");
results.push("结束本局 → 后台任务 → 新报告持久化");
const stream = await fetch(new URL("/agent-api/api/v1/chat/stream", origin), {
  method: "POST", headers: { ...agent, "Content-Type": "application/json" },
  body: JSON.stringify({ request_id: randomUUID(), evaluation_id: ended.job_id, question: "我这局为什么只有铜徽章？" }),
});
const events = await stream.text();
assert(stream.ok && events.includes("event: answer") && events.includes("REPORT_QA"));
assert(events.includes("event: done"));
results.push("SSE 报告追问：使用已有报告，不重复测评");
await request("/education-api/logout", { method: "POST", headers: education });
console.log(JSON.stringify({ status: "PASS", origin, checks: results }, null, 2));
