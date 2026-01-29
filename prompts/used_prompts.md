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
