# Invoice Management System - Clean Architecture
**Versión: 1.0** | **Fecha: 12 de Noviembre de 2025** | **Powered by Clean Architecture + Spring Boot**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Clean Architecture](https://img.shields.io/badge/Architecture-Clean-blue.svg)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
[![Code Coverage](https://img.shields.io/badge/Coverage-90%25+-success.svg)](https://www.jacoco.org/)

---

## ⚠️ REGLAS INQUEBRANTABLES - CÓDIGO LIMPIO Y PROFESIONAL

Este proyecto **NO TOLERA** código de baja calidad. Toda implementación debe adherirse **ESTRICTAMENTE** a los principios de **Clean Architecture** y **Clean Code** de Uncle Bob. Cualquier desviación es considerada **FALLO CRÍTICO** y debe refactorizarse inmediatamente.

### Principios Fundamentales

#### 1. Clean Architecture (Arquitectura Limpia)
```
┌─────────────────────────────────────────────────────┐
│                   FRAMEWORKS                        │
│  (Spring Boot, JPA, Kafka, JasperReports)          │
│  ┌───────────────────────────────────────────────┐ │
│  │          INTERFACE ADAPTERS                   │ │
│  │  (Controllers, Repositories, Kafka Producers) │ │
│  │  ┌─────────────────────────────────────────┐ │ │
│  │  │        APPLICATION BUSINESS RULES       │ │ │
│  │  │  (Use Cases - Casos de Uso)            │ │ │
│  │  │  ┌───────────────────────────────────┐ │ │ │
│  │  │  │   ENTERPRISE BUSINESS RULES       │ │ │ │
│  │  │  │   (Entities - Entidades Dominio)  │ │ │ │
│  │  │  └───────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘

        Dependencias fluyen SOLO hacia ADENTRO →→→
```

**Regla de Dependencia:** Las capas internas **NUNCA** dependen de las externas. Domain no conoce Infrastructure ni Presentation.

#### 2. Clean Code - Estándares Obligatorios
- **Nombres Significativos:** No `usr`, `tmp`, `data`. Sí: `userRepository`, `invoiceEntity`, `generatePdfUseCase`.
- **Funciones Cortas:** Máximo 20 líneas. Un único propósito (SRP).
- **Clases con Responsabilidad Única:** Una razón para cambiar.
- **DRY:** Cero duplicación. Refactoriza siempre.
- **Comentarios Mínimos:** El código se explica solo. Solo comenta el "por qué", nunca el "qué".
- **Manejo de Errores Explícito:** Excepciones específicas (`InvoiceNotFoundException`), no genéricas.
- **Formato Consistente:** 4 espacios, líneas < 120 caracteres, Checkstyle obligatorio.

#### 3. Tests Unitarios - Cobertura Mínima 90%
- **Estructura AAA:** Arrange, Act, Assert.
- **Nombres Descriptivos:** `shouldThrowExceptionWhenInvoiceIdIsInvalid()`.
- **Independientes:** Sin dependencias entre tests.
- **Mocks:** Mockea todo lo externo (DB, Kafka, APIs).
- **Herramientas:** JUnit 5, Mockito, JaCoCo.

---

## 📁 Estructura del Proyecto

### Arquitectura de Microservicios
```
invoices-back/
├── config-server/          # Configuración centralizada (Spring Cloud Config)
├── eureka-server/          # Service Discovery (Eureka)
├── gateway-service/        # API Gateway (Spring Cloud Gateway)
├── invoice-service/        # Gestión de facturas y PDFs ⭐
├── user-service/           # Gestión de usuarios y clientes ⭐
├── document-service/       # Gestión de documentos ⭐
└── trace-service/          # Trazabilidad y auditoría ⭐
```

### Estructura Clean Architecture por Servicio
Cada servicio sigue **EXACTAMENTE** esta estructura:

```
invoice-service/
├── src/
│   ├── main/
│   │   ├── java/com/invoices/invoice_service/
│   │   │   ├── domain/                    # ← CAPA DOMINIO (Sin dependencias externas)
│   │   │   │   ├── entities/              # Entidades de dominio puras
│   │   │   │   │   ├── Invoice.java       # Lógica de negocio central
│   │   │   │   │   └── InvoiceItem.java   # Validaciones de dominio
│   │   │   │   ├── usecases/              # Casos de uso (reglas de negocio)
│   │   │   │   │   ├── GetInvoiceByIdUseCase.java
│   │   │   │   │   └── GeneratePdfUseCase.java
│   │   │   │   └── ports/                 # Interfaces (Dependency Inversion)
│   │   │   │       ├── InvoiceRepository.java      # Port de salida
│   │   │   │       └── PdfGeneratorService.java    # Port de salida
│   │   │   │
│   │   │   ├── application/               # ← CAPA APLICACIÓN (Orquestación)
│   │   │   │   └── services/              # Servicios de aplicación
│   │   │   │       └── InvoiceApplicationService.java
│   │   │   │
│   │   │   ├── infrastructure/            # ← CAPA INFRAESTRUCTURA (Adaptadores)
│   │   │   │   ├── persistence/           # Adaptador JPA
│   │   │   │   │   ├── entities/          # JPA Entities (modelo de persistencia)
│   │   │   │   │   │   ├── InvoiceJpaEntity.java
│   │   │   │   │   │   └── InvoiceItemJpaEntity.java
│   │   │   │   │   ├── repositories/      # Implementaciones concretas
│   │   │   │   │   │   └── InvoiceRepositoryImpl.java
│   │   │   │   │   └── mappers/           # Mappers Domain ↔ JPA
│   │   │   │   │       └── InvoiceJpaMapper.java
│   │   │   │   ├── external/              # Adaptadores externos
│   │   │   │   │   └── jasper/            # JasperReports adapter
│   │   │   │   │       └── JasperPdfGenerator.java
│   │   │   │   └── messaging/             # Kafka producers/consumers
│   │   │   │       └── InvoiceEventProducer.java
│   │   │   │
│   │   │   └── presentation/              # ← CAPA PRESENTACIÓN (Entrada)
│   │   │       ├── controllers/           # REST Controllers
│   │   │       │   └── InvoiceController.java
│   │   │       ├── dto/                   # DTOs (auto-generados por OpenAPI)
│   │   │       │   └── InvoiceDTO.java
│   │   │       ├── mappers/               # Mappers Domain ↔ DTO
│   │   │       │   └── InvoiceDtoMapper.java
│   │   │       └── exceptionhandlers/     # Manejo global de excepciones
│   │   │           └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── openapi/
│   │       │   └── invoice-api.yaml       # OpenAPI 3.0 Spec
│   │       ├── application.yml            # Configuración Spring Boot
│   │       └── jasper-templates/          # Templates JasperReports
│   │
│   └── test/
│       └── java/com/invoices/invoice_service/
│           ├── domain/
│           │   ├── entities/              # Tests de entidades (lógica de dominio)
│           │   │   ├── InvoiceTest.java
│           │   │   └── InvoiceItemTest.java
│           │   └── usecases/              # Tests de casos de uso (90%+ cobertura)
│           │       ├── GetInvoiceByIdUseCaseTest.java
│           │       └── GeneratePdfUseCaseTest.java
│           ├── application/
│           │   └── services/              # Tests de servicios de aplicación
│           ├── infrastructure/
│           │   ├── persistence/           # Tests de repositorios
│           │   └── external/              # Tests de adaptadores externos
│           └── presentation/
│               └── controllers/           # Tests de controladores (@WebMvcTest)
│
├── pom.xml                                # Dependencias Maven + Plugins
└── README.md                              # Documentación del servicio
```

---

## 🛠️ Stack Tecnológico

### Core
- **Java 21** (LTS)
- **Spring Boot 3.4.4**
- **Spring Cloud 2024.0.1** (Config, Eureka, Gateway)
- **PostgreSQL** (Base de datos)
- **Apache Kafka** (Mensajería asíncrona)

### Generación de PDFs
- **JasperReports 7.0.2** (invoice-service)

### OpenAPI
- **OpenAPI Generator Maven Plugin 7.0.1** (Generación automática de APIs)
- **Springdoc OpenAPI 2.6.0** (Documentación Swagger UI)

### Testing & Quality
- **JUnit 5** (Tests unitarios)
- **Mockito** (Mocking)
- **JaCoCo** (Cobertura de código - mínimo 90%)
- **Checkstyle** (Estilo de código)
- **SpotBugs** (Análisis estático)
- **ArchUnit** (Tests de arquitectura)

### Utilities
- **Lombok** (Reducir boilerplate)
- **MapStruct** (Mappers automáticos)

---

## 🚀 Configuración y Ejecución

### Prerrequisitos
```bash
- Java 21+
- Maven 3.8+
- Docker (PostgreSQL, Kafka)
- Git
```

### 1. Clonar Repositorio
```bash
git clone <repository-url>
cd invoices-back
```

### 2. Iniciar Infraestructura (Docker Compose)
```bash
docker-compose up -d  # PostgreSQL, Kafka, Zookeeper
```

### 3. Compilar Proyecto
```bash
mvn clean install -DskipTests
```

### 4. Ejecutar Servicios (en orden)
```bash
# 1. Config Server
cd config-server && mvn spring-boot:run &

# 2. Eureka Server
cd eureka-server && mvn spring-boot:run &

# 3. Servicios de negocio
cd invoice-service && mvn spring-boot:run &
cd user-service && mvn spring-boot:run &
cd document-service && mvn spring-boot:run &
cd trace-service && mvn spring-boot:run &

# 4. Gateway
cd gateway-service && mvn spring-boot:run &
```

### 5. Verificar Servicios
- **Eureka Dashboard:** http://localhost:8761
- **Swagger UI (Invoice):** http://localhost:8081/swagger-ui.html
- **Swagger UI (User):** http://localhost:8082/swagger-ui.html

---

## ✅ Estándares de Desarrollo

### 1. Flujo de Trabajo (Git Flow)
```bash
# Crear feature branch
git checkout -b feature/add-invoice-validation

# Commits descriptivos
git commit -m "feat: add business validation for invoice amounts"

# Push y Pull Request
git push origin feature/add-invoice-validation
```

### 2. Mensajes de Commit (Conventional Commits)
```
feat: nueva funcionalidad
fix: corrección de bugs
refactor: refactorización sin cambios funcionales
test: agregar/modificar tests
docs: documentación
chore: tareas de mantenimiento
```

### 3. Code Review - Checklist Obligatorio
- [ ] ¿Sigue Clean Architecture? (Dependencias hacia adentro)
- [ ] ¿Nombres descriptivos y sin abreviaturas?
- [ ] ¿Funciones < 20 líneas?
- [ ] ¿Tests unitarios con cobertura 90%+?
- [ ] ¿Checkstyle pasa sin warnings?
- [ ] ¿JaCoCo reporta cobertura suficiente?
- [ ] ¿Excepciones específicas de dominio?
- [ ] ¿Sin duplicación de código (DRY)?

### 4. Herramientas de Calidad

#### Checkstyle (Estilo de Código)
```bash
mvn checkstyle:check
```
**Reglas:** Google Java Style Guide adaptado (checkstyle.xml en cada servicio).

#### SpotBugs (Análisis Estático)
```bash
mvn spotbugs:check
```

#### JaCoCo (Cobertura)
```bash
mvn clean test jacoco:report
# Ver reporte: target/site/jacoco/index.html
```
**Mínimo:** 90% líneas, 85% ramas.

#### ArchUnit (Tests de Arquitectura)
Valida que la arquitectura se respete:
```java
@Test
void domainLayerShouldNotDependOnInfrastructure() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .check(importedClasses);
}
```

---

## 📋 Ejemplos de Código - ESTÁNDARES OBLIGATORIOS

### Domain Entity (Lógica de Negocio Pura)
```java
package com.invoices.invoice_service.domain.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Invoice domain entity with business logic and validations.
 * NO dependencies on frameworks or infrastructure.
 */
public class Invoice {
    private final Long id;
    private final String invoiceNumber;
    private final LocalDateTime issueDate;
    private final List<InvoiceItem> items;
    private InvoiceStatus status;

    public Invoice(Long id, String invoiceNumber, LocalDateTime issueDate) {
        validateInvoiceNumber(invoiceNumber);
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.items = new ArrayList<>();
        this.status = InvoiceStatus.DRAFT;
    }

    public void addItem(InvoiceItem item) {
        if (status == InvoiceStatus.FINALIZED) {
            throw new IllegalStateException("Cannot modify finalized invoice");
        }
        items.add(item);
    }

    public BigDecimal calculateTotalAmount() {
        return items.stream()
            .map(InvoiceItem::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void finalize() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot finalize invoice without items");
        }
        this.status = InvoiceStatus.FINALIZED;
    }

    private void validateInvoiceNumber(String number) {
        if (number == null || !number.matches("\\d{4}-\\d{3}")) {
            throw new IllegalArgumentException("Invalid invoice number format. Expected: YYYY-XXX");
        }
    }

    // Getters (no setters - immutability)
    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public LocalDateTime getIssueDate() { return issueDate; }
    public List<InvoiceItem> getItems() { return Collections.unmodifiableList(items); }
    public InvoiceStatus getStatus() { return status; }
}
```

### Use Case (Caso de Uso)
```java
package com.invoices.invoice_service.domain.usecases;

import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice_service.domain.ports.InvoiceRepository;

/**
 * Use case: Get invoice by ID.
 * Pure business logic, no infrastructure concerns.
 */
public class GetInvoiceByIdUseCase {
    private final InvoiceRepository repository;

    public GetInvoiceByIdUseCase(InvoiceRepository repository) {
        this.repository = repository;
    }

    public Invoice execute(Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new IllegalArgumentException("Invoice ID must be positive");
        }

        return repository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }
}
```

### Unit Test (JUnit 5 + Mockito)
```java
package com.invoices.invoice_service.domain.usecases;

import com.invoices.invoice_service.domain.entities.Invoice;
import com.invoices.invoice_service.domain.exceptions.InvoiceNotFoundException;
import com.invoices.invoice_service.domain.ports.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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
        // Arrange
        Long invoiceId = 1L;
        Invoice expectedInvoice = new Invoice(invoiceId, "2025-001", LocalDateTime.now());
        when(repository.findById(invoiceId)).thenReturn(Optional.of(expectedInvoice));

        // Act
        Invoice result = useCase.execute(invoiceId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoiceId);
        verify(repository, times(1)).findById(invoiceId);
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(nonExistentId))
            .isInstanceOf(InvoiceNotFoundException.class)
            .hasMessageContaining("999");

        verify(repository, times(1)).findById(nonExistentId);
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");

        verify(repository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNegative() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(-1L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

---

## 🎯 Objetivos de Calidad

| Métrica | Objetivo | Herramienta |
|---------|----------|-------------|
| **Cobertura de Líneas** | ≥ 90% | JaCoCo |
| **Cobertura de Ramas** | ≥ 85% | JaCoCo |
| **Complejidad Ciclomática** | ≤ 10 por método | Checkstyle |
| **Duplicación de Código** | < 3% | SpotBugs |
| **Deuda Técnica** | Calificación A | SonarQube (opcional) |
| **Tiempo de Build** | < 5 min | Maven |

---

## 🚨 Advertencias Finales

### ¡NO SE TOLERA!
- ❌ **God Classes** (clases con > 500 líneas)
- ❌ **Métodos con > 20 líneas**
- ❌ **Lógica de negocio en Controllers**
- ❌ **Entidades JPA como entidades de dominio**
- ❌ **Try-catch genéricos sin manejo específico**
- ❌ **Comentarios obvios** (`// Get user by ID` sobre `getUserById()`)
- ❌ **Magic numbers** (usar constantes)
- ❌ **Dependencias circulares entre capas**
- ❌ **Tests sin assertions**
- ❌ **Código comentado (usar Git, no comentarios)**

### ✅ OBLIGATORIO
- ✅ **Dependency Injection** vía constructores (Spring `@Autowired` solo en constructores)
- ✅ **Validaciones en Domain Entities**
- ✅ **Excepciones específicas de dominio** (`InvoiceNotFoundException`, `InvalidInvoiceStateException`)
- ✅ **Inmutabilidad** donde sea posible (preferir `final`, records en DTOs)
- ✅ **Segregación de Interfaces** (ISP - no interfaces gigantes)
- ✅ **Tests independientes** (no depender de orden de ejecución)

---

## 📚 Referencias

- [Clean Architecture - Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Clean Code - Uncle Bob](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

---

## 📄 Licencia
Este proyecto es un ejemplo de arquitectura limpia para sistemas empresariales. Adaptado para proyectos profesionales.

---

**RECUERDA:** Código de calidad no es opcional. Es una **OBLIGACIÓN PROFESIONAL**. No generes basura, genera software que resista el paso del tiempo.

> "The only way to go fast, is to go well." - Robert C. Martin (Uncle Bob)
