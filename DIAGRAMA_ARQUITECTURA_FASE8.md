# DIAGRAMAS DE ARQUITECTURA - FASE 8 Service Tests

## 1. Arquitectura de Microservicios

```
                        ┌─────────────┐
                        │  Frontend   │ (React/Angular)
                        │  (3000)     │
                        └──────┬──────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  API Gateway         │
                    │  (8080)              │
                    │ - CORS Configuration│
                    │ - Route Management  │
                    │ - JWT Validation    │
                    └────────┬─────────────┘
                    ┌────────┼─────────────┐
                    │        │             │
          ┌─────────▼──┐ ┌──▼──────┐ ┌───▼───────┐
          │ USER-SERV  │ │INVOICE  │ │ DOCUMENT  │
          │ (8082)     │ │-SERVICE │ │-SERVICE   │
          │            │ │(8081)   │ │(8083)     │
          │ - Auth     │ │         │ │           │
          │ - Users    │ │ - PDF   │ │ - MinIO   │
          └────┬───────┘ │ - Items │ │ - Storage │
               │         └────┬────┘ └─────┬─────┘
               │              │            │
               │    ┌─────────▼────────────┤
               │    │                      │
               │    ▼                      ▼
               │  ┌─────────────────────────────┐
               │  │  TRACE-SERVICE             │
               │  │  (8084)                    │
               │  │  - Audit logging           │
               │  │  - Kafka event consumer    │
               │  └─────────────────────────────┘
               │         ▲
               │         │
               │    ┌────┴─────┐
               │    │          │
          ┌────▼─┐ ┌┴────┐ ┌──┴──┐
          │ BD1  │ │ BD2 │ │ BD3 │ (PostgreSQL × 4 instancias)
          │User  │ │Invoice │ │Doc │
          └──────┘ └─────┘ └─────┘

        ┌──────────────┐     ┌────────────────┐
        │  Eureka      │     │  Kafka         │
        │  (8761)      │     │  (9092)        │
        │  Discovery   │     │  Events        │
        └──────────────┘     └────────────────┘
```

---

## 2. Clean Architecture - invoice-service

```
        ┌─────────────────────────────────────────────────────┐
        │               PRESENTACIÓN (Controllers)             │
        │  - InvoiceController.java                           │
        │  - Valida requests, mapea DTOs                      │
        │  - HTTP 200/400/404/500                             │
        └──────────────────┬──────────────────────────────────┘
                           │ MapperDTO
                           ▼
        ┌─────────────────────────────────────────────────────┐
        │        APLICACIÓN (Use Cases / Orchestration)        │
        │  - GetInvoiceByIdUseCase                            │
        │  - GeneratePdfUseCase                               │
        │  - CreateInvoiceUseCase (etc)                       │
        └──────────────────┬──────────────────────────────────┘
                           │ Domain Objects
                           ▼
        ┌─────────────────────────────────────────────────────┐
        │           DOMINIO (Business Logic - PURA)           │
        │  - Invoice.java (entidad con validaciones)          │
        │  - InvoiceItem.java                                 │
        │  - InvoiceStatus enum                               │
        │  - Puertos (Interfaces):                            │
        │    - InvoiceRepository (persistencia)               │
        │    - PdfGeneratorService (generación)               │
        │  - Excepciones específicas del dominio              │
        └────────────────────────────────────────────────────┘
                 ▲                            ▲
                 │ (Dependencia invertida)   │
                 │                           │
        ┌────────┴─────────────────────────┴───────────┐
        │       INFRAESTRUCTURA (Adaptadores)          │
        │                                               │
        │  ┌─────────────────────────────────────────┐ │
        │  │  Persistencia (JPA)                     │ │
        │  │  - InvoiceRepositoryImpl                 │ │
        │  │  - InvoiceJpaEntity                     │ │
        │  │  - InvoiceJpaMapper                     │ │
        │  │  - JpaInvoiceRepository (Spring Data)   │ │
        │  └─────────────────────────────────────────┘ │
        │                                               │
        │  ┌─────────────────────────────────────────┐ │
        │  │  Generación de PDFs                     │ │
        │  │  - JasperPdfGeneratorService (JasperRep)│ │
        │  └─────────────────────────────────────────┘ │
        │                                               │
        │  ┌─────────────────────────────────────────┐ │
        │  │  Mensajería (Kafka)                     │ │
        │  │  - InvoiceEventProducer                 │ │
        │  │  - InvoiceEvent                         │ │
        │  └─────────────────────────────────────────┘ │
        │                                               │
        │  ┌─────────────────────────────────────────┐ │
        │  │  Configuración (DI Container)           │ │
        │  │  - UseCaseConfiguration.java            │ │
        │  └─────────────────────────────────────────┘ │
        └───────────────────────────────────────────────┘
                      ▲ Dependencies Injected
                      │
        ┌─────────────┴───────────────────────────────┐
        │  Spring Framework (Spring Boot, Data JPA,   │
        │  Kafka, Cloud, Security, etc)               │
        └─────────────────────────────────────────────┘
```

---

## 3. Flujo de Testing por Capa

```
┌───────────────────────────────────────────────────────────────┐
│                  TESTING PIRAMIDE                             │
└───────────────────────────────────────────────────────────────┘

                         INTEGRACIÓN
                      ┌────────────┐
                      │  @Spring   │  Integration Tests
                      │  BootTest  │  - Flujo end-to-end
                      │            │  - Con BD H2 real
                      │  Cantidad: │  - Kafka topics
                      │  Pocos     │  - 2-3 tests por feature
                      └────────────┘
                             ▲
                        ┌────┼────┐
                        │    │    │
              ┌─────────▼──┐ │ ┌──▼──────────┐
              │ @WebMvcTest│ │ │@DataJpaTest │
              │Controller  │ │ │Repository   │
              │Tests       │ │ │Tests        │
              │- MockMvc   │ │ │- H2 in-mem  │
              │- Requests  │ │ │- CRUD ops   │
              │- Security  │ │ │- Queries    │
              └────────────┘ │ └─────────────┘
                    ▲        │       ▲
                    └────────┼───────┘
                             │
                      ┌──────▼────────────────────┐
                      │ UNIDAD (@ExtendWith)      │
                      │ Mockito                   │
                      │ - Entidades dominio       │
                      │ - UseCases                │
                      │ - Services (con mocks)    │
                      │ - Mappers                 │
                      │ Cantidad: MUCHOS tests    │
                      │ (90%+ del coverage)       │
                      └───────────────────────────┘
```

---

## 4. Patrón de Testing Actual (invoice-service)

```
TEST FILE STRUCTURE (AAA Pattern):

┌────────────────────────────────────────────────────────────┐
│ GetInvoiceByIdUseCaseTest.java                             │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  @ExtendWith(MockitoExtension.class)   ◄─── Enables @Mock │
│  class GetInvoiceByIdUseCaseTest {                         │
│                                                             │
│    @Mock                               ◄─── Mocked dep.   │
│    InvoiceRepository repository;                           │
│                                                             │
│    GetInvoiceByIdUseCase useCase;      ◄─── Object being  │
│                                              tested (real)  │
│    @BeforeEach                         ◄─── Setup         │
│    void setUp() {                                          │
│      useCase = new GetInvoiceByIdUseCase(repository);      │
│    }                                                        │
│                                                             │
│    @Test                               ◄─── Test method   │
│    void shouldReturnInvoiceWhenIdIsValid() {              │
│      // ARRANGE (Preparar)                                │
│      Long invoiceId = 1L;                                 │
│      Invoice expected = createTestInvoice(invoiceId);     │
│      when(repository.findById(invoiceId))                 │
│        .thenReturn(Optional.of(expected));                │
│                                                            │
│      // ACT (Ejecutar)                                    │
│      Invoice result = useCase.execute(invoiceId);         │
│                                                            │
│      // ASSERT (Verificar)                                │
│      assertThat(result).isNotNull();                      │
│      assertThat(result.getId()).isEqualTo(invoiceId);     │
│      verify(repository, times(1)).findById(invoiceId);    │
│    }                                                       │
│  }                                                         │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 5. Estructura de Directorios - Mapeo Tests

```
ANTES (Parcial - actual):

/invoice-service/src/
├── main/java/com/invoices/invoice_service/
│   ├── domain/
│   │   ├── entities/             ✅ InvoiceTest.java
│   │   ├── usecases/             ✅ GetInvoiceByIdUseCaseTest.java
│   │   └── ports/
│   ├── infrastructure/           ❌ (Tests faltantes)
│   ├── presentation/             ❌ (Tests faltantes)
│   └── kafka/                    ❌ (Tests faltantes)
└── test/java/com/invoices/invoice_service/
    └── domain/
        ├── entities/
        └── usecases/


DESPUÉS (Fase 8 - Completo):

/invoice-service/src/
├── main/java/com/invoices/invoice_service/
│   ├── domain/
│   │   ├── entities/             ✅ InvoiceTest.java
│   │   ├── usecases/             ✅ GetInvoiceByIdUseCaseTest.java
│   │   └── ports/
│   ├── infrastructure/
│   │   ├── persistence/          [NEW] RepositoryTest + MapperTest
│   │   └── external/             [NEW] JasperTest
│   ├── presentation/             [NEW] ControllerTest + MapperTest
│   └── kafka/                    [NEW] ProducerTest
└── test/java/com/invoices/invoice_service/
    ├── domain/
    │   ├── entities/             ✅ Existing
    │   └── usecases/             ✅ Existing
    ├── infrastructure/           [NEW] RepositoryTest, MapperTest
    ├── presentation/             [NEW] ControllerTest, MapperTest
    ├── kafka/                    [NEW] ProducerTest
    └── integration/              [NEW] E2E Tests
```

---

## 6. Dependencias de Testing - Flujo

```
┌──────────────────────────────────┐
│ pom.xml Dependencies             │
├──────────────────────────────────┤
│                                  │
│  junit-jupiter (5.11.0)          │ ◄─ Test runner
│  spring-boot-starter-test        │ ◄─ Spring testing
│  mockito-core                    │ ◄─ Mocking framework
│  assertj-core                    │ ◄─ Fluent assertions
│  h2                              │ ◄─ In-memory DB
│  spring-kafka-test               │ ◄─ Kafka testing
│  [spring-security-test]          │ ◄─ For user-service
│                                  │
└──────┬───────────────────────────┘
       │ Maven Plugins:
       │ - jacoco-maven-plugin (0.8.11)
       │ - maven-checkstyle-plugin
       │ - spotbugs-maven-plugin
       │
       ▼
┌──────────────────────────────────┐
│ mvn test (Maven goals)           │
├──────────────────────────────────┤
│ 1. Compile test code             │
│ 2. Run all *Test.java files      │
│ 3. Generate coverage reports     │
│ 4. Enforce coverage minimums      │
│    (90% lines, 85% branches)      │
│ 5. Check code style              │
│ 6. Run static analysis           │
└──────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ Test Results                     │
├──────────────────────────────────┤
│ - Console output (PASS/FAIL)     │
│ - target/surefire-reports/       │
│ - target/site/jacoco/index.html  │
└──────────────────────────────────┘
```

---

## 7. Mapa de Cobertura de Tests (Porcentajes objetivo)

```
INVOICE-SERVICE COVERAGE TARGET: 90% líneas, 85% branches

DOMAIN LAYER (100% coverage - CRÍTICO):
  ├─ entities/
  │  └─ Invoice.java                    100%  ✅ (existente)
  │  └─ InvoiceItem.java                100%  ✅ (existente)
  ├─ usecases/
  │  └─ GetInvoiceByIdUseCase.java       100%  ✅ (existente)
  │  └─ GeneratePdfUseCase.java          100%  ✅ (existente)
  └─ ports/ & exceptions/               100%  (validar)

INFRASTRUCTURE LAYER (90% coverage):
  ├─ persistence/repositories/
  │  └─ InvoiceRepositoryImpl.java        90%  ❌ (FALTA test)
  ├─ persistence/mappers/
  │  └─ InvoiceJpaMapper.java            90%  ❌ (FALTA test)
  ├─ external/jasper/
  │  └─ JasperPdfGeneratorService.java   85%  ❌ (FALTA test)
  └─ config/
     └─ UseCaseConfiguration.java        N/A  (Configuration)

PRESENTATION LAYER (90% coverage):
  ├─ controllers/
  │  └─ InvoiceController.java           90%  ❌ (FALTA test @WebMvcTest)
  └─ mappers/
     └─ InvoiceDtoMapper.java            90%  ❌ (FALTA test)

KAFKA/EVENTS (85% coverage):
  └─ InvoiceEventProducer.java           85%  ❌ (FALTA test)

INTEGRATION (3-5 tests end-to-end):
  ├─ InvoiceServiceIntegrationTest       N/A  ❌ (FALTA test @SpringBootTest)
  └─ InvoiceKafkaIntegrationTest         N/A  ❌ (FALTA test)

GLOBAL TARGET: 90% líneas + 85% branches
```

---

## 8. Comparación: Antes vs Después de Fase 8

```
┌────────────────────────┬─────────────────┬─────────────────┐
│                        │   ANTES (Actual)│   DESPUÉS (Fase8│
├────────────────────────┼─────────────────┼─────────────────┤
│ Test Files             │  4 files        │  15+ files      │
│ Test Lines of Code     │  734 lines      │  2000+ lines    │
│ Coverage (lines)       │  ~40%           │  90%+           │
│ Coverage (branches)    │  ~30%           │  85%+           │
├────────────────────────┼─────────────────┼─────────────────┤
│ Unit Tests             │  4 (domain)     │  12+ (all layers│
│ Controller Tests       │  0              │  2-3            │
│ Repository Tests       │  0              │  1-2            │
│ Integration Tests      │  0              │  2-3            │
│ Kafka Tests            │  0              │  1              │
├────────────────────────┼─────────────────┼─────────────────┤
│ Services w/ Tests      │  1 (invoice)    │  4 (all)        │
│ Checkstyle Passing     │  50%            │  100%           │
│ SpotBugs Issues        │  N/A            │  0 críticos     │
│ JaCoCo Enforcement     │  No             │  Sí (90%+)      │
└────────────────────────┴─────────────────┴─────────────────┘
```

---

## 9. Testing Checklist por Servicio

```
INVOICE-SERVICE:
┌─────────────────────────────────────────────┐
│ Domain Layer Tests          │ Status        │
├─────────────────────────────┼───────────────┤
│ InvoiceTest                 │ ✅ Done       │
│ InvoiceItemTest             │ ✅ Done       │
│ GetInvoiceByIdUseCaseTest   │ ✅ Done       │
│ GeneratePdfUseCaseTest      │ ✅ Done       │
└─────────────────────────────┴───────────────┘

┌─────────────────────────────────────────────┐
│ Infrastructure Tests        │ Status        │
├─────────────────────────────┼───────────────┤
│ InvoiceRepositoryImplTest   │ ❌ TODO       │
│ InvoiceJpaMapperTest        │ ❌ TODO       │
│ JasperPdfGeneratorTest      │ ❌ TODO       │
└─────────────────────────────┴───────────────┘

┌─────────────────────────────────────────────┐
│ Presentation Tests          │ Status        │
├─────────────────────────────┼───────────────┤
│ InvoiceControllerTest       │ ❌ TODO       │
│ InvoiceDtoMapperTest        │ ❌ TODO       │
└─────────────────────────────┴───────────────┘

┌─────────────────────────────────────────────┐
│ Other Tests                 │ Status        │
├─────────────────────────────┼───────────────┤
│ InvoiceEventProducerTest    │ ❌ TODO       │
│ Integration Tests (E2E)     │ ❌ TODO       │
│ Kafka Integration Tests     │ ❌ TODO       │
└─────────────────────────────┴───────────────┘


USER-SERVICE: (Crear todos)
┌─────────────────────────────────────────────┐
│ Service Tests               │ Status        │
├─────────────────────────────┼───────────────┤
│ UserServiceTest             │ ❌ TODO       │
│ AuthenticationServiceTest   │ ❌ TODO       │
└─────────────────────────────┴───────────────┘

[Continúa para Document y Trace Services...]
```

---

## 10. Comando Maven Build Flow

```
mvn clean install
    │
    ├─ clean: Delete target/
    │
    ├─ compile: Compile source code
    │   └─ src/main/java → target/classes
    │
    ├─ test-compile: Compile test code
    │   └─ src/test/java → target/test-classes
    │
    ├─ test: Run tests
    │   ├─ [1] Find all *Test.java classes
    │   ├─ [2] Execute @Test methods
    │   ├─ [3] Generate surefire reports
    │   └─ [4] JaCoCo instrument and report
    │
    ├─ package: Create JAR
    │   └─ target/invoice-service-0.0.1-SNAPSHOT.jar
    │
    ├─ verify: Quality checks
    │   ├─ jacoco:check (90% lines, 85% branches)
    │   ├─ checkstyle:check (Google style)
    │   └─ spotbugs:check (No critical bugs)
    │
    └─ install: Install to local repo
        └─ ~/.m2/repository/com/invoices/...
```

---

## Referencias Rápidas

**Ubicaciones:**
- Raíz: `/home/user/invoices-back/`
- Tests invoice-service: `/home/user/invoices-back/invoice-service/src/test/java/`
- POM: `/home/user/invoices-back/invoice-service/pom.xml`

**Comandos frecuentes:**
```bash
cd /home/user/invoices-back/invoice-service
mvn clean test                    # Run tests
mvn test -Dtest=InvoiceTest      # Specific test
mvn jacoco:report                 # Coverage report
mvn clean verify                  # Full quality check
```

