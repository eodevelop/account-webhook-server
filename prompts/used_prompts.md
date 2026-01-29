# 사용한 프롬프트 기록

## 1단계: 프로젝트 세팅

### 프롬프트 1
```
Spring Boot 4.0에서 Exposed 1.0 + SQLite 의존성 build.gradle.kts로 알려줘
```

### 프롬프트 2
```
application 파일에 데이터 베이스와 웹훅 관련 필요 내용들도 넣어줘
```

---l 

## 2단계: Entity 생성

### 프롬프트 3
```
Exposed로 이 테이블 만들어줘

Account: accountKey(unique), email, status, created_at, updated_at
WebhookEvent: eventId(unique), eventType, payload, status, errorMessage, created_at, processed_at

timestamp는 java.time.Instant로
```

### 프롬프트 4
```
AccountStatus(ACTIVE, DELETED, APPLE_DELETED), EventStatus(RECEIVED, PROCESSING, DONE, FAILED) enum 만들어줘
```

### 프롬프트 5
```
Exposed에서 DB 연결하고 앱 시작할 때 테이블 자동생성되게 설정해줘
```

---

## 3단계: 기본 API 구조

### 프롬프트 6
```
웹훅 서버 기본 API 구조 만들어줘

Controller:
- WebhookController: POST /webhooks/account-changes
- AccountController: GET /accounts/{accountKey}
- InboxController: GET /inbox/events/{eventId}

Service, Repository도 같이
```

### 프롬프트 7
```
Request/Response DTO 만들어줘

WebhookRequest: accountKey, eventType, data(Map)
WebhookResponse: status, message
AccountResponse: accountKey, email, status
EventResponse: eventId, eventType, status, createdAt, processedAt
```

---

## 4단계: 서명 검증

### 프롬프트 8
```
HMAC-SHA256으로 웹훅 서명 검증하는 SignatureValidator 만들어줘
secret은 application.properties에서 주입받고
payload랑 signature 비교해서 boolean 반환
```

### 프롬프트 9
```
HandlerInterceptor로 컨트롤러 도달 전에 서명 검증하고 싶어
/webhooks/** 경로에만 적용되게
```

### 프롬프트 10
```
Filter로 request wrapper 씌우고 Interceptor에서 검증하는 방식으로 해줘
```

### 프롬프트 11
```
resources/http 폴더에 컨트롤러별로 .http 파일 만들어서 테스트할 수 있게 해줘
서명 값도 계산해서 넣어줘
```

---

## 5단계: 중복 처리 방지

### 프롬프트 12
```
X-Event-Id 헤더로 중복 체크해서 같은 이벤트 여러 번 와도 한 번만 처리되게 해줘
이미 있으면 duplicated 응답, 없으면 저장하고 received 응답
```

---
