import test from "node:test";
import assert from "node:assert/strict";
import { redirectToPasswordChange } from "../src/utils/password-navigation.js";

test("initial password rejection routes to security and enforces the restriction", async () => {
  const visits = [];
  const router = {
    currentRoute: { value: { path: "/index" } },
    replace: async (path) => visits.push(path),
  };
  const store = { mustChangePwd: false };
  await redirectToPasswordChange(router, store);
  assert.equal(store.mustChangePwd, true);
  assert.deepEqual(visits, ["/education-security"]);
});

test("a rejected background request on security does not navigate again", async () => {
  const router = {
    currentRoute: { value: { path: "/education-security" } },
    replace: () => assert.fail("must not reload or navigate on the security page"),
  };
  const store = { mustChangePwd: false };
  await redirectToPasswordChange(router, store);
  await redirectToPasswordChange(router, store);
  assert.equal(store.mustChangePwd, true);
});

test("concurrent rejected requests share one navigation", async () => {
  let visits = 0;
  const router = {
    currentRoute: { value: { path: "/education/games" } },
    replace: async () => { visits++; },
  };
  await Promise.all(Array.from({ length: 5 }, () =>
    redirectToPasswordChange(router, { mustChangePwd: false }),
  ));
  assert.equal(visits, 1);
});

test("a failed navigation allows a later retry", async () => {
  let visits = 0;
  const router = {
    currentRoute: { value: { path: "/index" } },
    replace: async () => {
      if (++visits === 1) throw new Error("navigation failed");
    },
  };
  await assert.rejects(redirectToPasswordChange(router, {}));
  await redirectToPasswordChange(router, {});
  assert.equal(visits, 2);
});
