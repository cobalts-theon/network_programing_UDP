# Bài tập mail UDP

Java dùng `DatagramSocket`, `DatagramPacket`, cổng `2000` theo cách gửi/nhận trong slide. Không cần thư viện ngoài (JDK 8 trở lên).

## Chạy chương trình

### Chạy bằng file .bat và giao diện Swing

1. Bấm đúp `server.bat` để biên dịch và chạy server. Giữ cửa sổ server mở.
2. Bấm đúp `client.bat` để biên dịch và mở giao diện `MailUI`.
3. Giữ địa chỉ server là `localhost` nếu chạy cùng máy; nếu khác máy, nhập IP server.
4. Nhập ô **Account (tạo / đăng nhập)** rồi bấm **Tạo account** hoặc **Đăng nhập / xem danh sách**.
5. Muốn gửi thư, nhập **Account nhận email**, nhập nội dung rồi bấm **Gửi email**. Nội dung hỗ trợ nhiều dòng và tiếng Việt.

Kết quả và danh sách tên file xuất hiện ở phần dưới cửa sổ. Đóng cửa sổ để thoát client. File `.bat` tự chuyển vào thư mục `mail`, biên dịch các file `.class` vào `mail/build` và lưu dữ liệu trong `mail/data`. Hai thư mục `build` và `data` được bỏ qua bởi `.gitignore`. Cần có `java` và `javac` trong PATH.

### Chạy client menu trong terminal

Mở terminal tại thư mục `mail`, biên dịch:

```powershell
javac -encoding UTF-8 -d build Server.java Client.java
```

Terminal thứ nhất (cũng tại `mail`):

```powershell
java -cp build Server
```

Terminal thứ hai (cũng tại `mail`):

```powershell
java -cp build Client
```

Nếu client chạy ở máy khác, truyền IP máy server, ví dụ `java -cp build Client 192.168.1.10`. Hai máy phải liên lạc được và máy server cho phép UDP cổng 2000.

## Thử theo đề

1. Chọn `1`, tạo account `an`. Server tạo `data/an/new_email.txt` chứa lời chào đúng trong đề.
2. Chọn `1`, tạo account `binh`.
3. Chọn `2`, nhập người nhận `binh`, nhập nội dung email. Server lưu nguyên nội dung vào một file `email_....txt` mới trong `data/binh`.
4. Chọn `3`, đăng nhập `binh`. Client nhận và hiển thị tất cả tên file trong thư mục đó, gồm thư chào và email vừa nhận.
5. Chọn `0` để thoát client; nhấn `Ctrl+C` ở terminal server để dừng server.

Có thể mở nhiều client. Dữ liệu giữ lại sau khi khởi động lại server nếu chạy tại cùng thư mục.

## Logic

Sau khi đăng nhập thành công, client đăng ký nhận thông báo bằng `watch|account`. Khi lưu email xong, server chủ động gửi ngay danh sách mới đến các client đang theo dõi tài khoản nhận, không chờ bộ hẹn giờ. Mở hai client: đăng nhập `binh` ở một cửa sổ, dùng cửa sổ kia gửi thư cho `binh`; tên file mới sẽ tự xuất hiện. Kết quả thao tác và hộp thư hiển thị riêng. UDP có thể mất gói hoặc đảo thứ tự; nếu danh sách chưa đúng, hoặc server vừa khởi động lại, hãy bấm đăng nhập lại để lấy danh sách và đăng ký lại. Không có cơ chế bảo đảm giao nhận trong bản bài tập này. Muốn chuyển tài khoản/server đồng bộ, nhập thông tin và đăng nhập thành công tài khoản mới.

Server in thời gian, tên luồng xử lý, địa chỉ client, lệnh nhận, file đã lưu và phản hồi. Server vẫn xử lý tuần tự trên luồng `main` cho dễ hiểu; client dùng `SwingWorker` để gửi yêu cầu và một luồng nhận UDP riêng để cập nhật Swing ngay khi nhận thông báo. Đóng cửa sổ hoặc chuyển đăng nhập sẽ gửi `unwatch` và đóng socket cũ. Sau khi sửa code, đóng server/client cũ rồi mở lại các file `.bat`; chỉ chạy một server trên cổng 2000.

- `create|account`: tạo thư mục account và file `new_email.txt`.
- `send|account|noi dung`: kiểm tra người nhận rồi tạo file chứa nội dung thư.
- `login|account`: kiểm tra account rồi trả danh sách tên file.
- Server lặp: nhận gói tin → xử lý → gửi kết quả về địa chỉ và cổng của client.

Các thư mục (`mail`, `data`, tên account) đều viết thường. Tên lớp Java `Server`, `Client` theo quy ước Java. Tên account gồm 1–30 chữ thường, chữ số hoặc dấu `_`.

Đăng nhập chỉ kiểm tra tên account, không có mật khẩu vì đề không yêu cầu. Client nhập email một dòng; dữ liệu truyền bằng UTF-8. Bản bài tập dùng một gói UDP cho mỗi yêu cầu/phản hồi, giới hạn 60000 byte; danh sách lớn hơn giới hạn sẽ báo lỗi. UDP có thể mất gói; client chờ tối đa 5 giây và không tự gửi lại. Nếu hết thời gian chờ sau khi tạo account/gửi thư, đăng nhập để kiểm tra kết quả trước khi thao tác lại.

