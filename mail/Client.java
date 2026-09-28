import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {
        InetAddress server = InetAddress.getByName(args.length > 0 ? args[0] : "localhost");
        try (Scanner input = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n1. Tao account");
                System.out.println("2. Gui email");
                System.out.println("3. Dang nhap / xem danh sach email");
                System.out.println("0. Thoat");
                System.out.print("Chon: ");
                if (!input.hasNextLine()) break;
                String choice = input.nextLine();
                if (choice.equals("0")) break;
                if (!choice.equals("1") && !choice.equals("2") && !choice.equals("3")) {
                    System.out.println("Lua chon khong hop le.");
                    continue;
                }

                System.out.print(choice.equals("2") ? "Account nhan: " : "Ten account: ");
                String account = input.nextLine().trim();
                if (!account.matches("[a-z0-9_]{1,30}")) {
                    System.out.println("Ten account chi gom chu thuong, so, dau _ (1-30 ky tu).");
                    continue;
                }
                String message;
                if (choice.equals("1")) {
                    message = "create|" + account;
                } else if (choice.equals("2")) {
                    System.out.print("Noi dung email (mot dong): ");
                    message = "send|" + account + "|" + input.nextLine();
                } else {
                    message = "login|" + account;
                }

                byte[] data = message.getBytes(StandardCharsets.UTF_8);
                if (data.length > 60000) {
                    System.out.println("Email qua dai (goi tin toi da 60000 byte).");
                    continue;
                }
                // Moi yeu cau dung mot socket, tranh nhan lai phan hoi cu khi timeout.
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(5000);
                    DatagramPacket request = new DatagramPacket(data, data.length, server, 2000);
                    socket.send(request);
                    byte[] buffer = new byte[65507];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    socket.receive(reply);
                    System.out.println(new String(reply.getData(), 0,
                            reply.getLength(), StandardCharsets.UTF_8));
                } catch (SocketTimeoutException e) {
                    System.out.println("Khong nhan duoc phan hoi sau 5 giay. Kiem tra server.");
                } catch (java.io.IOException e) {
                    System.out.println("Loi gui/nhan UDP: " + e.getMessage());
                }
            }
        }
    }
}
