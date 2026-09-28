import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import javax.swing.*;

public class MailUI extends JFrame {
    private final JTextField host = new JTextField("localhost");
    private final JTextField account = new JTextField();
    private final JTextField recipient = new JTextField();
    private final JTextArea content = new JTextArea(6, 35);
    private final JTextArea result = new JTextArea(8, 35);
    private final JTextArea inbox = new JTextArea(7, 35);
    private final JLabel syncStatus = new JLabel("Chưa đăng nhập để đồng bộ hộp thư.");
    private volatile DatagramSocket watchSocket;
    private boolean busy;
    private final JButton create = new JButton("Tạo account");
    private final JButton login = new JButton("Đăng nhập / xem danh sách");
    private final JButton send = new JButton("Gửi email");

    public MailUI() {
        super("Mail UDP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel fields = new JPanel(new GridLayout(3, 2, 8, 8));
        fields.add(new JLabel("Địa chỉ server:"));
        fields.add(host);
        fields.add(new JLabel("Account (tạo / đăng nhập):"));
        fields.add(account);
        fields.add(new JLabel("Account nhận email:"));
        fields.add(recipient);

        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        JScrollPane editor = new JScrollPane(content);
        editor.setBorder(BorderFactory.createTitledBorder("Nội dung email"));
        JScrollPane output = new JScrollPane(result);
        output.setBorder(BorderFactory.createTitledBorder("Kết quả thao tác"));
        inbox.setEditable(false);
        JScrollPane inboxPane = new JScrollPane(inbox);
        inboxPane.setBorder(BorderFactory.createTitledBorder("Hộp thư cập nhật khi có thư mới"));
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(output, BorderLayout.NORTH);
        bottom.add(inboxPane, BorderLayout.CENTER);
        bottom.add(syncStatus, BorderLayout.SOUTH);

        JPanel buttons = new JPanel();
        buttons.add(create);
        buttons.add(login);
        buttons.add(send);
        JPanel middle = new JPanel(new BorderLayout(8, 8));
        middle.add(editor, BorderLayout.CENTER);
        middle.add(buttons, BorderLayout.SOUTH);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(fields, BorderLayout.NORTH);
        panel.add(middle, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        setContentPane(panel);

        create.addActionListener(e -> submit("create", account.getText().trim()));
        login.addActionListener(e -> submit("login", account.getText().trim()));
        send.addActionListener(e -> submit("send", recipient.getText().trim()));
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopWatching();
            }
        });
    }

    private void submit(String command, String name) {
        if (busy) return;
        if (!name.matches("[a-z0-9_]{1,30}")) {
            result.setText("Tên account gồm 1–30 chữ thường, chữ số hoặc dấu _.");
            return;
        }
        String address = host.getText().trim();
        if (address.isEmpty()) {
            result.setText("Vui lòng nhập địa chỉ server.");
            return;
        }
        String message = command + "|" + name;
        if (command.equals("send")) {
            if (content.getText().trim().isEmpty()) {
                result.setText("Vui lòng nhập nội dung email.");
                return;
            }
            message += "|" + content.getText();
        }
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        if (data.length > 60000) {
            result.setText("Email quá dài (gói tin tối đa 60000 byte).");
            return;
        }
        request(command, name, address, data);
    }

    private void request(String command, String name, String address, byte[] data) {
        setBusy(true);
        result.setText("Đang chờ server...");

        // Gui/nhan o luong nen de cua so khong bi dung khi cho server.
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                InetAddress server = InetAddress.getByName(address);
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(5000);
                    socket.send(new DatagramPacket(data, data.length, server, 2000));
                    byte[] buffer = new byte[65507];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    socket.receive(reply);
                    return new String(reply.getData(), 0, reply.getLength(), StandardCharsets.UTF_8);
                } catch (SocketTimeoutException e) {
                    return "Không nhận được phản hồi sau 5 giây. Kiểm tra server.\n"
                            + "Nếu vừa tạo account hoặc gửi thư, hãy đăng nhập kiểm tra trước khi gửi lại.";
                }
            }

            @Override
            protected void done() {
                try {
                    String reply = get();
                    result.setText(reply);
                    if (command.equals("login")) {
                        if (reply.startsWith("Dang nhap thanh cong.")) {
                            int line = reply.indexOf('\n');
                            inbox.setText(line < 0 ? "" : reply.substring(line + 1));
                            syncStatus.setText("Account: " + name + " @ " + address
                                    + " | Cập nhật: " + java.time.LocalTime.now().withNano(0));
                            startWatching(name, address);
                        }
                    }
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    result.setText("Lỗi gửi/nhận UDP: " + cause.getMessage());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void stopWatching() {
        DatagramSocket old = watchSocket;
        watchSocket = null;
        if (old != null) {
            try {
                byte[] data = "unwatch|account".getBytes(StandardCharsets.UTF_8);
                old.send(new DatagramPacket(data, data.length));
            } catch (java.io.IOException ignored) {
                // Socket van duoc dong neu server khong con chay.
            }
            old.close();
        }
    }

    private void startWatching(String name, String address) throws java.net.SocketException {
        stopWatching();
        DatagramSocket socket = new DatagramSocket();
        watchSocket = socket;
        syncStatus.setText("Đang đăng ký nhận thư mới cho " + name + "...");
        Thread listener = new Thread(() -> {
            try {
                // UDP connect chi loc dia chi gui/nhan, khong tao ket noi TCP.
                socket.connect(InetAddress.getByName(address), 2000);
                socket.setSoTimeout(5000);
                byte[] data = ("watch|" + name).getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(data, data.length));
                while (!socket.isClosed()) {
                    byte[] buffer = new byte[65507];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String update = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    // Sau phan hoi dang ky, cho server push, khong hoi dinh ky.
                    socket.setSoTimeout(0);
                    SwingUtilities.invokeLater(() -> {
                        if (watchSocket != socket) return;
                        if (update.startsWith("Dang nhap thanh cong.")) {
                            inbox.setText(update.substring(update.indexOf('\n') + 1));
                            syncStatus.setText("Account: " + name + " @ " + address
                                    + " | Nhận cập nhật: " + java.time.LocalTime.now().withNano(0));
                        } else {
                            syncStatus.setText(update + " Hãy đăng nhập lại.");
                        }
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (watchSocket == socket) {
                        syncStatus.setText("Không nhận được cập nhật. Hãy đăng nhập lại.");
                    }
                });
            } finally {
                socket.close();
            }
        }, "nhan-thu-" + name);
        listener.setDaemon(true);
        listener.start();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        create.setEnabled(!busy);
        login.setEnabled(!busy);
        send.setEnabled(!busy);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MailUI().setVisible(true));
    }
}
