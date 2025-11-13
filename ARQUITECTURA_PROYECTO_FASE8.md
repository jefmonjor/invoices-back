# EXPLORACIÓN COMPLETA - ARQUITECTURA DEL PROYECTO invoices-back
## Para implementación de FASE 8: Service Tests

**Fecha:** 13 Noviembre 2025  
**Rama:** claude/phase-8-service-tests-01W6m9QigjyFE8YAJGhbgRKc  
**Status:** ANÁLISIS COMPLETO

---

## 1. VISIÓN GENERAL DEL PROYECTO

### Tipo de Proyecto
- **Framework:** Spring Boot 3.4.4 + Java 21
- **Arquitectura:** Microservicios con Spring Cloud
- **Patrón:** Clean Architecture (implementado en invoice-service)
- **Testing:** JUnit 5, Mockito, AssertJ, JaCoCo (90%+ coverage requerido)
- **Persistencia:** PostgreSQL con Flyway + JPA/Hibernate
- **Mensajería:** Apache Kafka
- **Service Discovery:** Netflix Eureka
- **API Gateway:** Spring Cloud Gateway
- **Build:** Maven 3.x

### Stack Técnico Completo
```
VERSIÓN JAVA: 21 (LTS)
SPRING BOOT: 3.4.4
SPRING CLOUD: 2024.0.1
POSTGRESQL: Múltiples instancias (por servicio)
KAFKA: Para eventos asíncronos
JUNIT: 5.11.0
MOCKITO: Core + JUnit Jupiter integration
JACOCO: 0.8.11 (Code coverage)
CHECKSTYLE: Google checks
SPOTBUGS: Análisis estático
JASPERREPORTS: 7.0.2 (Invoice service)
MINIO: 8.5.7 (Document service)
JWT: jjwt 0.11.5
LOMBOK: Procesamiento de anotaciones
```

---

## 2. SERVICIOS EXISTENTES (4 principales)

### 2.1 INVOICE-SERVICE (Puerto 8081)
**Responsabilidad:** Gestión de facturas, ítems y generación de PDFs

#### Estructura Clean Architecture
```
invoice-service/src/main/java/com/invoices/invoice_service/
├── domain/                                    ← CAPA DOMINIO (Sin deps externas)
│   ├── entities/
│   │   ├── Invoice.java                      ← Entidad principal con lógica
│   │   ├── InvoiceItem.java                  ← Item de factura
│   │   └── InvoiceStatus.java                ← Enum (DRAFT, PENDING, PAID, CANCELLED)
│   ├── usecases/
│   │   ├── GetInvoiceByIdUseCase.java        ← Obtener factura
│   │   └── GeneratePdfUseCase.java           ← Generar PDF
│   ├── ports/                                 ← Interfaces (Dependency Inversion)
│   │   ├── InvoiceRepository.java            ← Port de persistencia
│   │   └── PdfGeneratorService.java          ← Port de PDF
│   └── exceptions/
│       ├── InvoiceNotFoundException.java
│       ├── InvalidInvoiceStateException.java
│       └── InvalidInvoiceNumberFormatException.java
│
├── infrastructure/                            ← CAPA INFRAESTRUCTURA (Adaptadores)
│   ├── persistence/
│   │   ├── entities/
│   │   │   ├── InvoiceJpaEntity.java         ← Modelo JPA (diferente de dominio)
│   │   │   └── InvoiceItemJpaEntity.java
│   │   ├── repositories/
│   │   │   ├── JpaInvoiceRepository.java     ← Interface JPA Spring Data
│   │   │   └── InvoiceRepositoryImpl.java    ← Implementación del port
│   │   └── mappers/
│   │       └── InvoiceJpaMapper.java         ← Entity domain ↔ JPA
│   ├── external/
│   │   └── jasper/
│   │       └── JasperPdfGeneratorService.java ← Implementación JasperReports
│   ├── config/
│   │   └── UseCaseConfiguration.java         ← Inyección de dependencias
│   └── external/
├── presentation/                              ← CAPA PRESENTACIÓN (Controladores)
│   ├── controllers/
│   │   └── InvoiceController.java            ← Endpoints REST
│   └── mappers/
│       └── InvoiceDtoMapper.java             ← DTO ↔ Dominio
│
├── kafka/
│   ├── InvoiceEvent.java                     ← Evento de dominio
│   └── InvoiceEventProducer.java             ← Productor Kafka
│
├── dto/
│   ├── InvoiceDTO.java
│   ├── InvoiceItemDTO.java
│   ├── CreateInvoiceItemRequest.java
│   └── UpdateInvoiceRequest.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── Excepciones varias
│
├── client/
│   └── Feign clients para otros servicios
│
└── config/
    └── Configuración general
```

#### Dependencias de Testing - invoice-service
```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>

<!-- Spring Boot Test (incluye TestRestTemplate, MockMvc, etc.) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Kafka Test -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ para assertions fluidas -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- H2 para tests (en-memoria) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### 2.2 USER-SERVICE (Puerto 8082)
**Responsabilidad:** Gestión de usuarios, autenticación y autorización

**Estructura:** Tradicional (no Clean Architecture yet)
```
user-service/src/main/java/com/invoices/user_service/
├── controller/
├── service/
├── entity/
├── repository/
├── dto/
├── mapper/
├── security/
└── exception/
```

**Dependencias Testing:** JUnit 5, Spring Boot Test, Spring Security Test

### 2.3 DOCUMENT-SERVICE (Puerto 8083)
**Responsabilidad:** Almacenamiento de PDFs en MinIO, gestión de documentos

**Características especiales:**
- Integración MinIO (8.5.7)
- Validación de archivos
- Gestión de almacenamiento en objeto

**Dependencias Testing:** JUnit 5, Spring Boot Test

### 2.4 TRACE-SERVICE (Puerto 8084)
**Responsabilidad:** Trazabilidad y auditoría de operaciones, consumidor Kafka

**Estructura:**
```
trace-service/src/main/java/com/invoices/trace_service/
├── controller/
├── service/
├── entity/
├── repository/
├── dto/
├── kafka/
│   └── InvoiceEventConsumer.java    ← Consume eventos de Invoice
├── exception/
└── config/
```

### 2.5 SERVICIOS DE INFRAESTRUCTURA
- **eureka-server (8761):** Service Discovery
- **gateway-service (8080):** API Gateway con CORS y enrutamiento
- **config-server:** Configuración centralizada

---

## 3. TESTS EXISTENTES (Partial Implementation)

### Tests Implementados: 4 archivos
```
invoice-service/src/test/java/com/invoices/invoice_service/
├── InvoiceServiceApplicationTests.java          (BasicTest)
├── domain/
│   ├── entities/
│   │   ├── InvoiceTest.java                    (271 líneas) ✅ UNIT TESTS
│   │   └── InvoiceItemTest.java                (182 líneas) ✅ UNIT TESTS
│   └── usecases/
│       ├── GetInvoiceByIdUseCaseTest.java      (116 líneas) ✅ UNIT TESTS
│       └── GeneratePdfUseCaseTest.java         (165 líneas) ✅ UNIT TESTS
```

**Total:** 734 líneas de tests existentes

### Patrón de Testing Implementado (AAA Pattern)
```java
@ExtendWith(MockitoExtension.class)
class GetInvoiceByIdUseCaseTest {
    
    @Mock
    private InvoiceRepository repository;
    
    private GetInvoiceByIdUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new GetInvoiceByIdUseCase(repository);
    }
    
    @Test
    void shouldReturnInvoiceWhenIdIsValid() {
        // ARRANGE
        Long invoiceId = 1L;
        Invoice expected = createTestInvoice(invoiceId);
        when(repository.findById(invoiceId)).thenReturn(Optional.of(expected));
        
        // ACT
        Invoice result = useCase.execute(invoiceId);
        
        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoiceId);
        verify(repository, times(1)).findById(invoiceId);
    }
    
    // Tests para casos de error, validación, etc.
}
```

### Tests Faltantes (por servicio)
- **invoice-service:** Controllers, Mappers, Repositories, Integration tests
- **user-service:** Todos los tests
- **document-service:** Todos los tests (solo ApplicationTest existe)
- **trace-service:** Todos los tests (solo ApplicationTest existe)
- **gateway-service:** Todos los tests (solo ApplicationTest existe)

---

## 4. ARCHIVOS DE CONFIGURACIÓN

### 4.1 Maven POM Files
```
/home/user/invoices-back/
├── eureka-server/pom.xml              (Spring Cloud Eureka Server)
├── gateway-service/pom.xml            (Spring Cloud Gateway)
├── user-service/pom.xml               (Basic dependencies)
├── invoice-service/pom.xml            (Rich + JasperReports + JaCoCo)
├── document-service/pom.xml           (MinIO integration)
├── trace-service/pom.xml              (Kafka consumer)
└── config-server/pom.xml              (Spring Cloud Config Server)
```

### 4.2 JaCoCo Configuration (invoice-service)
```xml
<!-- Code Coverage - Obligatorio 90% líneas, 85% branches -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <!-- Configurado para generar reports y enforcer mínimos -->
</plugin>

<!-- Checkstyle - Google checks -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
</plugin>

<!-- SpotBugs - Static analysis -->
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.2.0</version>
</plugin>
```

### 4.3 Application Configuration Files
```
application.yml (por servicio)
├── eureka-server/src/main/resources/application.yml
├── gateway-service/src/main/resources/application.yml
├── user-service/src/main/resources/application.yml
├── invoice-service/src/main/resources/application.yml
├── document-service/src/main/resources/application.yml
├── trace-service/src/main/resources/application.yml
└── config-server/src/main/resources/application.yml
```

**Características comunes:**
- Configuración de BD por servicio (datasource)
- JPA/Hibernate settings
- Flyway para migraciones
- Kafka configuration
- Eureka client registration

### 4.4 Environment Variables (.env.example)
```
# 5 variables de BD (host, port, name, user, password × 4 servicios)
USER_DB_*
INVOICE_DB_*
DOCUMENT_DB_*
TRACE_DB_*

# JWT Configuration
JWT_SECRET
JWT_EXPIRATION_MS
JWT_ISSUER

# Kafka
KAFKA_BOOTSTRAP_SERVERS

# CORS
CORS_ALLOWED_ORIGINS
CORS_ALLOWED_METHODS
```

---

## 5. DEPENDENCIAS INSTALADAS PARA TESTING

### Nivel: Test Scope (Solo en tests)

| Dependencia | Versión | Propósito |
|---|---|---|
| `junit-jupiter` | 5.11.0 | Framework de testing principal |
| `spring-boot-starter-test` | 3.4.4 | Spring testing utilities |
| `mockito-core` | (maven managed) | Mocking de dependencias |
| `mockito-junit-jupiter` | (maven managed) | Integración Mockito con JUnit 5 |
| `assertj-core` | (maven managed) | Assertions fluidas |
| `h2` | (maven managed) | BD en memoria para tests |
| `spring-kafka-test` | 3.4.4 | Testing de Kafka (invoice-service) |
| `spring-security-test` | 3.4.4 | Testing de Security (user-service) |

### Plugins Maven para Calidad

| Plugin | Versión | Propósito |
|---|---|---|
| `jacoco-maven-plugin` | 0.8.11 | Cobertura de código |
| `maven-checkstyle-plugin` | 3.3.1 | Validación de estilos |
| `spotbugs-maven-plugin` | 4.8.2.0 | Análisis de bugs |
| `spring-boot-maven-plugin` | (managed) | Build + Run |
| `openapi-generator-maven-plugin` | 7.0.1 | Generación desde OpenAPI |

---

## 6. ESTRUCTURA DE CARPETAS src/

### 6.1 invoice-service (Clean Architecture)
```
invoice-service/src/
├── main/
│   ├── java/com/invoices/invoice_service/
│   │   ├── domain/              ← Lógica de negocio PURA
│   │   ├── infrastructure/      ← Adaptadores técnicos
│   │   ├── presentation/        ← Controladores REST
│   │   ├── kafka/               ← Eventos y productores
│   │   ├── dto/                 ← Transfer Objects
│   │   ├── exception/           ← Manejo de excepciones
│   │   └── config/              ← Inyección de dependencias
│   └── resources/
│       ├── application.yml
│       ├── db/migration/        ← Scripts Flyway
│       └── openapi/             ← API spec YAML
└── test/
    └── java/com/invoices/invoice_service/
        └── domain/
            ├── entities/        ← Unit tests de entidades
            └── usecases/        ← Unit tests de casos de uso
```

### 6.2 user-service (Estructura Tradicional)
```
user-service/src/
├── main/
│   ├── java/com/invoices/user_service/
│   │   ├── controller/          ← REST endpoints
│   │   ├── service/             ← Lógica de negocio
│   │   ├── entity/              ← JPA entities
│   │   ├── repository/          ← Acceso a datos
│   │   ├── dto/
│   │   ├── mapper/
│   │   ├── security/            ← JWT, Spring Security
│   │   └── exception/
│   └── resources/
│       ├── application.yml
│       ├── db/migration/
│       └── openapi/
└── test/
    └── java/com/invoices/user_service/
        └── UserServiceApplicationTests.java  (basic)
```

### 6.3 document-service & trace-service (Estructura Similar a user-service)
```
{service}/src/
├── main/
│   ├── java/com/invoices/{service}/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── dto/
│   │   ├── kafka/               (trace-service)
│   │   ├── exception/
│   │   └── config/
│   └── resources/
└── test/
    └── {service}ApplicationTests.java
```

---

## 7. FASE 8: IMPLEMENTACIÓN DE TESTS

### Alcance de la Fase 8

**Objetivo:** Implementar tests unitarios y de integración para todos los servicios

#### 7.1 Tests a Implementar

```
INVOICE-SERVICE (CONTINUACIÓN)
✅ domain/entities/               (YA EXISTE - 271+182 líneas)
✅ domain/usecases/               (YA EXISTE - 116+165 líneas)
❌ infrastructure/persistence/    (FALTA)
   └── repositories/InvoiceRepositoryImplTest.java
   └── mappers/InvoiceJpaMapperTest.java
❌ presentation/                  (FALTA)
   └── controllers/InvoiceControllerTest.java
   └── mappers/InvoiceDtoMapperTest.java
❌ integration/                   (FALTA)
   └── InvoiceServiceIntegrationTest.java
   └── InvoiceKafkaIntegrationTest.java

USER-SERVICE
❌ service/                        (TODOS)
   └── UserServiceTest.java
   └── AuthenticationServiceTest.java
❌ controller/                    (TODOS)
   └── UserControllerTest.java
❌ repository/                    (TODOS)
   └── UserRepositoryTest.java
❌ security/                      (TODOS)
   └── JwtTokenProviderTest.java
❌ integration/                   (TODOS)
   └── UserServiceIntegrationTest.java

DOCUMENT-SERVICE
❌ service/
❌ controller/
❌ repository/
❌ integration/

TRACE-SERVICE
❌ service/
❌ controller/
❌ kafka/
❌ integration/
```

### 7.2 Tipos de Tests Requeridos

#### Unit Tests (@ExtendWith(MockitoExtension.class))
- Entidades de dominio
- Casos de uso (usecases)
- Servicios (con dependencias mockeadas)
- Mappers y convertidores
- Validadores

#### Controller Tests (@WebMvcTest)
- Verificar endpoints REST
- Validar request/response
- Verificar security (sin token = 401)
- Validar mapeos de DTOs

#### Repository Tests (@DataJpaTest)
- Operaciones CRUD
- Queries personalizadas
- Relaciones entre entidades
- Con BD H2 en memoria

#### Integration Tests (@SpringBootTest)
- Flujo completo request → response
- Interacción BD real (H2)
- Seguridad (JWT)
- Kafka producers/consumers

### 7.3 Estándares de Testing (FASE 8)

#### Estructura AAA (Arrange-Act-Assert)
```java
@Test
void shouldDoSomethingWhenConditionMet() {
    // ARRANGE - Preparar datos y mocks
    User user = createTestUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    
    // ACT - Ejecutar la acción
    User result = userService.getUserById(1L);
    
    // ASSERT - Verificar resultados
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    verify(userRepository, times(1)).findById(1L);
}
```

#### Convención de Nombres
- `shouldReturnXWhenYCondition()`
- `shouldThrowXExceptionWhenYCondition()`
- `shouldValidateXAndRejectInvalidY()`

#### Coverage Mínimo
- **Líneas:** 90%
- **Branches:** 85%
- **Métodos:** 100% (al menos un test por método)

#### Herramientas
- **Test Runner:** JUnit 5 (@Test, @BeforeEach, @ParameterizedTest)
- **Mocking:** Mockito (@Mock, @ExtendWith(MockitoExtension.class))
- **Assertions:** AssertJ (fluent API)
- **Coverage:** JaCoCo (reports generados automáticamente)

---

## 8. COMANDOS CLAVE PARA FASE 8

### 8.1 Compilar y correr tests
```bash
cd /home/user/invoices-back/invoice-service
mvn clean test                           # Run all tests
mvn test -Dtest=InvoiceTest             # Run specific test
mvn clean test jacoco:report            # Generate coverage report
```

### 8.2 Verificar Coverage
```bash
# Generar reporte
mvn clean test jacoco:report

# Ver reporte
open target/site/jacoco/index.html       # macOS
xdg-open target/site/jacoco/index.html  # Linux

# Verificar cobertura mínima
mvn jacoco:check
```

### 8.3 Validación de Calidad
```bash
mvn clean checkstyle:check              # Google style checks
mvn spotbugs:check                      # Static analysis
mvn verify                              # Full build + tests + quality
```

### 8.4 Ver tests disponibles
```bash
find . -name "*Test.java" -type f       # List all test files
grep -r "@Test" --include="*.java"      # Find all test methods
```

---

## 9. RESUMEN EJECUCIÓN

### ✅ HALLAZGOS CONFIRMADOS

1. **Servicios identificados (4):**
   - invoice-service (Puerto 8081) - Clean Architecture ✅
   - user-service (Puerto 8082) - Estructura tradicional
   - document-service (Puerto 8083) - Estructura tradicional
   - trace-service (Puerto 8084) - Kafka consumer

2. **Tests existentes (solo invoice-service):**
   - InvoiceTest.java (271 líneas)
   - InvoiceItemTest.java (182 líneas)
   - GetInvoiceByIdUseCaseTest.java (116 líneas)
   - GeneratePdfUseCaseTest.java (165 líneas)
   - Total: 734 líneas = 4 archivos test

3. **Configuración testing:**
   - JUnit 5 ✅
   - Mockito ✅
   - AssertJ ✅
   - JaCoCo (90% mínimo) ✅
   - H2 para BD en memoria ✅
   - Checkstyle (Google checks) ✅
   - SpotBugs (análisis estático) ✅

4. **Dependencias validadas:**
   - Todas las versiones en pom.xml son coherentes
   - Spring Boot 3.4.4 (LTS)
   - Java 21 (LTS)
   - Spring Cloud 2024.0.1 (compatible)

5. **Estructura carpetas src/:**
   - invoice-service: 8 niveles con Clean Architecture
   - user-service: 7 niveles con estructura tradicional
   - document-service: 7 niveles
   - trace-service: 7 niveles con Kafka integration

6. **Archivos configuración:**
   - 7 pom.xml (uno por servicio/infraestructura)
   - 7 application.yml (configuración por servicio)
   - .env.example (variables de entorno)
   - Maven Wrapper (.mvn/)

### 🎯 PRÓXIMOS PASOS (FASE 8)

1. **Completar tests invoice-service:**
   - [ ] InvoiceRepositoryImplTest (persistencia)
   - [ ] InvoiceControllerTest (presentación)
   - [ ] Integration tests (flujo completo)

2. **Implementar tests user-service:**
   - [ ] UserServiceTest
   - [ ] UserControllerTest
   - [ ] SecurityTest (JWT)

3. **Implementar tests document-service:**
   - [ ] DocumentServiceTest
   - [ ] MinIO integrationTest

4. **Implementar tests trace-service:**
   - [ ] KafkaConsumerTest
   - [ ] AuditLogServiceTest

5. **Validar coverage:** Mínimo 90% líneas, 85% branches

---

## REFERENCIAS RÁPIDAS

### Locations
- **Raíz proyecto:** `/home/user/invoices-back/`
- **Tests actuales:** `/home/user/invoices-back/invoice-service/src/test/java/com/invoices/invoice_service/`
- **Configuración:** `/home/user/invoices-back/*-service/src/main/resources/application.yml`

### Documentación importante
- `/home/user/invoices-back/README.md` - Overview y arquitectura
- `/home/user/invoices-back/PLAN_ACCION_EJECUTIVO.md` - Fases detalladas
- `/home/user/invoices-back/POST_MERGE_REFACTORING_PLAN.md` - Refactoring post-merge

### Versiones clave
- Spring Boot: 3.4.4
- Java: 21
- JUnit: 5.11.0
- PostgreSQL: Multiple instances (por servicio)
- Kafka: 3.x (from Spring managed)

