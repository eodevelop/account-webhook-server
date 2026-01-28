# Kotlin + Spring Boot Code Style Guide

> Code conventions, architecture patterns, and style guide for Kotlin/Spring Boot projects

---

## 1. Package Structure

### 1.1 Project Structure

```
src/main/kotlin/com/service/accountwebhookserver/
├── AccountWebhookServerApplication.kt   # Spring Boot entry point
├── config/                              # Configuration classes (Bean, Security, etc.)
├── controller/                          # REST API controllers
├── entity/                              # JPA entities, domain models
├── model/                               # DTOs, Request/Response models
├── repository/                          # Data access layer
├── service/                             # Business logic
└── util/                                # Utilities, helper classes
```

### 1.2 Package Responsibilities

| Package | Responsibility | Examples |
|---------|----------------|----------|
| `config` | Spring configuration, Bean definitions, Security settings | `WebConfig`, `SecurityConfig` |
| `controller` | REST API endpoints, request/response handling | `WebhookController` |
| `entity` | DB table mapping entities, domain objects | `Account`, `WebhookEvent` |
| `model` | DTOs, Request/Response, Projections | `WebhookRequest`, `AccountDto` |
| `repository` | JPA Repository, query methods | `AccountRepository` |
| `service` | Business logic, transaction management | `WebhookService` |
| `util` | Common utilities, constants, helper functions | `DateUtils`, `Constants` |

### 1.3 Dependency Rules

```
Controller → Service → Repository → Entity
               ↓           ↓
             Model       Model
               ↓
            Config (configuration injection)
             Util (utility usage)
```

- Controller only calls Service
- Service can call Repository and other Services
- Repository only references Entity
- Util does not reference any package

---

## 2. Layer Structure

```
Controller Layer    Request/response handling, input validation
       ↓
Service Layer       Business logic, transaction management
       ↓
Repository Layer    Data access, JPA queries
       ↓
Entity Layer        Entities, domain models
```

---

## 3. Coding Conventions

### 3.1 Naming Rules

| Target | Convention | Example |
|--------|------------|---------|
| Class | PascalCase | `UserService`, `OrderRepository` |
| Function/Variable | camelCase | `getUserById`, `orderCount` |
| Constant | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Package | lowercase | `com.example.project` |
| Table | snake_case | `user_order_map` |
| Column | snake_case | `created_at` |

### 3.2 ktlint

```bash
# Check
./gradlew ktlintCheck

# Auto-fix
./gradlew ktlintFormat
```

---

## 4. Controller Pattern

```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: String): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.getUser(id))
    }

    @PostMapping
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(userService.createUser(request))
    }

    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.updateUser(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: String): ResponseEntity<Unit> {
        userService.deleteUser(id)
        return ResponseEntity.noContent().build()
    }
}
```

**Rules:**
- Use constructor injection
- Validate input with `@Valid`
- Return appropriate HTTP status codes
- Delegate business logic to Service

---

## 5. Service Pattern

```kotlin
@Service
@Transactional
class UserService(
    // 1. Repository dependencies
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
) {
    // public methods: business logic
    fun createUser(request: CreateUserRequest): UserResponse {
        validateCreateRequest(request)

        val user = userRepository.save(request.toEntity())

        return user.toResponse()
    }

    fun getUser(id: String): UserResponse {
        val user = userRepository.findByPk(id)
            ?: throw ApiException(ApiErrorCode.USER_NOT_FOUND)
        return user.toResponse()
    }

    // private methods: helpers/validation
    private fun validateCreateRequest(request: CreateUserRequest) {
        if (userRepository.existsByEmail(request.email)) {
            throw ApiException(ApiErrorCode.EMAIL_ALREADY_EXISTS)
        }
    }
}
```

**Rules:**
- Class-level `@Transactional`
- Constructor injection
- Ensure immutability with `private val`
- Separate validation logic into private methods
- Use custom ApiException for exceptions

---

## 6. Repository Pattern (Spring Data JPA)

### 6.1 Entity Definition

```kotlin
@Entity
@Table(name = "user")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "pk", unique = true, nullable = false)
    val pk: String,

    @Column(name = "email", unique = true, nullable = false)
    val email: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null,
)
```

### 6.2 Repository Implementation

```kotlin
@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByPk(pk: String): User?

    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean

    fun findAllByStatus(status: UserStatus, pageable: Pageable): Page<User>

    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword%")
    fun searchByName(@Param("keyword") keyword: String): List<User>
}
```

**Rules:**
- Utilize Spring Data JPA query methods
- Use `@Query` for complex queries
- Return nullable for single-item queries

---

## 7. Entity / Model Pattern

### 7.1 Entity (entity package)

```kotlin
@Entity
@Table(name = "user")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "pk", unique = true)
    val pk: String,

    @Column(name = "email")
    val email: String,

    @Column(name = "name")
    var name: String,
)
```

### 7.2 Model (model package)

```kotlin
// Request
data class CreateUserRequest(
    @field:NotBlank
    val email: String,

    @field:Size(min = 2, max = 50)
    val name: String,
)

// Response
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: Instant,
)

// DTO
data class UserDto(
    val pk: String,
    val email: String,
    val name: String,
)

// Conversion extension functions
fun CreateUserRequest.toEntity(pk: String) = User(
    pk = pk,
    email = email,
    name = name,
)

fun User.toResponse() = UserResponse(
    id = pk,
    email = email,
    name = name,
    createdAt = createdAt,
)
```

---

## 8. Test Code Patterns

### 8.1 Service Test

```kotlin
@SpringBootTest
@Transactional
class UserServiceTest {

    @MockkBean
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `can create user`() {
        // Given
        val request = CreateUserRequest(
            email = "test@example.com",
            name = "Test"
        )

        // When
        val result = userService.createUser(request)

        // Then
        assertThat(result.email).isEqualTo("test@example.com")
        assertThat(result.name).isEqualTo("Test")
    }

    @Test
    fun `throws exception when user not found`() {
        // Given
        val nonExistentId = "non-existent-id"

        // When & Then
        assertThrows<ApiException> {
            userService.getUser(nonExistentId)
        }.also {
            assertThat(it.errorCode).isEqualTo(ApiErrorCode.USER_NOT_FOUND)
        }
    }
}
```

### 8.2 Controller Test

```kotlin
@WebMvcTest(UserController::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userService: UserService

    @Test
    fun `GET user returns success`() {
        // Given
        val userId = "user-123"
        val response = UserResponse(
            id = userId,
            email = "test@example.com",
            name = "Test",
            createdAt = Instant.now()
        )
        every { userService.getUser(userId) } returns response

        // When & Then
        mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.email").value("test@example.com"))
    }
}
```

### 8.3 Test Rules

```kotlin
// Test method naming: backticks + descriptive name
@Test
fun `can create order`() { }

@Test
fun `cannot order when out of stock`() { }

// Use Given-When-Then pattern
@Test
fun `example test`() {
    // Given - prepare test data
    val input = ...

    // When - execute test target
    val result = service.doSomething(input)

    // Then - verify results
    assertThat(result).isEqualTo(expected)
}

// MockK usage
every { mockService.method(any()) } returns expectedValue
verify { mockService.method(any()) }
verify(exactly = 1) { mockService.method(any()) }
```

---

## 9. Exception Handling Pattern

### 9.1 Custom Exception

```kotlin
class ApiException(
    val errorCode: ApiErrorCode,
    override val message: String? = null,
    override val cause: Throwable? = null,
) : RuntimeException(message ?: errorCode.message, cause)

enum class ApiErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // 400 Bad Request
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),

    // 409 Conflict
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),

    // 500 Internal Server Error
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
}
```

### 9.2 Global Exception Handler

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApiException(e: ApiException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ErrorResponse(
                code = e.errorCode.name,
                message = e.message ?: e.errorCode.message
            ))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                code = "VALIDATION_ERROR",
                message = message
            ))
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
)
```

---

## 10. Config Pattern

```kotlin
@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
    }
}

@Configuration
class ObjectMapperConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
```

---

## 11. DB Design Principles

```
1. Primary Key
   - auto-incremental bigint (id) + UUID (pk)
   - id: for DB internal optimization
   - pk: application identifier

2. Default Columns
   - created_at: required (DEFAULT CURRENT_TIMESTAMP)
   - updated_at: when needed (ON UPDATE CURRENT_TIMESTAMP)

3. Indexes
   - Columns used in query conditions
   - Mapping tables: composite index (key1, key2)

4. Foreign Keys
   - Minimize FK constraints
   - Manage at application level
```
---

## 12. Build Commands

```bash
# Build
./gradlew build
./gradlew build -x test

# Test
./gradlew test

# Code style
./gradlew ktlintCheck
./gradlew ktlintFormat

# Run
./gradlew bootRun --args='--spring.profiles.active=local'
```
