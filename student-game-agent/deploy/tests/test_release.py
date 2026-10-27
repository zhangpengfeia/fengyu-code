"""Exercise release ordering and recovery without contacting Docker or a server."""

import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

import yaml

DEPLOY = Path(__file__).resolve().parents[1]
ROOT = DEPLOY.parents[1]


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="agent-release-")
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name).resolve()
        self.bin = self.base / "bin"
        self.bin.mkdir()
        (self.base / ".pipeline").mkdir()
        (self.base / ".pipeline/agent.env").write_text("POSTGRES_PASSWORD=" + "a" * 64 + "\n")
        self.env = {
            **os.environ,
            "PATH": f"{self.bin}:{os.environ['PATH']}",
            "TEST_BASE": str(self.base),
        }
        self.stub("flock", "#!/usr/bin/env bash\nexit 0\n")
        self.stub(
            "docker",
            """#!/usr/bin/env python3
import os, pathlib, sys
base = pathlib.Path(os.environ['TEST_BASE'])
args = sys.argv[1:]
with (base / 'calls').open('a') as stream:
    stream.write(' '.join(args) + '\\n')
if '-f' not in args: sys.exit(0)
directory = pathlib.Path(args[args.index('-f') + 1]).parent
values = dict(line.split('=', 1) for line in (directory / '.env').read_text().splitlines())
if 'up' in args and 'agent-api' in args:
    if os.environ.get('FAIL_TAG') == values['IMAGE_TAG']: sys.exit(7)
    (base / 'active').write_text(values['IMAGE_TAG'])
""",
        )
        self.stub(
            "curl",
            """#!/usr/bin/env python3
import os, pathlib, sys
args = sys.argv[1:]
if '-w' in args:
    print('404', end=''); sys.exit(0)
if '__release.txt' in args[-1]:
    base = pathlib.Path(os.environ['TEST_BASE'])
    marker = 'wrong-release' if os.environ.get('BAD_MARKER') else (base / 'active').read_text()
    print(marker, end='')
""",
        )

    def stub(self, name, contents):
        path = self.bin / name
        path.write_text(contents)
        path.chmod(0o700)

    def release(self, number):
        tag = f"sha-{'a' * 40}-{number}-1"
        directory = self.base / "releases" / tag / "deploy"
        directory.mkdir(parents=True)
        shutil.copy(DEPLOY / "pipeline-deploy.sh", directory)
        shutil.copy(DEPLOY / "compose.release.yaml", directory / "compose.yaml")
        return tag, directory

    def run_release(self, tag, directory, **changes):
        return subprocess.run(
            [
                "bash",
                str(directory / "pipeline-deploy.sh"),
                "deploy",
                str(self.base),
                "ghcr.io/example/repo",
                tag,
                "crm-ai_default",
                "18089",
            ],
            env={**self.env, **changes},
            capture_output=True,
            text=True,
        )

    def test_migrations_precede_switch_and_marker_precedes_current(self):
        tag, directory = self.release(1)
        result = self.run_release(tag, directory)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.base / "current").resolve(), directory)
        calls = (self.base / "calls").read_text()
        self.assertLess(
            calls.index("run --rm --no-deps migrate"),
            calls.index("180 agent-api worker student-web"),
        )
        self.assertNotIn(" down", calls)

    def test_failed_switch_restores_old_images_and_current(self):
        first, old = self.release(1)
        self.assertEqual(self.run_release(first, old).returncode, 0)
        tag, directory = self.release(2)
        result = self.run_release(tag, directory, FAIL_TAG=tag)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.base / "current").resolve(), old)
        self.assertEqual((self.base / "active").read_text(), first)
        self.assertIn("Restoring", result.stderr)

    def test_wrong_release_marker_rejects_first_deploy(self):
        tag, directory = self.release(1)
        result = self.run_release(tag, directory, BAD_MARKER="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.base / "current").exists())
        self.assertIn("stop agent-api worker student-web", (self.base / "calls").read_text())

    def test_explicit_rollback_switches_previous_verified_release(self):
        first, old = self.release(1)
        self.assertEqual(self.run_release(first, old).returncode, 0)
        tag, directory = self.release(2)
        self.assertEqual(self.run_release(tag, directory).returncode, 0)
        result = subprocess.run(
            ["bash", str(directory / "pipeline-deploy.sh"), "rollback", str(self.base)],
            env=self.env,
            capture_output=True,
            text=True,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.base / "current").resolve(), old)
        self.assertEqual((self.base / "previous").resolve(), directory)

    def test_missing_credentials_fails_before_container_changes(self):
        (self.base / ".pipeline/agent.env").unlink()
        tag, directory = self.release(1)
        self.assertNotEqual(self.run_release(tag, directory).returncode, 0)
        self.assertNotIn(" up ", (self.base / "calls").read_text())


class ConfigurationTests(unittest.TestCase):
    def test_production_isolation_and_prefix(self):
        config = yaml.safe_load((DEPLOY / "compose.release.yaml").read_text())
        services = config["services"]
        for name in ("agent-api", "worker", "migrate"):
            self.assertEqual(services[name]["environment"]["AGENT_ENVIRONMENT"], "production")
            self.assertEqual(services[name]["environment"]["AGENT_MOCK_MODE"], "false")
            self.assertNotIn("ports", services[name])
        self.assertNotIn("ports", services["postgres"])
        self.assertTrue(services["student-web"]["ports"][0].startswith("127.0.0.1:"))
        nginx = (ROOT / "student-web/deploy/nginx.agent.conf").read_text()
        self.assertIn("(internal|dev)", nginx)
        self.assertIn("/student/index.html", nginx)
        self.assertIn("proxy_buffering off", nginx)


if __name__ == "__main__":
    unittest.main()
