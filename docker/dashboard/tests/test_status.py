import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[3]
SCRIPT = ROOT / "docker/dashboard/poll-status.sh"


class StatusPollTest(unittest.TestCase):
    def run_poll(self, targets):
        if shutil.which("jq") is None:
            self.skipTest("jq is required")
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            curl = directory / "curl"
            curl.write_text("""#!/bin/sh
for argument do url=$argument; done
case "$url" in
    */up) printf 200 ;;
    */redirect) printf 302 ;;
    */http-error) printf 503; exit 22 ;;
    */timeout) exit 28 ;;
    *) exit 7 ;;
esac
""")
            curl.chmod(0o755)
            env = {key: value for key, value in os.environ.items()
                   if not key.startswith(("HEALTH_", "LINK_", "TITLE_", "DESCRIPTION_"))}
            env.update(targets)
            env["PATH"] = str(directory) + os.pathsep + env["PATH"]
            env["STATUS_DIR"] = str(directory / "status")
            result = subprocess.run(["sh", str(SCRIPT), "--once"], env=env,
                                    capture_output=True, text=True, timeout=10)
            self.assertEqual(result.returncode, 0, result.stderr)
            return json.loads((directory / "status/status.json").read_text())

    def test_catalogue_and_metadata_come_from_compose_environment(self):
        result = self.run_poll({
            "HEALTH_example-app": "http://example-app/up",
            "LINK_example-app": "http://localhost:1234/",
            "TITLE_example-app": 'App "Example"',
            "DESCRIPTION_example-app": "Example\nDescription",
        })

        self.assertGreater(result["updatedAt"], 0)
        self.assertEqual(result["services"], [{
            "id": "example-app", "title": 'App "Example"',
            "description": "Example\nDescription", "link": "http://localhost:1234/",
            "ready": True,
        }])

    def test_failed_checks_do_not_hide_a_healthy_service(self):
        result = self.run_poll({
            "HEALTH_up": "http://up/up",
            "HEALTH_http-error": "http://http-error/http-error",
            "HEALTH_timeout": "http://timeout/timeout",
            "HEALTH_missing": "http://missing/missing",
            "HEALTH_redirect": "http://redirect/redirect",
        })

        self.assertEqual({service["id"]: service["ready"] for service in result["services"]}, {
            "up": True, "http-error": False, "timeout": False,
            "missing": False, "redirect": False,
        })

    def test_empty_catalogue_produces_an_empty_service_list(self):
        self.assertEqual(self.run_poll({})["services"], [])


class ComposeHealthContractTest(unittest.TestCase):
    def test_dashboard_and_app_healthchecks_share_urls_without_startup_dependencies(self):
        if shutil.which("docker") is None:
            self.skipTest("docker compose is required")
        result = subprocess.run(["docker", "compose", "config", "--format", "json"],
                                cwd=ROOT, capture_output=True, text=True, timeout=15)
        self.assertEqual(result.returncode, 0, result.stderr)
        services = json.loads(result.stdout)["services"]
        dashboard = services["dashboard"]
        targets = {key.removeprefix("HEALTH_"): url
                   for key, url in dashboard["environment"].items() if key.startswith("HEALTH_")}
        self.assertEqual(len(targets), 7)
        self.assertFalse(dashboard.get("depends_on"))
        self.assertFalse(dashboard.get("volumes"))
        for name, url in targets.items():
            with self.subTest(service=name):
                healthcheck = services[name]["healthcheck"]["test"]
                self.assertEqual(healthcheck[0], "CMD")
                self.assertEqual(healthcheck[-1], url)
                self.assertTrue(url.startswith("http://" + name + ":"))


if __name__ == "__main__":
    unittest.main()
