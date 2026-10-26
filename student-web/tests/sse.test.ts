import assert from "node:assert/strict";
import test from "node:test";
import {
  createSseParser,
  readEventStream,
  type ServerEvent,
} from "../src/features/companion/sse.ts";

test("SSE handles CRLF split across chunks, comments and multiline data", () => {
  const events: ServerEvent[] = [];
  const parser = createSseParser((value) => events.push(value));
  for (const chunk of [
    ": heartbeat\r",
    "\nevent: answer\r",
    "\ndata: first\r\ndata: second\r",
    "\n\r",
    "\nevent: done\ndata: {}\n\n",
  ])
    parser.push(chunk);
  parser.finish();
  assert.deepEqual(events, [
    { event: "answer", data: "first\nsecond" },
    { event: "done", data: "{}" },
  ]);
});

test("SSE preserves UTF-8 characters even when bytes arrive individually", async () => {
  const bytes = new TextEncoder().encode(
    'event: answer\ndata: {"answer":"你好，继续探索吧"}\n\n',
  );
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      for (const value of bytes) controller.enqueue(Uint8Array.of(value));
      controller.close();
    },
  });
  const events: ServerEvent[] = [];
  await readEventStream(new Response(stream), (value) => events.push(value));
  assert.equal(JSON.parse(events[0]!.data).answer, "你好，继续探索吧");
});

test("consumer errors propagate and cancel the unread network body", async () => {
  let cancelled = false;
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      controller.enqueue(
        new TextEncoder().encode(
          'event: error\ndata: {"message":"规则暂不可用"}\n\n',
        ),
      );
    },
    cancel() {
      cancelled = true;
    },
  });
  await assert.rejects(
    readEventStream(new Response(stream), (event) => {
      throw new Error(JSON.parse(event.data).message);
    }),
    /规则暂不可用/,
  );
  assert.equal(cancelled, true);
});
