import assert from "node:assert/strict";
import test from "node:test";
import { safeNavigationUrl } from "../src/shared/utils/url.ts";

test("game launch preserves single-use exchange tickets and HTTPS external URLs", () => {
  assert.equal(
    safeNavigationUrl(
      "/play/planet/exchange?ticket=one-use",
      "https://school.example",
    ),
    "https://school.example/play/planet/exchange?ticket=one-use",
  );
  assert.equal(
    safeNavigationUrl("https://games.example/play", "https://school.example"),
    "https://games.example/play",
  );
});

test("game launch rejects executable protocols and embedded credentials", () => {
  for (const value of [
    "javascript:alert(1)",
    "data:text/html,hello",
    "file:///etc/passwd",
    "https://user:password@games.example/",
  ]) {
    assert.throws(
      () => safeNavigationUrl(value, "https://school.example"),
      /游戏入口地址无效/,
    );
  }
});
