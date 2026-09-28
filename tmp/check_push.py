from pathlib import Path
import socket
import subprocess
import tempfile
import time

root = Path.cwd()
java = 'C:/Program Files/Java/jdk-24/bin/java.exe'
with tempfile.TemporaryDirectory(dir=root / 'tmp') as temp:
    work = Path(temp)
    source = (root / 'mail/Server.java').read_text('utf-8').replace('2000', '22002')
    (work / 'Server.java').write_text(source, 'utf-8')
    subprocess.run(['javac', '-encoding', 'UTF-8', '-d', temp, str(work / 'Server.java')], check=True)
    server = subprocess.Popen([java, '-cp', temp, 'Server'], cwd=temp,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    clients = []
    def client():
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(5)
        s.connect(('127.0.0.1', 22002))
        clients.append(s)
        return s
    def ask(s, text):
        s.send(text.encode('utf-8'))
        return s.recv(65507).decode('utf-8')
    try:
        assert '22002' in server.stdout.readline()
        sender = client()
        assert 'thanh cong' in ask(sender, 'create|binh')
        assert 'thanh cong' in ask(sender, 'create|an')
        a, b, other = client(), client(), client()
        assert 'new_email.txt' in ask(a, 'watch|binh')
        assert 'new_email.txt' in ask(b, 'watch|binh')
        assert 'new_email.txt' in ask(other, 'watch|an')
        start = time.monotonic()
        assert ask(sender, 'send|binh|Xin chào\nThư mới') == 'Gui email thanh cong.'
        first = a.recv(65507).decode('utf-8')
        assert 'email_' in first and b.recv(65507).decode('utf-8') == first
        print('PASS: both recipients received push without polling, elapsed %.3fs' % (time.monotonic() - start))
        other.settimeout(0.3)
        try:
            other.recv(65507)
            raise AssertionError('Wrong account received push')
        except socket.timeout:
            pass
        a.send(b'unwatch|account')
        # A reply on this same endpoint confirms server processed earlier unwatch.
        assert 'thanh cong' in ask(a, 'login|binh')
        assert 'thanh cong' in ask(sender, 'send|binh|Second')
        assert b.recv(65507).decode('utf-8').count('email_') == 2
        a.settimeout(0.3)
        try:
            a.recv(65507)
            raise AssertionError('Unregistered client received push')
        except socket.timeout:
            pass
        print('PASS: account isolation and unsubscribe; second email pushed.')
    finally:
        for s in clients:
            s.close()
        server.terminate()
        server.communicate(timeout=5)
