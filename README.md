# 🎮 Number Guessing Game

Dự án game đoán số bằng Java 17 + Spring Boot 3, hỗ trợ đăng nhập JWT, chơi game, mua lượt và xem bảng xếp hạng.

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Tính năng chính](#tính-năng-chính)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt và chạy](#cài-đặt-và-chạy)
- [Cấu hình ứng dụng](#cấu-hình-ứng-dụng)
- [Xác thực JWT](#xác-thực-jwt)
- [API endpoints](#api-endpoints)
- [Ví dụ request/response](#ví-dụ-requestresponse)
- [Kiểm thử VNPAY](#kiểm-thử-vnpay)
- [Xử lý sự cố thường gặp](#xử-lý-sự-cố-thường-gặp)

---

## Giới thiệu

Project này là một ứng dụng web mini-game cho phép người dùng:

- đăng ký / đăng nhập bằng username và password
- chơi game đoán số từ 1 đến 5
- nhận điểm khi đoán đúng
- mua thêm lượt chơi
- xem bảng xếp hạng theo điểm số
- tích hợp thanh toán VNPAY sandbox

Dự án được xây dựng theo kiến trúc Spring Boot REST API kết hợp với giao diện web tĩnh bằng HTML/CSS/JavaScript.

---

## Tính năng chính

- 🔐 Authentication với JWT
- 🎯 Game đoán số với tỷ lệ thắng có thể cấu hình
- 💳 Mua lượt chơi qua VNPAY
- 🏆 Bảng xếp hạng top người chơi
- 🛡️ Bảo vệ API với Spring Security
- ⚡ Xử lý race condition khi nhiều request cùng truy cập
- 📱 Giao diện web responsive

---

## Công nghệ sử dụng

| Layer | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security |
| Persistence | Spring Data JPA |
| Database | PostgreSQL / H2 |
| Auth | JWT (jjwt) |
| Payment | VNPAY Sandbox |
| Frontend | HTML, CSS, JavaScript |

---

## Yêu cầu hệ thống

| Công cụ | Phiên bản | Ghi chú |
|---|---|---|
| JDK | 17+ | Cài đặt Java 17 |
| Maven | 3.8+ | Quản lý dependency |
| PostgreSQL | 14+ | Hoặc dùng H2 trong môi trường dev |
| IDE | IntelliJ / VS Code | Khuyến nghị IntelliJ IDEA |
| ngrok | 3.x | Dùng cho test VNPAY nếu cần |
| Postman | Nên có | Test API dễ hơn |

---

## Cài đặt và chạy

### 1. Clone project

```bash
git clone <repo-url>
cd number-guessing-game
```

### 2. Cài đặt database

Nếu dùng PostgreSQL:

```sql
CREATE DATABASE guess_game;
```

Hoặc có thể dùng H2 mặc định trong môi trường local.

### 3. Cấu hình application

Mở file `application.yml` và cập nhật thông tin database, JWT, VNPAY:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/guess_game
    username: postgres
    password: your_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

app:
  jwt:
    secret: "your-secret-key-at-least-32-characters"
    expiration-ms: 86400000
  game:
    win-rate: 0.05
    start-turns: 10
    buy-turns-amount: 5
    turn-price-vnd: 10000

vnpay:
  tmn-code: "YOUR_TMN_CODE"
  hash-secret: "YOUR_HASH_SECRET"
  pay-url: "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
  return-url: "http://localhost:8080/api/payment/vnpay-return"
  ipn-url: "http://localhost:8080/api/payment/vnpay-ipn"
```

### 4. Gán JWT secret (nếu cần)

```bash
openssl rand -base64 64
```

### 5. Chạy ứng dụng

```bash
mvn clean spring-boot:run
```

Hoặc build JAR:

```bash
mvn clean package -DskipTests
java -jar target/guess-game-1.0.0.jar
```

Sau khi server chạy, truy cập:

```text
http://localhost:8080/
```

Nếu giao diện đăng nhập hiện ra, ứng dụng đã chạy thành công.

---

## Cấu hình ứng dụng

Các tham số quan trọng nằm trong `application.yml`:

- `app.jwt.secret`: khóa bí mật để ký JWT
- `app.jwt.expiration-ms`: thời hạn token (mặc định 24h)
- `app.game.win-rate`: xác suất thắng khi đoán số, ví dụ 0.05 = 5%
- `app.game.start-turns`: số lượt ban đầu khi đăng ký
- `app.game.buy-turns-amount`: số lượt mua thêm mỗi lần
- `app.game.turn-price-vnd`: giá tiền mỗi gói lượt
- `vnpay.*`: cấu hình sandbox VNPAY

---

## Xác thực JWT

Project dùng JWT Bearer Token để xác thực API.

### Bước 1: Đăng ký tài khoản

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "secret123"
  }'
```

Response mẫu:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "alice"
}
```

### Bước 2: Đăng nhập

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "secret123"
  }'
```

### Bước 3: Gửi token trong header

```bash
curl http://localhost:8080/api/me \
  -H "Authorization: Bearer <token>"
```

> Token có thời hạn theo `app.jwt.expiration-ms`. Khi hết hạn, hãy đăng nhập lại để lấy token mới.

---

## API endpoints

### Public APIs

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký tài khoản |
| POST | `/api/auth/login` | Đăng nhập và nhận JWT |
| GET | `/api/leaderboard` | Xem bảng xếp hạng |
| GET | `/api/payment/vnpay-return` | Callback sau khi thanh toán |
| GET | `/api/payment/vnpay-ipn` | IPN từ VNPAY |

### Protected APIs

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/guess` | Đoán số 1-5 |
| GET | `/api/me` | Lấy thông tin người dùng |
| POST | `/api/payment/buy-turns` | Mua thêm lượt chơi |

---

## Ví dụ request/response

### 1. Đoán số

Request:

```json
{
  "number": 3
}
```

Response thành công:

```json
{
  "serverNumber": 3,
  "yourGuess": 3,
  "correct": true,
  "score": 1,
  "turnsLeft": 9,
  "message": "🎉 Đoán đúng! +1 điểm"
}
```

Response sai:

```json
{
  "serverNumber": 1,
  "yourGuess": 3,
  "correct": false,
  "score": 0,
  "turnsLeft": 9,
  "message": "❌ Sai rồi, thử lại nhé!"
}
```

Response khi hết lượt:

```json
{
  "error": "Bạn đã hết lượt chơi. Hãy mua thêm."
}
```

### 2. Thông tin người dùng

```bash
curl http://localhost:8080/api/me \
  -H "Authorization: Bearer <token>"
```

Response:

```json
{
  "username": "alice",
  "score": 5,
  "turns": 8
}
```

### 3. Mua thêm lượt

```bash
curl -X POST http://localhost:8080/api/payment/buy-turns \
  -H "Authorization: Bearer <token>"
```

Response:

```json
{
  "txnRef": "TURNS_alice_d4bcb55c",
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "amountVnd": 10000,
  "turnsAmount": 5
}
```

### 4. Bảng xếp hạng

```bash
curl http://localhost:8080/api/leaderboard
```

Response:

```json
[
  { "rank": 1, "username": "alice", "score": 12 },
  { "rank": 2, "username": "bob", "score": 8 },
  { "rank": 3, "username": "charlie", "score": 5 }
]
```

---

## Kiểm thử VNPAY

### Cách 1: Dùng ngrok (khuyến nghị)

1. Cài đặt ngrok
2. Chạy:

```bash
ngrok http 8080
```

3. Cập nhật `vnpay.return-url` và `vnpay.ipn-url` theo URL public của ngrok.
4. Restart app.
5. Gọi API mua lượt và mở `paymentUrl` để test thanh toán sandbox.

Thông tin thẻ test VNPAY sandbox:

| Trường | Giá trị |
|---|---|
| Ngân hàng | NCB |
| Số thẻ | 9704198526191432198 |
| Tên chủ thẻ | NGUYEN VAN A |
| Ngày phát hành | 07/15 |
| OTP | 123456 |

Sau khi thanh toán thành công, hệ thống sẽ redirect về `/game.html?payment=success`.

### Cách 2: Test fallback local

Nếu không dùng ngrok, bạn có thể xử lý thanh toán ngay trong endpoint return để test nhanh hơn.

---

## Xử lý sự cố thường gặp

### 1. Lỗi database connection

Kiểm tra:

- username/password PostgreSQL
- tên database
- port `5432`
- firewall / Docker nếu đang chạy trong container

### 2. JWT không hợp lệ

Nguyên nhân thường gặp:

- thiếu header `Authorization`
- token hết hạn
- secret key không khớp giữa server và config

### 3. VNPAY không redirect về đúng URL

Kiểm tra lại:

- `vnpay.return-url`
- `vnpay.ipn-url`
- domain/public URL của ngrok hoặc server public
- đúng với cấu hình sandbox của VNPAY

### 4. API trả 401 hoặc 403

Đảm bảo bạn đã thêm header:

```http
Authorization: Bearer <token>
```

---

## Ghi chú

- Đây là project demo / mini-game phục vụ học tập và showcase backend API.
- Tỷ lệ thắng và số lượt có thể tùy chỉnh qua `application.yml`.
- Nếu cần, bạn có thể mở rộng thêm như: lưu lịch sử trận đấu, leaderboard nâng cao, quản lý user, hoặc triển khai frontend React/Vue.

---

## Kết luận

Game này kết hợp nhiều tính năng đầy đủ của một ứng dụng backend hiện đại: xác thực, bảo mật, thanh toán, bảo trì dữ liệu và API REST. Đây là một project phù hợp để học tập, demo hoặc làm nền tảng cho sản phẩm thực tế.

Security	Spring Security + JWT (jjwt)
Database	PostgreSQL (H2 cho test)
ORM	Spring Data JPA / Hibernate
Build	Maven
Frontend	HTML/CSS/JS thuần
Payment	VNPAY Sandbox
Documentation	Swagger (tuỳ chọn)

Cấu trúc project
src/main/java/com/game/guess/
├── GuessGameApplication.java        # Main class
├── config/
│   ├── SecurityConfig.java          # Security + JWT filter
│   └── JwtAuthenticationFilter.java
├── controller/
│   ├── AuthController.java          # /register, /login
│   ├── GameController.java          # /guess, /me, /leaderboard
│   └── PaymentController.java       # /buy-turns, VNPAY callbacks
├── dto/                              # Request/Response DTOs
├── entity/
│   ├── User.java
│   └── PaymentTransaction.java
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/
│   ├── UserRepository.java
│   └── PaymentTransactionRepository.java
├── security/
│   └── JwtService.java
└── service/
    ├── AuthService.java
    ├── GameService.java
    ├── PaymentService.java
    └── VnpayService.java

src/main/resources/
├── application.yml
└── static/
    ├── index.html                    # Login/Register
    ├── game.html                     # Game chính
    ├── leaderboard.html              # BXH
    ├── css/style.css
    └── js/
        ├── api.js
        ├── auth.js
        ├── game.js
        └── leaderboard.js

⚡ Xử lý race condition
Vấn đề
Khi user bấm nút đoán nhanh nhiều lần hoặc gọi API đồng thời, có thể xảy ra:

turns bị âm (trừ nhiều lần)

score bị cộng sai

Giải pháp
1. Atomic UPDATE ở tầng database

java
@Modifying
@Query("UPDATE User u SET u.turns = u.turns - 1 WHERE u.id = :id AND u.turns > 0")
int decrementTurn(@Param("id") Long id);

Điều kiện turns > 0 trong câu UPDATE đảm bảo:
Nếu user còn lượt → affected = 1 → tiếp tục xử lý
Nếu hết lượt → affected = 0 → throw exception

2. Optimistic Locking với @Version
@Version
private Long version;

Hibernate tự kiểm tra version khi update → conflict thì throw OptimisticLockException.

3. Idempotency cho payment

java
@Modifying
@Query("UPDATE PaymentTransaction p SET p.status = 'SUCCESS' " +
       "WHERE p.txnRef = :txnRef AND p.status = 'PENDING'")
int markAsSuccess(@Param("txnRef") String txnRef, ...);

Chỉ update khi status còn PENDING → dù VNPAY gọi IPN nhiều lần, turns chỉ cộng 1 lần.

Test race condition
bash
# Bắn 20 request song song khi user có 10 turns
for i in {1..20}; do
    curl -s -X POST http://localhost:8080/api/guess \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"number":1}' &
done
wait

# Check kết quả
curl http://localhost:8080/api/me -H "Authorization: Bearer $TOKEN"


Kết quả đúng: turns = 0 (không âm).


🔧 Troubleshooting
Lỗi thường gặp

Lỗi	Nguyên nhân	Cách fix
401 Unauthorized	Token sai / hết hạn / thiếu header	Kiểm tra Authorization: Bearer <token>
403 Forbidden	Endpoint chưa permitAll trong SecurityConfig	Cập nhật requestMatchers(...).permitAll()
500 Internal Server Error	Lỗi backend	Xem log console Spring Boot
Connection refused	PostgreSQL chưa chạy	sudo systemctl start postgresql
relation "user" does not exist	PostgreSQL keyword conflict	Đặt @Table(name = "users")
JWT signature does not match	Secret thay đổi giữa các lần restart	Dùng biến môi trường cố định
Lombok không hoạt động	IDE chưa bật Annotation Processing	IntelliJ: Settings → Build → Compiler → Annotation Processors → Enable
Payment status vẫn PENDING	IPN không được gọi	Dùng ngrok hoặc fallback Return URL
