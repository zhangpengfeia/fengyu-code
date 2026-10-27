"""Exercise real release switching with fake Docker/curl, without a server."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SOURCE = Path(__file__).resolve().parents[1]
PREFIX = "ghcr.io/zhangpengfeia/fengyu-code"


class DeploymentTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="fengyu-deploy-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        self.base = self.root / "site"
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.log = self.root / "calls"
        self.env = os.environ | {
            "PATH": f"{self.bin}:{os.environ['PATH']}",
            "CALL_LOG": str(self.log),
            "MOCK_ACTIVE_TAG": str(self.root / "active-tag"),
            "AGENT_DEPLOY_BASE_DIR": str(self.root / "agent"),
            "MOCK_AGENT_BRIDGE_CREATED": str(self.root / "agent-bridge-created"),
        }
        self.tool("flock", "exit 0")
        self.tool("sleep", "exit 0")
        self.tool(
            "curl",
            """
[[ "${FAIL_CURL:-0}" != 1 ]] || exit 1
if [[ "$*" == *"https://"* && "${FAIL_HTTPS:-0}" == 1 ]]; then
  echo 'TLS handshake failed' >&2
  exit 35
fi
if [[ "$*" == *"https://"* && "${WRONG_PUBLIC_RELEASE:-0}" == 1 ]]; then
  echo wrong-release
elif [[ "$*" == *"/prod-api/biz/auth/status"* ]]; then
  if [[ "${WRONG_API_RELEASE:-0}" == 1 ]]; then printf '{"status":"UP","release":"wrong"}';
  else printf '{"status":"UP","release":"%s"}' "$(cat "$MOCK_ACTIVE_TAG")"; fi
elif [[ "$*" == *"/play/planet/index.html"* ]]; then
  printf '%s' "${GAME_STATUS:-403}"
elif [[ "$*" == *"__release.txt"* ]]; then
  cat "$MOCK_ACTIVE_TAG"
fi
""",
        )
        self.tool(
            "docker",
            """
if [[ "$1 ${2:-}" == 'network inspect' && "$*" == *fengyucode-agent-bridge* ]]; then
  if [[ "${MISSING_AGENT_BRIDGE:-0}" == 1 && ! -f "$MOCK_AGENT_BRIDGE_CREATED" ]]; then exit 1; fi
  if [[ "$*" == *--format* ]]; then printf '%s' "${AGENT_BRIDGE_METADATA:-true bridge}"; fi
fi
if [[ "$1 ${2:-}" == 'network create' ]]; then touch "$MOCK_AGENT_BRIDGE_CREATED"; fi
if [[ "$*" == *"pull"* && "${FAIL_PULL:-0}" == 1 ]]; then exit 1; fi
if [[ "$*" == *"up -d"* && "$PWD" == *"${FAIL_RELEASE:-never-match}"* ]]; then exit 1; fi
if [[ "$*" == *"up -d"* ]]; then sed -n 's/^IMAGE_TAG=//p' .env > "$MOCK_ACTIVE_TAG"; fi
exit 0
""",
        )

    def tool(self, name, body):
        file = self.bin / name
        file.write_text(
            '#!/usr/bin/env bash\nset -eu\nprintf "%s %s %s\\n" "$(basename "$0")" "$PWD" "$*" >> "$CALL_LOG"\n'
            + body
            + "\n"
        )
        file.chmod(0o755)

    def release(self, number):
        tag = f"sha-{'a' * 40}-{number}-1"
        directory = self.base / "releases" / tag / "deploy"
        directory.mkdir(parents=True)
        for name in ("compose.release.yaml", "pipeline-deploy.sh", "education.release"):
            shutil.copyfile(SOURCE / name, directory / ("compose.yaml" if name == "compose.release.yaml" else name))
        return tag, directory

    def run_deploy(self, release, **env):
        tag, directory = release
        return subprocess.run(
            [
                "bash",
                str(directory / "pipeline-deploy.sh"),
                "deploy",
                str(self.base),
                PREFIX,
                tag,
                "crm-ai_default",
                "18088",
            ],
            env=self.env | env,
            text=True,
            capture_output=True,
        )

    def test_first_deployment_records_current(self):
        release = self.release(1)
        result = self.run_deploy(release)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.base / "current").resolve(), release[1])
        self.assertTrue((self.base / ".pipeline/rollback.sh").exists())
        self.assertIn("--project-name fengyucode", self.log.read_text())
        self.assertNotIn("--project-name crm-ai", self.log.read_text())

    def test_private_agent_bridge_is_created_and_validated(self):
        result = self.run_deploy(self.release(1), MISSING_AGENT_BRIDGE="1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("network create --driver bridge --internal fengyucode-agent-bridge", self.log.read_text())

    def test_public_or_incompatible_agent_network_is_rejected(self):
        for metadata in ("false bridge", "true overlay"):
            result = self.run_deploy(self.release(1 if metadata.startswith("false") else 2),
                                     AGENT_BRIDGE_METADATA=metadata)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("must be an internal Docker bridge", result.stderr)
            self.assertNotIn("up -d", self.log.read_text())

    def test_missing_agent_config_keeps_first_education_deploy_available(self):
        result = self.run_deploy(self.release(1))
        self.assertEqual(result.returncode, 0, result.stderr)
        bridge = self.base / ".pipeline/agent-bridge.env"
        self.assertTrue(bridge.exists())
        self.assertNotIn("EDU_AGENT_SERVICE_API_KEY=", bridge.read_text())
        self.assertEqual(bridge.stat().st_mode & 0o777, 0o600)

    def test_only_integration_key_is_copied_without_rotating_existing_credentials(self):
        source = self.root / "agent/.pipeline/agent.env"
        source.parent.mkdir(parents=True)
        integration = "integration-" + "b" * 50
        jwt = "signing-" + "c" * 50
        database = "database-" + "d" * 50
        contents = f"AGENT_SERVICE_API_KEY={integration}\nAGENT_JWT_SECRET={jwt}\nPOSTGRES_PASSWORD={database}\n"
        source.write_text(contents)
        first = self.run_deploy(self.release(1))
        self.assertEqual(first.returncode, 0, first.stderr)
        credentials = self.base / ".pipeline/education.env"
        original = credentials.read_text()
        bridge = self.base / ".pipeline/agent-bridge.env"
        self.assertEqual(bridge.read_text(), f'EDU_AGENT_SERVICE_API_KEY="{integration}"\n')
        self.assertNotIn(jwt, bridge.read_text())
        self.assertNotIn(database, bridge.read_text())
        self.assertEqual(bridge.stat().st_mode & 0o777, 0o600)
        second = self.run_deploy(self.release(2))
        self.assertEqual(second.returncode, 0, second.stderr)
        self.assertEqual(credentials.read_text(), original)
        self.assertEqual(source.read_text(), contents)
        for secret in (integration, jwt, database):
            self.assertNotIn(secret, first.stdout + first.stderr + second.stdout + second.stderr)

    def test_missing_source_never_removes_existing_bridge_key(self):
        bridge = self.base / ".pipeline/agent-bridge.env"
        bridge.parent.mkdir(parents=True)
        original = 'EDU_AGENT_SERVICE_API_KEY="' + "a" * 64 + '"\n'
        bridge.write_text(original)
        result = self.run_deploy(self.release(1))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(bridge.read_text(), original)

    def test_invalid_agent_key_cannot_replace_existing_bridge_key(self):
        source = self.root / "agent/.pipeline/agent.env"
        source.parent.mkdir(parents=True)
        source.write_text("AGENT_SERVICE_API_KEY=short\n")
        bridge = self.base / ".pipeline/agent-bridge.env"
        bridge.parent.mkdir(parents=True)
        original = 'EDU_AGENT_SERVICE_API_KEY="' + "a" * 64 + '"\n'
        bridge.write_text(original)
        result = self.run_deploy(self.release(1))
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(bridge.read_text(), original)
        self.assertNotIn("up -d", self.log.read_text())

    def test_failed_pull_leaves_previous_container_untouched(self):
        old = self.release(1)
        self.assertEqual(self.run_deploy(old).returncode, 0)
        self.log.write_text("")
        result = self.run_deploy(self.release(2), FAIL_PULL="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.base / "current").resolve(), old[1])
        self.assertNotIn("up -d", self.log.read_text())

    def test_failed_start_restores_previous_image(self):
        old = self.release(1)
        self.assertEqual(self.run_deploy(old).returncode, 0)
        new = self.release(2)
        result = self.run_deploy(new, FAIL_RELEASE=new[0])
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.base / "current").resolve(), old[1])
        self.assertIn("Previous release restored", result.stderr)

    def test_first_failed_release_is_removed(self):
        new = self.release(1)
        result = self.run_deploy(new, FAIL_RELEASE=new[0])
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.base / "current").exists())
        self.assertIn("--project-name fengyucode down", self.log.read_text())

    def test_failed_health_does_not_record_new_current(self):
        old = self.release(1)
        self.assertEqual(self.run_deploy(old).returncode, 0)
        result = self.run_deploy(self.release(2), FAIL_CURL="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.base / "current").resolve(), old[1])
        self.assertIn("Restoring the previous web image", result.stderr)

    def test_wrong_caddy_route_cannot_pass_with_a_healthy_container(self):
        release = self.release(1)
        result = self.run_deploy(release, WRONG_PUBLIC_RELEASE="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.base / "current").exists())
        calls = self.log.read_text()
        self.assertIn("http://127.0.0.1:18088/__release.txt", calls)
        self.assertIn("https://fengyucode.zhangff.cn/__release.txt", calls)
        self.assertIn("--project-name fengyucode down", calls)

    def test_tls_failure_reports_the_cause_before_cleanup(self):
        result = self.run_deploy(self.release(1), FAIL_HTTPS="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Container version verified", result.stdout)
        self.assertIn("Caddy HTTPS request failed: TLS handshake failed", result.stderr)
        self.assertIn("Publish the fengyucode site block", result.stderr)
        self.assertFalse((self.base / "current").exists())

    def test_manual_rollback_swaps_current_and_previous(self):
        old, new = self.release(1), self.release(2)
        self.assertEqual(self.run_deploy(old).returncode, 0)
        self.assertEqual(self.run_deploy(new).returncode, 0)
        result = subprocess.run(
            ["bash", str(self.base / ".pipeline/rollback.sh"), "rollback", str(self.base)],
            env=self.env,
            text=True,
            capture_output=True,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.base / "current").resolve(), old[1])
        self.assertEqual((self.base / "previous").resolve(), new[1])

    def test_api_version_mismatch_rolls_back(self):
        old=self.release(1)
        self.assertEqual(self.run_deploy(old).returncode,0)
        result=self.run_deploy(self.release(2),WRONG_API_RELEASE="1")
        self.assertNotEqual(result.returncode,0)
        self.assertEqual((self.base / "current").resolve(),old[1])
        self.assertIn("Education API version mismatch",result.stderr)

    def test_public_game_assets_cannot_pass_readiness(self):
        result=self.run_deploy(self.release(1),GAME_STATUS="200")
        self.assertNotEqual(result.returncode,0)
        self.assertIn("Anonymous planet access must return 403",result.stderr)

    def test_credentials_are_generated_once_and_never_printed(self):
        first=self.run_deploy(self.release(1));self.assertEqual(first.returncode,0,first.stderr)
        file=self.base / ".pipeline/education.env";contents=file.read_text()
        self.assertEqual(file.stat().st_mode & 0o777,0o600)
        self.assertEqual(self.run_deploy(self.release(2)).returncode,0)
        self.assertEqual(file.read_text(),contents)
        for line in contents.splitlines(): self.assertNotIn(line.split("=",1)[1],first.stdout+first.stderr)

    def test_existing_test_admin_password_is_updated_without_rotating_other_secrets(self):
        release = self.release(1)
        credentials = self.base / ".pipeline/education.env"
        credentials.parent.mkdir(parents=True)
        credentials.write_text(
            "EDU_ADMIN_PASSWORD=old-test-password\n"
            "EDU_DEMO_PASSWORD=existing-demo-secret\n"
            "EDU_TOKEN_SECRET=existing-token-secret\n"
        )
        result = self.run_deploy(release)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(credentials.read_text().splitlines(), [
            "EDU_DEMO_PASSWORD=existing-demo-secret",
            "EDU_TOKEN_SECRET=existing-token-secret",
            "EDU_ADMIN_PASSWORD=admin123456",
        ])
        self.assertEqual(credentials.stat().st_mode & 0o777, 0o600)
        self.assertNotIn("admin123456", result.stdout + result.stderr)

    def test_failed_first_migration_restores_legacy_release(self):
        old=self.release(1)
        (old[1] / "education.release").unlink()
        (old[1] / ".env").write_text(f"IMAGE_TAG={old[0]}\nHTTP_PORT=18088\n")
        (self.base / "current").symlink_to(old[1])
        new=self.release(2)
        result=self.run_deploy(new,FAIL_RELEASE=new[0])
        self.assertNotEqual(result.returncode,0)
        self.assertEqual((self.base / "current").resolve(),old[1])
        self.assertIn("Previous release restored",result.stderr)
        self.assertIn("--remove-orphans",self.log.read_text())

    def test_existing_directory_is_preserved(self):
        release = self.release(1)
        (self.base / "current").mkdir()
        result = self.run_deploy(release)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.base / "current").is_symlink())


if __name__ == "__main__":
    unittest.main()
