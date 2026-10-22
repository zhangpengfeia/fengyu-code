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
elif [[ "$*" == *"__release.txt"* ]]; then
  cat "$MOCK_ACTIVE_TAG"
fi
""",
        )
        self.tool(
            "docker",
            """
if [[ "$*" == *"pull fengyucode-web"* && "${FAIL_PULL:-0}" == 1 ]]; then exit 1; fi
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
        for name in ("compose.yaml", "pipeline-deploy.sh"):
            shutil.copyfile(SOURCE / name, directory / name)
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

    def test_existing_directory_is_preserved(self):
        release = self.release(1)
        (self.base / "current").mkdir()
        result = self.run_deploy(release)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.base / "current").is_symlink())


if __name__ == "__main__":
    unittest.main()
