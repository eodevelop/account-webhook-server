# Account Webhook Server

계정 변경 웹훅을 수신하고 처리하는 서버

## 기술 스택

- Kotlin + Spring Boot 4.0
- Exposed (Kotlin SQL Framework)
- SQLite

## API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/webhooks/account-changes` | 웹훅 수신 |
| POST | `/accounts` | 계정 생성 (테스트용) |
| GET | `/accounts/{accountKey}` | 계정 조회 |
| GET | `/inbox/events/{eventId}` | 이벤트 조회 |

## HTTP 파일 테스트

IntelliJ에서 `src/main/resources/http/*.http` 파일을 열고 각 요청 옆의 실행 버튼을 클릭하면 됩니다.

- `account.http` - 계정 생성/조회
- `webhook.http` - 웹훅 수신 테스트 (서명 포함)
- `inbox.http` - 이벤트 조회

### 웹훅 요청 시 필수 헤더

```
X-Event-Id: {고유 이벤트 ID}
X-Signature: {HMAC-SHA256 서명}
```

서명은 `webhook.secret` 값으로 payload를 HMAC-SHA256 해시한 뒤 Base64 인코딩한 값입니다.
