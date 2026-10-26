import assert from "node:assert/strict";
import test from "node:test";
import { randomUUID } from "node:crypto";
import {
  WIDGET_PROTOCOL,
  isMockGameContext,
  isWidgetMessage,
  widgetOrigin,
} from "@fengyu/agent-widget-sdk/protocol";

const context = () => ({
  mode: "mock",
  requestId: randomUUID(),
  student: { id: "mock-student-001", name: "联调同学", schoolId: "11000" },
  game: {
    code: "eight-pieces",
    name: "八子归位",
    volume: 5,
    mode: "ASSESSMENT",
    difficulty: "NOVICE",
  },
  result: {
    durationMs: 142000,
    actualSteps: 49,
    optimalSteps: 28,
    badge: "SILVER",
    passed: true,
  },
});

test("widget channel rejects foreign protocols and stale frame channels", () => {
  const message = {
    protocol: WIDGET_PROTOCOL,
    channelId: "current-frame",
    type: "context",
    context: context(),
  };
  assert.equal(isWidgetMessage(message, "current-frame"), true);
  assert.equal(isWidgetMessage(message, "previous-frame"), false);
  assert.equal(
    isWidgetMessage({ ...message, protocol: "foreign" }, "current-frame"),
    false,
  );
  assert.equal(
    isWidgetMessage({ ...message, type: "execute" }, "current-frame"),
    false,
  );
});
test("host context validates typed scores, valid rounds and consistent medals", () => {
  const valid = context();
  assert.equal(isMockGameContext(valid), true);
  assert.equal(
    isMockGameContext({
      ...valid,
      result: { ...valid.result, actualSteps: "49" },
    }),
    false,
  );
  assert.equal(
    isMockGameContext({
      ...valid,
      result: { ...valid.result, durationMs: -1 },
    }),
    false,
  );
  assert.equal(
    isMockGameContext({ ...valid, result: { ...valid.result, passed: false } }),
    false,
  );
  assert.equal(
    isMockGameContext({
      ...valid,
      game: { ...valid.game, code: "../invalid" },
    }),
    false,
  );
  assert.equal(
    isMockGameContext({ ...valid, requestId: "not-a-round" }),
    false,
  );
});
test("widget address is pinned to a web origin and excludes executable URLs", () => {
  assert.equal(
    widgetOrigin("http://127.0.0.1:5176/widget", "https://school.example"),
    "http://127.0.0.1:5176",
  );
  assert.throws(() =>
    widgetOrigin("javascript:alert(1)", "https://school.example"),
  );
  assert.throws(() =>
    widgetOrigin(
      "https://user:secret@school.example/widget",
      "https://school.example",
    ),
  );
});
