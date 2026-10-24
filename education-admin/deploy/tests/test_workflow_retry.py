"""Run the workflow's shell steps with fake SSH to check failed-job retries."""

import os
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT.parent / ".github/workflows/education.yml"


def shell_step(name):
    lines = WORKFLOW.read_text().splitlines(keepends=True)
    start = next(index for index, line in enumerate(lines) if line.strip() == f"- name: {name}")
    start = (
        next(index for index in range(start, len(lines)) if lines[index].strip() == "run: |") + 1
    )
    end = start
    while end < len(lines) and (lines[end].startswith("          ") or not lines[end].strip()):
        end += 1
    return textwrap.dedent("".join(lines[start:end]))


class WorkflowRetryTests(unittest.TestCase):
    def test_release_uses_existing_domain_and_publishes_both_images(self):
        workflow=WORKFLOW.read_text()
        self.assertIn("file: education-admin/deploy/Dockerfile.demo",workflow)
        self.assertIn("file: education-admin/deploy/Dockerfile.web",workflow)
        self.assertIn("/web:api-${{ steps.image.outputs.tag }}",workflow)
        self.assertIn("WEB_PORT=8080",workflow)
        self.assertIn("needs: demo-deployment",workflow)
        self.assertIn("name: production",workflow)
        self.assertIn("secrets.DEPLOY_KNOWN_HOSTS",workflow)
        self.assertNotIn("StrictHostKeyChecking=no",workflow)
        self.assertIn("cp education-admin/deploy/compose.release.yaml",workflow)

    def test_deploy_rerun_uses_original_published_image(self):
        with tempfile.TemporaryDirectory(prefix="fengyu-workflow-") as directory:
            root = Path(directory)
            output, summary, log = root / "output", root / "summary", root / "ssh-calls"
            env = os.environ | {
                "GITHUB_REPOSITORY": "ZhangPengFeia/fengyu-code",
                "GITHUB_SHA": "a" * 40,
                "GITHUB_RUN_ID": "12345",
                "GITHUB_RUN_ATTEMPT": "1",
                "GITHUB_OUTPUT": str(output),
            }
            result = subprocess.run(
                ["bash", "-euo", "pipefail", "-c", shell_step("Set immutable release identity")],
                env=env,
                text=True,
                capture_output=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            outputs = dict(line.split("=", 1) for line in output.read_text().splitlines())

            binaries = root / "bin"
            binaries.mkdir()
            for name in ("ssh", "scp", "ssh-keygen"):
                tool = binaries / name
                tool.write_text('#!/usr/bin/env bash\nprintf "%s\\n" "$*" >> "$CALL_LOG"\n')
                tool.chmod(0o755)
            # tar needs no network; it packages the real release files into the temp directory.
            env |= {
                "PATH": f"{binaries}:{env['PATH']}",
                "CALL_LOG": str(log),
                "RUNNER_TEMP": str(root),
                "GITHUB_STEP_SUMMARY": str(summary),
                "GITHUB_RUN_ATTEMPT": "2",
                "IMAGE_PREFIX": outputs["prefix"],
                "IMAGE_TAG": outputs["tag"],
                "DEPLOY_HOST": "152.136.228.231",
                "DEPLOY_USER": "root",
                "DEPLOY_SSH_KEY": "test-placeholder",
                "DEPLOY_KNOWN_HOSTS": "test-placeholder",
                "DEPLOY_PORT": "22",
                "DEPLOY_BASE_DIR": "/project/fengyucode",
                "DEPLOY_NETWORK": "crm-ai_default",
                "DEPLOY_HTTP_PORT": "18088",
            }
            result = subprocess.run(
                ["bash", "-euo", "pipefail", "-c", shell_step("Upload release and deploy")],
                cwd=ROOT.parent,
                env=env,
                text=True,
                capture_output=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            calls = log.read_text()
            published_tag = f"sha-{'a' * 40}-12345-1"
            nonexistent_retry_tag = f"sha-{'a' * 40}-12345-2"
            self.assertIn(f"/releases/{published_tag}/deploy", calls)
            self.assertNotIn(nonexistent_retry_tag, calls)
            self.assertIn(published_tag, summary.read_text())


if __name__ == "__main__":
    unittest.main()
