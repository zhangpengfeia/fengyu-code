import assert from "node:assert/strict";
import { test } from "node:test";
import { openLiveSession } from "../src/widget/frame/live-session";
import {
  agentSession,
  getAgentTokenProvider,
  registerAgentTokenProvider,
  type AgentTokenResult,
} from "../src/shared/api/session";

const validSession = {
  access_token: "server-issued-game-session-token",
  expires_in: 900,
  student: { id: "student-1", name: "同学", school_id: "school-1" },
};

function jsonResponse(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

test("live widget exchanges only its game cookie and discards unsupported report fields", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, init) => {
    assert.equal(url, "/play/planet/agent-session");
    assert.equal(init?.method, "POST");
    assert.equal(init?.credentials, "same-origin");
    assert.equal(init?.cache, "no-store");
    assert.equal(init?.body, undefined);
    assert.equal(new Headers(init?.headers).has("Authorization"), false);
    return jsonResponse({
      ...validSession,
      evaluation_id: "untrusted-report-id",
      facts: { actual_steps: 999 },
    });
  });
  const result = await openLiveSession(new AbortController().signal);
  assert.deepEqual(result, {
    ...validSession,
    mock: false,
    demo: false,
    evaluation_id: null,
  });
  assert.equal("facts" in result, false);
});

test("demo replies retain the authenticated student without accepting mock bootstrap facts", async (t) => {
  t.mock.method(globalThis, "fetch", async () =>
    jsonResponse({
      ...validSession,
      demo: true,
      evaluation_id: "not-a-real-report",
      facts: { actual_steps: 36 },
    }),
  );
  const result = await openLiveSession(new AbortController().signal);
  assert.equal(result.demo, true);
  assert.equal(result.mock, false);
  assert.deepEqual(result.student, validSession.student);
  assert.equal(result.evaluation_id, null);
  assert.equal("facts" in result, false);
});

test("live widget rejects mock responses and invalid server identity or expiry", async (t) => {
  const responses = [
    { ...validSession, mock: true },
    { ...validSession, mock: true, demo: true },
    { ...validSession, demo: "true" },
    { ...validSession, expires_in: 0 },
    { ...validSession, expires_in: "900" },
    { ...validSession, student: { ...validSession.student, id: 1 } },
    { ...validSession, student: { ...validSession.student, school_id: "" } },
  ];
  t.mock.method(globalThis, "fetch", async () =>
    jsonResponse(responses.shift()),
  );
  while (responses.length)
    await assert.rejects(
      openLiveSession(new AbortController().signal),
      /未返回有效的学伴会话/,
    );
});

test("live widget preserves actionable Chinese permission and configuration errors", async (t) => {
  const responses = [
    jsonResponse({ code: 403, msg: "请通过已授权的游戏菜单进入。" }, 403),
    jsonResponse({ code: 503 }, 503),
  ];
  t.mock.method(globalThis, "fetch", async () => responses.shift()!);
  await assert.rejects(
    openLiveSession(new AbortController().signal),
    /请通过已授权的游戏菜单进入/,
  );
  await assert.rejects(
    openLiveSession(new AbortController().signal),
    /服务暂未配置完成/,
  );
});

test("live widget cancellation aborts the actual exchange request", async (t) => {
  t.mock.method(globalThis, "fetch", async (_url, init) => {
    return new Promise<Response>((_resolve, reject) => {
      init?.signal?.addEventListener("abort", () =>
        reject(new DOMException("Cancelled", "AbortError")),
      );
    });
  });
  const controller = new AbortController();
  const pending = openLiveSession(controller.signal);
  controller.abort();
  await assert.rejects(pending, { name: "AbortError" });
});

test("widget token refresh is shared and cannot fall through to another login", async () => {
  const result = deferred<AgentTokenResult>();
  let requests = 0;
  const dispose = registerAgentTokenProvider(() => {
    requests++;
    return result.promise;
  });
  try {
    const refresh = getAgentTokenProvider()!;
    const first = refresh();
    const second = refresh();
    result.resolve(validSession);
    assert.equal(await first, validSession.access_token);
    assert.equal(await second, validSession.access_token);
    assert.equal(requests, 1);
    assert.equal(agentSession.getToken(), validSession.access_token);
    agentSession.clear();
    dispose();
    await assert.rejects(refresh(), /窗口已变化/);
    assert.equal(getAgentTokenProvider(), undefined);
  } finally {
    dispose();
  }
});

test("disposed widget cannot overwrite a newer widget token or clear its owner", async () => {
  const oldResult = deferred<AgentTokenResult>();
  const disposeOld = registerAgentTokenProvider(() => oldResult.promise);
  const refreshOld = getAgentTokenProvider()!();
  // Start the exchange before changing the mounted provider owner.
  await Promise.resolve();
  const disposeNew = registerAgentTokenProvider(async () => ({
    access_token: "new-widget-token",
    expires_in: 900,
  }));
  try {
    await getAgentTokenProvider()!();
    disposeOld();
    assert.equal(agentSession.getToken(), "new-widget-token");
    oldResult.resolve(validSession);
    await assert.rejects(refreshOld, /窗口已关闭/);
    assert.equal(agentSession.getToken(), "new-widget-token");
  } finally {
    disposeOld();
    disposeNew();
  }
});

test("failed widget provider leaves no token and remains the only renewal source", async () => {
  let requests = 0;
  const dispose = registerAgentTokenProvider(async () => {
    requests++;
    throw new Error("游戏登录已过期，请重新进入星球。");
  });
  try {
    await assert.rejects(getAgentTokenProvider()!(), /游戏登录已过期/);
    await assert.rejects(getAgentTokenProvider()!(), /游戏登录已过期/);
    assert.equal(requests, 2);
    assert.equal(agentSession.getToken(), "");
    assert.equal(typeof getAgentTokenProvider(), "function");
  } finally {
    dispose();
  }
});
