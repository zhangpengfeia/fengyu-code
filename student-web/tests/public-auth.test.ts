import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import vm from "node:vm";
import ts from "typescript";

function loadModule(path: string, dependencies: Record<string, unknown>) {
  const source = readFileSync(new URL(path, import.meta.url), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true },
  }).outputText;
  const exports: Record<string, any> = {};
  vm.runInNewContext(compiled, {
    exports,
    require: (name: string) => {
      assert.ok(name in dependencies, `Unexpected dependency: ${name}`);
      return dependencies[name];
    },
  });
  return exports;
}

test("login and captcha never carry a previous session; protected requests still do", async () => {
  const requests: any[] = [];
  const client = loadModule("../src/shared/api/client.ts", {
    axios: { request: async (options: any) => {
      requests.push(options);
      return { data: { code: 200, data: {} } };
    } },
    "@/shared/config": { config: { educationBaseUrl: "/education-api" } },
    "./session": { educationSession: { getToken: () => "old-password-change-session" } },
  });
  const api = loadModule("../src/features/auth/api.ts", {
    "@/shared/api/client": client,
  });
  await api.getCaptcha();
  await api.signIn({ mode: "ACCOUNT", username: "admin", password: "test-password" });
  await client.educationRequest({ url: "/biz/options" });
  assert.equal(requests[0].url, "/captchaImage");
  assert.equal(requests[1].url, "/biz/auth/login");
  assert.equal(requests[0].headers.Authorization, undefined);
  assert.equal(requests[1].headers.Authorization, undefined);
  assert.equal(requests[2].headers.Authorization, "Bearer old-password-change-session");
});
