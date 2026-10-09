import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


MOCK_COMMAND = r'''#!/usr/bin/env python3
import json
import os
from pathlib import Path
import signal
import sys

command = Path(sys.argv[0]).name
args = sys.argv[1:]
scenario = os.environ['SCENARIO']
with open(os.environ['COMMAND_LOG'], 'a') as log:
    log.write(json.dumps([command, *args]) + '\n')

if command in ('oc', 'kubectl'):
    if args == ['config', 'current-context']:
        print('test-context')
    elif 'whoami' in args:
        assert '--context=test-context' in args
        assert '--request-timeout=10s' in args
        assert sys.stdin.read() == ''
        if scenario in ('logged-out', 'api-unreachable'):
            sys.exit(1)
        print('test-user')
    elif 'view' in args:
        print('https://wrong.example' if scenario == 'wrong-server' else
              'https://api.eid-systest.norwayeast.aroapp.io:6443')
    elif 'get' in args:
        assert '--request-timeout=10s' in args
        assert '--field-selector=status.phase=Running' in args
        assert '--selector=app.kubernetes.io/instance=mariadb,app.kubernetes.io/component=primary,app.kubernetes.io/part-of=digital-lommebok' in args
        if scenario == 'pod-list-failure':
            sys.exit(1)
        for app, label in [('eudiw-rp-register-service', 'rp-register'),
                           ('eudiw-ca-service', 'ca'), ('eudiw-issuer-server', 'issuer'),
                           ('eudiw-status-list', 'status-list')]:
            ready = 'False' if scenario == 'no-ready-pods' else 'True'
            print(f'arbitrary-{label}-pod digital-lommebok-{label}-mariadb {ready}')
        print('unrelated-pod unknown-mariadb True')
    elif 'port-forward' in args:
        assert any(arg.startswith('arbitrary-') for arg in args)
        Path(os.environ['FORWARD_PID_FILE']).write_text(str(os.getpid()))
        print('Forwarding from 127.0.0.1:33067 -> 3306', flush=True)
        signal.pause()
    else:
        sys.exit(99)
elif command == 'fzf':
    choices = sys.stdin.read().splitlines()
    if 'Importer til lokal Docker' in choices:
        assert choices == ['Importer til lokal Docker', 'Lagre dump']
        if scenario == 'cancel-import':
            sys.exit(130)
        print(choices[1] if scenario == 'save-only' else choices[0])
    else:
        assert len(choices) == 4
        assert not any('unrelated-pod' in choice for choice in choices)
        if scenario == 'invalid-pod':
            print('eudiw-rp-register-service|unrelated-pod')
        elif scenario == 'cancel-pod':
            sys.exit(130)
        else:
            print(next(choice for choice in choices
                       if choice.startswith(os.environ['APPLICATION'] + '|')))
elif command == 'mariadb-dump':
    assert args[-1] == os.environ['EXPECTED_DATABASE']
    print('-- simulated SQL dump')
    if scenario == 'dump-failure':
        sys.exit(1)
elif command == 'docker':
    if args == ['context', 'show']:
        if scenario == 'docker-context-failure':
            sys.exit(1)
        print('local-test')
    elif args[:2] == ['context', 'inspect']:
        print('tcp://remote.example:2376' if scenario == 'remote-docker' else
              'unix:///test/docker.sock')
    elif 'ps' in args:
        if scenario == 'docker-list-failure':
            sys.exit(1)
        project, service = {
            'eudiw-rp-register-service': ('digital-lommebok-rp', 'rp-register-db'),
            'eudiw-ca-service': ('digital-lommebok-rp', 'ca-db'),
            'eudiw-issuer-server': ('digital-lommebok-issuer', 'issuer-db'),
            'eudiw-status-list': ('digital-lommebok-issuer', 'status-list-db'),
        }[os.environ['APPLICATION']]
        assert f'label=com.docker.compose.project={project}' in args
        if '--all' not in args:
            assert f'label=com.docker.compose.service={service}' in args
            if scenario == 'no-database':
                sys.exit(0)
            if scenario == 'multiple-databases':
                print('database-id\nother-database-id')
                sys.exit(0)
        if '--all' in args and scenario == 'no-service':
            pass
        elif '--all' in args and scenario == 'multiple-services':
            print('service-id\nother-service-id')
        else:
            print('service-id' if '--all' in args else 'database-id')
    elif 'inspect' in args:
        print('false' if scenario == 'stopped-service' else 'true')
    elif 'exec' in args:
        expected_local = {'eudiw-rp-register-service': 'rp_register_db',
                          'eudiw-ca-service': 'ca_db',
                          'eudiw-issuer-server': 'issuer_server_db',
                          'eudiw-status-list': 'status_list_db'}[os.environ['APPLICATION']]
        assert args[-1] == expected_local
        if 'SELECT 1' in ' '.join(args) and scenario == 'database-login-failure':
            sys.exit(1)
        if 'DROP DATABASE' in ' '.join(args) and scenario == 'reset-failure':
            sys.exit(1)
        if '-i' in args:
            assert sys.stdin.read() == '-- simulated SQL dump\n'
            if scenario == 'import-failure':
                sys.exit(1)
            if scenario == 'import-interrupted':
                os.kill(os.getppid(), signal.SIGTERM)
    elif 'stop' in args and scenario == 'stop-failure':
        sys.exit(1)
    elif 'start' in args and scenario == 'start-failure':
        sys.exit(1)
    elif 'stop' not in args and 'start' not in args:
        sys.exit(99)
else:
    sys.exit(99)
'''


class DumpMysqlTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.scripts = self.root / 'scripts'
        self.scripts.mkdir()
        self.script = self.scripts / 'mariadb-systest-dump.sh'
        shutil.copyfile(Path(__file__).resolve().parents[1] / 'mariadb-systest-dump.sh', self.script)
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        for command in ('oc', 'kubectl', 'fzf', 'mariadb-dump', 'docker'):
            mock = self.bin / command
            mock.write_text(MOCK_COMMAND)
            mock.chmod(0o700)
        self.log = self.root / 'commands.jsonl'

    def run_script(self, scenario, application='eudiw-rp-register-service',
                    expected_database='rp_register'):
        if self.log.exists():
            self.log.unlink()
        env = dict(os.environ, PATH=f'{self.bin}:{os.environ["PATH"]}',
                    SCENARIO=scenario, COMMAND_LOG=str(self.log), APPLICATION=application,
                    EXPECTED_DATABASE=expected_database, FORWARD_PID_FILE=str(self.root / 'forward.pid'))
        env.pop('DOCKER_HOST', None)
        env.pop('DOCKER_CONTEXT', None)
        result = subprocess.run(['bash', str(self.script)], cwd=self.root, env=env,
                                capture_output=True, text=True, timeout=15)
        calls = [json.loads(line) for line in self.log.read_text().splitlines()]
        pid_file = self.root / 'forward.pid'
        if pid_file.exists():
            with self.assertRaises(ProcessLookupError):
                os.kill(int(pid_file.read_text()), 0)
        return result, calls

    def assert_dump_retained(self):
        files = list((self.scripts / 'dump').iterdir())
        self.assertEqual(len(files), 1)
        self.assertEqual(files[0].suffix, '.sql')
        self.assertEqual(files[0].read_text(), '-- simulated SQL dump\n')
        self.assertEqual(files[0].stat().st_mode & 0o777, 0o600)

    def test_default_import_stops_service_before_reset_and_restarts_after_import(self):
        result, calls = self.run_script('default-import')
        self.assertEqual(result.returncode, 0, result.stderr)
        docker = [call for call in calls if call[0] == 'docker']
        self.assertFalse(any(call[0] == 'kubectl' for call in calls))
        login = next(i for i, call in enumerate(docker) if 'SELECT 1' in ' '.join(call))
        stop = next(i for i, call in enumerate(docker) if 'stop' in call)
        reset = next(i for i, call in enumerate(docker) if 'DROP DATABASE' in ' '.join(call))
        load = next(i for i, call in enumerate(docker) if '-i' in call)
        start = next(i for i, call in enumerate(docker) if 'start' in call)
        self.assertLess(login, stop)
        self.assertLess(stop, reset)
        self.assertLess(reset, load)
        self.assertLess(load, start)
        self.assert_dump_retained()

    def test_second_choice_saves_dump_without_docker(self):
        result, calls = self.run_script('save-only')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(any(call[0] == 'docker' for call in calls))
        self.assert_dump_retained()

    def test_cancelled_import_retains_dump_without_docker(self):
        result, calls = self.run_script('cancel-import')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(any(call[0] == 'docker' for call in calls))
        self.assert_dump_retained()

    def test_wrong_server_blocks_dump(self):
        result, calls = self.run_script('wrong-server')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(call[0] == 'mariadb-dump' for call in calls))
        self.assertFalse((self.scripts / 'dump').exists())

    def test_logged_out_blocks_menus_and_dump_with_login_guidance(self):
        result, calls = self.run_script('logged-out')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('oc login', result.stderr)
        self.assertFalse(any(call[0] in ('fzf', 'mariadb-dump', 'docker') for call in calls))

    def test_unreachable_api_blocks_menus_and_dump(self):
        result, calls = self.run_script('api-unreachable')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('nettverk', result.stderr)
        self.assertFalse(any(call[0] in ('fzf', 'mariadb-dump', 'docker') for call in calls))

    def test_failed_dump_removes_partial_file(self):
        result, calls = self.run_script('dump-failure')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(list((self.scripts / 'dump').iterdir()), [])
        self.assertFalse(any(call[0] == 'docker' for call in calls))

    def test_remote_docker_blocks_reset_and_retains_dump(self):
        result, calls = self.run_script('remote-docker')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(call[0] == 'docker' and 'exec' in call for call in calls))
        self.assert_dump_retained()

    def test_failed_import_keeps_service_stopped_and_retains_dump(self):
        result, calls = self.run_script('import-failure')
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(any(call[0] == 'docker' and 'stop' in call for call in calls))
        self.assertFalse(any(call[0] == 'docker' and 'start' in call for call in calls))
        self.assert_dump_retained()

    def test_stopped_service_is_not_started_after_import(self):
        result, calls = self.run_script('stopped-service')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(any(call[0] == 'docker' and ('stop' in call or 'start' in call) for call in calls))
        self.assert_dump_retained()

    def test_failed_database_login_does_not_stop_service_or_reset_database(self):
        result, calls = self.run_script('database-login-failure')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(call[0] == 'docker' and ('stop' in call or 'DROP DATABASE' in ' '.join(call)) for call in calls))
        self.assert_dump_retained()

    def test_import_without_application_container(self):
        result, calls = self.run_script('no-service')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(any(call[0] == 'docker' and ('stop' in call or 'start' in call) for call in calls))
        self.assert_dump_retained()

    def test_multiple_service_containers_block_import(self):
        result, calls = self.run_script('multiple-services')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(call[0] == 'docker' and 'exec' in call for call in calls))
        self.assert_dump_retained()

    def test_interrupted_import_keeps_service_stopped_and_dump_retained(self):
        result, calls = self.run_script('import-interrupted')
        self.assertEqual(result.returncode, 143, result.stderr)
        self.assertIn('delvis gjennomført', result.stderr)
        self.assertFalse(any(call[0] == 'docker' and 'start' in call for call in calls))
        self.assert_dump_retained()

    def test_all_four_databases_use_the_same_dump_and_import_flow(self):
        for application, database in [('eudiw-rp-register-service', 'rp_register'),
                                      ('eudiw-ca-service', 'eudiw_ca'),
                                      ('eudiw-issuer-server', 'issuer_server_db'),
                                      ('eudiw-status-list', 'status_list_db')]:
            with self.subTest(application=application):
                result, calls = self.run_script('default-import', application,
                                                expected_database=database)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertTrue(any('DROP DATABASE' in ' '.join(call) for call in calls))

    def test_unavailable_or_invalid_pods_block_dump(self):
        for scenario in ('no-ready-pods', 'pod-list-failure', 'invalid-pod', 'cancel-pod'):
            with self.subTest(scenario=scenario):
                result, calls = self.run_script(scenario)
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse(any(call[0] == 'mariadb-dump' for call in calls))

    def test_missing_or_ambiguous_database_blocks_import_and_retains_dump(self):
        for scenario in ('no-database', 'multiple-databases'):
            with self.subTest(scenario=scenario):
                result, calls = self.run_script(scenario)
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse(any(call[0] == 'docker' and 'exec' in call for call in calls))

    def test_docker_lookup_failures_explain_that_dump_is_retained(self):
        for scenario in ('docker-context-failure', 'docker-list-failure'):
            with self.subTest(scenario=scenario):
                result, calls = self.run_script(scenario)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn('dumpen er beholdt', result.stderr)
                self.assertFalse(any(call[0] == 'docker' and 'exec' in call for call in calls))

    def test_failed_stop_blocks_reset_and_retains_dump(self):
        result, calls = self.run_script('stop-failure')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any('DROP DATABASE' in ' '.join(call) for call in calls))
        self.assert_dump_retained()

    def test_failed_restart_reports_successful_import_and_retains_dump(self):
        result, calls = self.run_script('start-failure')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Dumpen er importert', result.stderr)
        self.assert_dump_retained()

    def test_invalid_database_configuration_blocks_dump(self):
        self.script.write_text(self.script.read_text().replace('|rp_register_db|', '|unsafe`name|'))
        result, calls = self.run_script('default-import')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Ugyldig databasekonfigurasjon', result.stderr)
        self.assertFalse(any(call[0] == 'mariadb-dump' for call in calls))

    def test_reset_failure_without_service_does_not_claim_service_is_stopped(self):
        mock = self.bin / 'docker'
        mock.write_text(MOCK_COMMAND.replace("scenario == 'no-service'",
                                             "scenario in ('no-service', 'reset-failure')"))
        result, calls = self.run_script('reset-failure')
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn('forblir stoppet', result.stderr)
        self.assert_dump_retained()

    def test_interrupted_import_without_service_warns_of_partial_import(self):
        mock = self.bin / 'docker'
        mock.write_text(MOCK_COMMAND.replace("scenario == 'no-service'",
                                             "scenario in ('no-service', 'import-interrupted')"))
        result, calls = self.run_script('import-interrupted')
        self.assertEqual(result.returncode, 143, result.stderr)
        self.assertIn('delvis gjennomført', result.stderr)
        self.assertNotIn('forblir stoppet', result.stderr)
        self.assert_dump_retained()


if __name__ == '__main__':
    unittest.main()
