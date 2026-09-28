from pathlib import Path
import socket
import subprocess
import tempfile

classes = str(Path('tmp/classes').resolve())
java = 'C:/Program Files/Java/jdk-24/bin/java.exe'
with tempfile.TemporaryDirectory(dir='tmp') as folder:
    server = subprocess.Popen([java, '-cp', classes, 'Server'], cwd=folder,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    try:
        assert '2000' in server.stdout.readline()
        def request(message):
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as client:
                client.settimeout(15)
                client.sendto(message.encode('utf-8'), ('127.0.0.1', 2000))
                return client.recv(65507).decode('utf-8')

        assert request('create|an') == 'Tao account thanh cong.'
        welcome = Path(folder, 'data/an/new_email.txt')
        assert welcome.read_text('utf-8') == 'Thank you for using this service. we hope that you will feel comfortabl........'
        assert request('create|an') == 'Account da ton tai.'
        assert request('create|binh') == 'Tao account thanh cong.'
        body = 'Chào Bình | đây là email thử nghiệm.'
        assert request('send|binh|' + body) == 'Gui email thanh cong.'
        assert request('send|binh|thu thu hai') == 'Gui email thanh cong.'
        emails = list(Path(folder, 'data/binh').glob('email_*.txt'))
        assert len(emails) == 2
        assert body in [p.read_text('utf-8') for p in emails]
        result = request('login|binh')
        assert set(result.splitlines()[1:]) == {p.name for p in Path(folder, 'data/binh').iterdir()}
        assert request('login|missing') == 'Account khong ton tai.'
        assert request('send|missing|hello') == 'Account nhan khong ton tai.'
        assert request('send|an|') == 'Noi dung email khong duoc rong.'
        assert 'Ten account' in request('create|../outside')
        assert request('bad') == 'Yeu cau khong hop le.'
        client = subprocess.run([java, '-cp', classes, 'Client'],
                                input='3\nbinh\n0\n', capture_output=True, text=True, timeout=10)
        assert client.returncode == 0 and 'new_email.txt' in client.stdout
        assert all(p.name in client.stdout for p in emails)
        print('PASS: welcome, duplicate account, UTF-8 email, unique files, login list, invalid requests, client menu.')
    finally:
        server.terminate()
        server.communicate(timeout=5)
