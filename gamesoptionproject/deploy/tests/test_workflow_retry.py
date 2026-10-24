"""The legacy source workflow must not overwrite the education website."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT.parent / ".github/workflows/deploy.yml"

class LegacyWorkflowTests(unittest.TestCase):
    def test_planet_source_checks_do_not_publish_or_deploy(self):
        workflow=WORKFLOW.read_text()
        self.assertIn("working-directory: gamesoptionproject",workflow)
        self.assertIn("npm run build",workflow)
        self.assertNotIn("docker push",workflow)
        self.assertNotIn("DEPLOY_SSH_KEY",workflow)
        self.assertNotIn("name: production",workflow)
        self.assertTrue((ROOT.parent / ".github/workflows/education.yml").is_file())

if __name__ == "__main__": unittest.main()
