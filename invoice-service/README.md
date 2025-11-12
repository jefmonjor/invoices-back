# Invoice Service - Clean Architecture Implementation

## Descripción
Microservicio para gestión de facturas con generación de PDFs usando JasperReports. Implementado siguiendo **Clean Architecture** y **Clean Code** principles.

## Arquitectura

```
invoice-service/
├── domain/                    # ⭐ CAPA DOMINIO (Sin dependencias externas)
│   ├── entities/              # Entidades con lógica de negocio
│   │   ├── Invoice.java
│   │   ├── InvoiceItem.java
│   │   └── InvoiceStatus.java
│   ├── usecases/              # Casos de uso (reglas de negocio puras)
│   │   ├── GetInvoiceByIdUseCase.java
│   │   └── GeneratePdfUseCase.java
│   ├── ports/                 # Interfaces (Dependency Inversion)
│   │   ├── InvoiceRepository.java
│   │   └── PdfGeneratorService.java
│   └── exceptions/            # Excepciones de dominio
│       ├── InvoiceNotFoundException.java
│       ├── InvalidInvoiceStateException.java
│       └── InvalidInvoiceNumberFormatException.java
│
├── application/               # ⭐ CAPA APLICACIÓN (Orquestación)
│   └── services/              # Servicios de aplicación (no implementados aún)
│
├── infrastructure/            # ⭐ CAPA INFRAESTRUCTURA (Adaptadores)
│   ├── persistence/           # Adaptador JPA
│   │   ├── entities/          # JPA Entities (separadas del dominio)
│   │   │   ├── InvoiceJpaEntity.java
│   │   │   └── InvoiceItemJpaEntity.java
│   │   ├── repositories/      # Spring Data JPA
│   │   │   ├── JpaInvoiceRepository.java
│   │   │   └── InvoiceRepositoryImpl.java (implementa port)
│   │   └── mappers/           # Mappers Domain ↔ JPA
│   │       └── InvoiceJpaMapper.java
│   ├── external/              # Adaptadores externos
│   │   └── jasper/            # JasperReports adapter
│   │       └── JasperPdfGeneratorService.java
│   └── config/                # Configuración Spring
│       └── UseCaseConfiguration.java
│
└── presentation/              # ⭐ CAPA PRESENTACIÓN (Entrada)
    ├── controllers/           # REST Controllers
    │   └── InvoiceController.java
    └── mappers/               # Mappers Domain ↔ DTO
        └── InvoiceDtoMapper.java
```

## Endpoints (OpenAPI)

### GET /invoices/{id}
Obtener factura por ID.

**Request:**
```bash
curl -X GET http://localhost:8081/invoices/1
```

**Response 200:**
```json
{
  "id": 1,
  "userId": 10,
  "clientId": 20,
  "invoiceNumber": "2025-001",
  "issueDate": "2025-03-14T10:00:00Z",
  "baseAmount": 1000.00,
  "irpfPercentage": 15.00,
  "irpfAmount": 150.00,
  "rePercentage": 5.00,
  "reAmount": 50.00,
  "totalAmount": 1210.00,
  "status": "Pendiente",
  "items": [...]
}
```

### POST /invoices/generate-pdf
Generar PDF personalizado.

**Request:**
```bash
curl -X POST http://localhost:8081/invoices/generate-pdf \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceNumber": "2025-001",
    "baseAmount": 1000.00,
    "irpfPercentage": 15.00,
    "rePercentage": 5.00,
    "totalAmount": 1210.00,
    "color": "#FFFFFF",
    "textStyle": "bold"
  }' \
  --output invoice.pdf
```

**Response 200:** PDF binary (application/pdf)

## Reglas de Negocio Implementadas

### Invoice Entity
- Validación de formato de número de factura: `YYYY-XXX` (e.g., `2025-001`)
- Cálculo automático de base, IRPF, RE y total
- Estados del ciclo de vida:
  - `DRAFT` → `PENDING` → `PAID`
  - `DRAFT` → `CANCELLED`
- No se puede modificar factura `FINALIZED` o `PAID`
- No se puede cancelar factura `PAID`
- No se puede marcar como pendiente sin items

### InvoiceItem Entity
- Validación de campos obligatorios
- Cálculo de subtotal con descuento
- Cálculo de total con IVA
- Validaciones:
  - Units > 0
  - Price > 0
  - VAT ≥ 0
  - Discount entre 0-100%

## Tests Unitarios

### Cobertura Objetivo: 90%+
```bash
# Ejecutar tests
mvn clean test

# Ver reporte de cobertura
mvn jacoco:report
# Reporte en: target/site/jacoco/index.html
```

### Tests Implementados
- ✅ `InvoiceTest.java` - 15 tests (entidad Invoice)
- ✅ `InvoiceItemTest.java` - 12 tests (entidad InvoiceItem)
- ✅ `GetInvoiceByIdUseCaseTest.java` - 6 tests (caso de uso)
- ✅ `GeneratePdfUseCaseTest.java` - 7 tests (caso de uso)

**Total: 40+ tests**

## Calidad de Código

### JaCoCo (Cobertura)
```bash
mvn jacoco:check  # Falla si < 90% líneas o < 85% ramas
```

### Checkstyle (Estilo)
```bash
mvn checkstyle:check  # Valida Google Java Style
```

### SpotBugs (Análisis Estático)
```bash
mvn spotbugs:check  # Detecta bugs potenciales
```

### Ejecutar todo
```bash
mvn clean verify  # Compila, tests, cobertura, calidad
```

## Dependencias Principales

- **Spring Boot 3.4.4** (Web, Data JPA, Cloud Config)
- **Spring Cloud 2024.0.1** (Eureka Client)
- **PostgreSQL** (producción) / **H2** (tests)
- **JasperReports 7.0.2** (generación de PDFs)
- **OpenAPI Generator 7.0.1** (generación de APIs)
- **JUnit 5 + Mockito + AssertJ** (tests)

## Configuración

### application.yml
```yaml
spring:
  application:
    name: invoice-service
  datasource:
    url: jdbc:postgresql://localhost:5432/invoices_db
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

server:
  port: 8081

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

## Ejecución

### Con Maven
```bash
# Compilar
mvn clean install -DskipTests

# Ejecutar
mvn spring-boot:run
```

### Con Java
```bash
java -jar target/invoice-service-0.0.1-SNAPSHOT.jar
```

## Principios Aplicados (SOLID)

- **S**ingle Responsibility: Cada clase una única responsabilidad
- **O**pen/Closed: Abierto a extensión, cerrado a modificación
- **L**iskov Substitution: Interfaces bien definidas (ports)
- **I**nterface Segregation: Interfaces pequeñas y específicas
- **D**ependency Inversion: Dominio no depende de infraestructura

## Mejoras Futuras (Roadmap)

- [ ] Implementar CreateInvoiceUseCase
- [ ] Implementar UpdateInvoiceUseCase
- [ ] Agregar eventos Kafka (InvoiceCreatedEvent, InvoicePaidEvent)
- [ ] Implementar circuit breaker (Resilience4j)
- [ ] Agregar tests de integración (@SpringBootTest)
- [ ] Implementar validaciones con Bean Validation
- [ ] Agregar logs estructurados (ELK Stack)
- [ ] Implementar cache (Redis)

## Contacto

Para dudas o contribuciones, revisa el [README principal](../README.md).
