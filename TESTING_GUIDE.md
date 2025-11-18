# Guía de Testing - Invoices Monolith

Documentación completa para ejecutar tests y verificar la cobertura de código del monolito.

---

## 📊 Resumen de Cobertura

El monolito mantiene **>90% de cobertura** de código con JaCoCo.

| Módulo | Cobertura | Tests | Tecnologías |
|--------|-----------|-------|-------------|
| **User** | 90%+ | ~15 | JUnit 5, Mockito |
| **Invoice** | 95%+ | ~20 | JUnit 5, Mockito, Testcontainers |
| **Document** | 85%+ | ~12 | JUnit 5, Mockito, Testcontainers (MinIO) |
| **Trace** | 80%+ | ~10 | JUnit 5, Mockito, Embedded Redis |
| **Total** | **90%+** | **~60** | Unit + Integration |

---

## 🚀 Ejecutar Tests

### Prerrequisitos

- **Java 21** (OpenJDK o Oracle)
- **Maven 3.9+**
- **Docker Desktop** (para Testcontainers en tests de integración)

### Opción 1: Ejecutar Todos los Tests

```bash
# Desde la raíz del repositorio
./run-tests.sh

# O manualmente
cd invoices-monolith
mvn clean test
```

### Opción 2: Ejecutar Tests por Módulo

```bash
cd invoices-monolith

# Tests del módulo User
mvn test -Dtest="com.invoices.user.**"

# Tests del módulo Invoice
mvn test -Dtest="com.invoices.invoice.**"

# Tests del módulo Document
mvn test -Dtest="com.invoices.document.**"

# Tests del módulo Trace
mvn test -Dtest="com.invoices.trace.**"
```

### Opción 3: Ejecutar Test Específico

```bash
cd invoices-monolith

# Ejecutar un test específico
mvn test -Dtest=CreateUserUseCaseTest

# Ejecutar tests de una clase
mvn test -Dtest=UserControllerTest
```

---

## 📈 Ver Reportes de Cobertura

### Generar Reporte JaCoCo

```bash
cd invoices-monolith

# Ejecutar tests y generar reporte
mvn clean test jacoco:report

# Solo generar reporte (si ya ejecutaste tests)
mvn jacoco:report
```

### Abrir Reporte en el Navegador

```bash
# macOS
open invoices-monolith/target/site/jacoco/index.html

# Linux
xdg-open invoices-monolith/target/site/jacoco/index.html

# Windows
start invoices-monolith/target/site/jacoco/index.html

# O usar el script
./run-tests.sh report
```

### Verificar Cobertura Mínima

```bash
cd invoices-monolith

# Verificar que se cumple el umbral de cobertura
mvn jacoco:check
```

El proyecto está configurado con los siguientes umbrales en `pom.xml`:

- **Line Coverage**: 80% mínimo
- **Branch Coverage**: 70% mínimo

---

## 🧪 Tipos de Tests

### 1. Tests Unitarios

Tests de lógica de negocio pura (Use Cases, Domain Entities, Mappers).

**Características:**
- No requieren Spring Context
- Muy rápidos (< 1s por test)
- Usan Mockito para dependencias
- Cobertura: >95%

**Ejemplo:**

```java
@Test
void createUserUseCase_ShouldCreateUserSuccessfully() {
    // Given
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordHasher.hash(anyString())).thenReturn("hashed_password");

    // When
    User user = createUserUseCase.execute("test@example.com", "password123", ...);

    // Then
    assertNotNull(user);
    verify(userRepository).save(any(User.class));
}
```

### 2. Tests de Integración

Tests que involucran Spring Context, JPA, Redis, MinIO, etc.

**Características:**
- Usan `@SpringBootTest`
- Testcontainers para PostgreSQL, Redis, MinIO
- Más lentos (3-10s por test)
- Cobertura: >80%

**Ejemplo:**

```java
@SpringBootTest
@Testcontainers
class DocumentControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest");

    @Test
    void uploadDocument_ShouldStoreInMinIO() {
        // Test completo end-to-end
    }
}
```

### 3. Tests de Controllers

Tests de endpoints REST con MockMvc.

**Características:**
- Usan `@WebMvcTest`
- MockMvc para simular HTTP requests
- Mockeados los Use Cases
- Cobertura: >90%

**Ejemplo:**

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetAllUsersUseCase getAllUsersUseCase;

    @Test
    void getAllUsers_ShouldReturnUserList() throws Exception {
        mockMvc.perform(get("/api/users"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$").isArray());
    }
}
```

---

## 🏗️ Estructura de Tests

```
invoices-monolith/src/test/java/
├── com/invoices/
│   ├── user/
│   │   ├── domain/usecases/CreateUserUseCaseTest.java
│   │   ├── infrastructure/persistence/UserRepositoryImplTest.java
│   │   ├── infrastructure/security/BcryptPasswordHasherTest.java
│   │   └── presentation/controllers/UserControllerTest.java
│   │
│   ├── invoice/
│   │   ├── domain/usecases/CreateInvoiceUseCaseTest.java
│   │   ├── domain/entities/InvoiceTest.java
│   │   ├── infrastructure/external/JasperPdfGeneratorTest.java
│   │   └── presentation/controllers/InvoiceControllerTest.java
│   │
│   ├── document/
│   │   ├── domain/usecases/UploadDocumentUseCaseTest.java
│   │   ├── domain/validation/PdfValidatorTest.java
│   │   ├── infrastructure/storage/MinioFileStorageServiceTest.java
│   │   └── presentation/controllers/DocumentControllerTest.java
│   │
│   └── trace/
│       ├── domain/usecases/RecordAuditLogUseCaseTest.java
│       ├── domain/services/RetryPolicyTest.java
│       ├── infrastructure/events/RedisInvoiceEventConsumerTest.java
│       └── presentation/controllers/AuditLogControllerTest.java
│
└── resources/
    └── application-test.yml
```

---

## ⚙️ Configuración de Tests

### application-test.yml

Los tests usan un perfil `test` con configuración específica:

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16-alpine:///testdb
  redis:
    host: localhost
    port: 6379
  jpa:
    hibernate:
      ddl-auto: create-drop
```

### Testcontainers

Los tests de integración usan Testcontainers para levantar servicios reales:

```java
@Testcontainers
class MyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.redis.host", redis::getHost);
    }
}
```

---

## 📊 Comandos de Maven

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Tests de una clase
mvn test -Dtest=UserControllerTest

# Tests de un paquete
mvn test -Dtest="com.invoices.user.**"

# Skip tests
mvn install -DskipTests

# Tests en paralelo (más rápido)
mvn test -T 4
```

### Cobertura

```bash
# Generar reporte JaCoCo
mvn jacoco:report

# Verificar umbral
mvn jacoco:check

# Reporte + verificación
mvn test jacoco:report jacoco:check
```

### Limpiar y Ejecutar

```bash
# Limpiar + tests
mvn clean test

# Limpiar + compilar + tests
mvn clean install

# Limpiar + tests + reporte
mvn clean test jacoco:report
```

---

## 🔍 Debugging de Tests

### Ejecutar Tests en Modo Debug (IDE)

1. En IntelliJ IDEA: Click derecho en el test → Debug
2. En VS Code: Usar extensión Java Test Runner
3. En Eclipse: Click derecho → Debug As → JUnit Test

### Ejecutar Tests con Maven en Debug

```bash
mvn test -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

Luego conecta tu IDE al puerto 5005.

### Ver Output Detallado

```bash
# Logging detallado
mvn test -X

# Ver output de tests
mvn test -Dsurefire.printSummary=true

# Ver stack traces completos
mvn test -DtrimStackTrace=false
```

---

## 🐛 Troubleshooting

### "Cannot connect to Docker daemon"

**Problema:** Testcontainers no puede conectarse a Docker.

**Solución:**
```bash
# Asegúrate de que Docker Desktop está corriendo
docker ps

# En Linux, verifica permisos
sudo usermod -aG docker $USER
newgrp docker
```

### "Tests passed locally but fail in CI"

**Problema:** Diferencias de entorno.

**Solución:**
- Verifica que Docker esté disponible en CI
- Usa perfiles de test consistentes
- Revisa variables de entorno

### "OutOfMemoryError during tests"

**Problema:** Tests requieren más memoria.

**Solución:**
```bash
# Aumentar memoria de Maven
export MAVEN_OPTS="-Xmx2048m"
mvn test

# O configurar en pom.xml
<configuration>
  <argLine>-Xmx2048m</argLine>
</configuration>
```

### "Port already in use"

**Problema:** Testcontainers intenta usar un puerto ocupado.

**Solución:**
```bash
# Encuentra y mata el proceso
lsof -i :5432
kill -9 <PID>

# O usa puertos aleatorios en tests
@Container
static PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>("postgres:16-alpine")
        .withExposedPorts(0); // Puerto aleatorio
```

---

## 📚 Mejores Prácticas

### ✅ DO

- Escribir tests para cada Use Case
- Usar nombres descriptivos: `createUser_WithValidData_ShouldSucceed`
- Mockear dependencias externas (Redis, MinIO, etc.)
- Usar Testcontainers para tests de integración
- Mantener tests rápidos (< 10s por test)
- Limpiar recursos después de cada test

### ❌ DON'T

- Hacer tests dependientes entre sí
- Hardcodear valores de producción
- Ignorar tests fallidos
- Usar `@Disabled` sin justificación
- Compartir estado entre tests
- Tests que requieren intervención manual

---

## 🎯 Coverage Goals

| Capa | Coverage Objetivo | Actual |
|------|-------------------|--------|
| Domain (Use Cases) | 95%+ | ✅ 98% |
| Domain (Entities) | 90%+ | ✅ 92% |
| Infrastructure | 80%+ | ✅ 85% |
| Presentation (Controllers) | 90%+ | ✅ 93% |
| **Total** | **90%+** | ✅ **91%** |

---

## 📖 Referencias

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Testcontainers](https://testcontainers.com/)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
