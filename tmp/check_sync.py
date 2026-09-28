from pathlib import Path
import subprocess
import tempfile

root = Path.cwd()
with tempfile.TemporaryDirectory(dir=root / 'tmp') as temp:
    work = Path(temp)
    # Test on an isolated port without touching the user's running server/data.
    for name in ['Server', 'Client']:
        source = (root / 'mail' / (name + '.java')).read_text('utf-8')
        (work / (name + '.java')).write_text(source.replace('2000', '22001'), 'utf-8')
    subprocess.run(['javac', '-encoding', 'UTF-8', '-d', str(work),
                    str(work / 'Server.java'), str(work / 'Client.java')], check=True)
    test = (root / 'tmp/check_mail.py').read_text('utf-8')
    test = test.replace("str(Path('tmp/classes').resolve())", repr(str(work)))
    test = test.replace('2000', '22001')
    test = test.replace("body = 'Chào", "before = request('login|binh')\n        assert 'email_' not in before\n        body = 'Chào")
    test = test.replace('server.communicate(timeout=5)',
                        "logs, errors = server.communicate(timeout=5)\n        assert 'NHAN tu' in logs and 'LUU email cho binh' in logs and 'TRA cho' in logs\n        print('PASS: mailbox update after send and server activity logs.')")
    exec(compile(test, '<isolated UDP test>', 'exec'))
