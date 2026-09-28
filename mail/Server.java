import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Server {
    static final Path DATA = Paths.get("data");
    static final java.util.Map<java.net.SocketAddress, String> watchers = new java.util.HashMap<>();

    static void reply(DatagramSocket socket, java.net.SocketAddress target, String text)
            throws java.io.IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 60000) {
            bytes = "Danh sach qua lon de gui trong mot goi UDP.".getBytes(StandardCharsets.UTF_8);
        }
        socket.send(new DatagramPacket(bytes, bytes.length, target));
    }

    static void log(String message) {
        System.out.println("[" + java.time.LocalTime.now().withNano(0) + "]["
                + Thread.currentThread().getName() + "] " + message);
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DATA);
        try (DatagramSocket socket = new DatagramSocket(2000)) {
            log("Mail server dang chay tai cong 2000. Dang cho yeu cau...");
            while (true) {
                byte[] buffer = new byte[65507];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                String message = new String(request.getData(), 0,
                        request.getLength(), StandardCharsets.UTF_8);
                String client = request.getAddress().getHostAddress() + ":" + request.getPort();
                String[] info = message.split("\\|", 3);
                log("NHAN tu " + client + " | lenh=" + info[0]
                        + (info.length > 1 ? " | account=" + info[1] : ""));
                String result;
                try {
                    if (info.length == 2 && info[0].equals("unwatch")) {
                        watchers.remove(request.getSocketAddress());
                        continue;
                    }
                    if (info.length == 2 && info[0].equals("watch")) {
                        result = process("login|" + info[1]);
                        if (result.startsWith("Dang nhap thanh cong.")) {
                            watchers.put(request.getSocketAddress(), info[1]);
                            log("THEO DOI " + info[1] + " tai " + client);
                        }
                    } else {
                        result = process(message);
                    }
                } catch (java.io.IOException e) {
                    result = "Loi doc/ghi du lieu tren server.";
                    System.err.println(e.getMessage());
                }
                reply(socket, request.getSocketAddress(), result);
                log("TRA cho " + client + " | " + result.split("\n", 2)[0]);
                if (result.equals("Gui email thanh cong.")) {
                    String mailbox = process("login|" + info[1]);
                    for (java.util.Map.Entry<java.net.SocketAddress, String> entry : watchers.entrySet()) {
                        if (entry.getValue().equals(info[1])) {
                            try {
                                reply(socket, entry.getKey(), mailbox);
                                log("PUSH thu moi cho " + info[1] + " -> " + entry.getKey());
                            } catch (java.io.IOException e) {
                                log("Khong push duoc toi " + entry.getKey());
                            }
                        }
                    }
                }
            }
        } catch (java.net.BindException e) {
            System.out.println("Cong UDP 2000 dang duoc su dung. Neu server da mo, hay chay client.bat."
                    + " Muon khoi dong lai, hay dong server cu truoc.");
        }
    }

    static String process(String message) throws java.io.IOException {
        // Goi tin: create|account, login|account, send|account|noi dung.
        // Tach toi da 3 phan de noi dung email van chua duoc dau |.
        String[] parts = message.split("\\|", 3);
        if (parts.length < 2) return "Yeu cau khong hop le.";
        String command = parts[0];
        String account = parts[1];
        if (!account.matches("[a-z0-9_]{1,30}")) {
            return "Ten account chi gom chu thuong, so, dau _ (1-30 ky tu).";
        }
        Path folder = DATA.resolve(account);

        switch (command) {
            case "create":
                if (Files.exists(folder)) return "Account da ton tai.";
                Files.createDirectory(folder);
                Files.write(folder.resolve("new_email.txt"),
                        "Thank you for using this service. we hope that you will feel comfortabl........"
                                .getBytes(StandardCharsets.UTF_8));
                log("TAO account " + account + " va " + folder.resolve("new_email.txt"));
                return "Tao account thanh cong.";

            case "send":
                if (!Files.isDirectory(folder)) return "Account nhan khong ton tai.";
                if (parts.length < 3 || parts[2].trim().isEmpty()) {
                    return "Noi dung email khong duoc rong.";
                }
                // Tao ten rieng cho moi email, khong ghi de thu cu.
                int number = 1;
                Path email = folder.resolve("email_" + number + ".txt");
                while (Files.exists(email)) {
                    email = folder.resolve("email_" + (++number) + ".txt");
                }
                Files.write(email, parts[2].getBytes(StandardCharsets.UTF_8),
                        java.nio.file.StandardOpenOption.CREATE_NEW);
                log("LUU email cho " + account + " -> " + email);
                return "Gui email thanh cong.";

            case "login":
                if (!Files.isDirectory(folder)) return "Account khong ton tai.";
                String[] names = folder.toFile().list((dir, name) ->
                        new java.io.File(dir, name).isFile());
                if (names == null) return "Khong doc duoc danh sach email.";
                Arrays.sort(names);
                log("DANH SACH " + account + " | " + names.length + " file");
                return "Dang nhap thanh cong. Danh sach email:\n" + String.join("\n", names);

            default:
                return "Lenh khong hop le.";
        }
    }
}
