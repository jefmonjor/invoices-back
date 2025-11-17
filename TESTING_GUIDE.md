# 🧪 Guía de Testing y Cobertura

Esta guía explica cómo ejecutar los tests y verificar la cobertura de código en todos los microservicios.

## 📊 Resumen de Cobertura Implementada

| Servicio | JaCoCo Coverage | Tests | Tipo |
|----------|-----------------|-------|------|
| **Document Service** | 80% | 17 | Unit + Integration (Testcontainers) |
| **Trace Service** | 70% | 26 | Unit + Integration (EmbeddedKafka + DLQ) |
| **Gateway Service** | 90%+ | 51 | Unit (JWT, Routing, CORS) |
| **Config Server** | N/A | - | Configuración |

---

## 🚀 Ejecutar Tests

### Prerrequisitos

- **Docker Desktop** corriendo (para Testcontainers)
- **Maven 3.8+**
- **Java 21**

### Ejecutar Tests por Servicio

```bash
# Document Service (requiere Docker para MinIO + PostgreSQL)
cd document-service
mvn clean test

# Trace Service (incluye EmbeddedKafka + PostgreSQL)
cd trace-service
mvn clean test

# Gateway Service (tests unitarios)
cd gateway-service
mvn clean test

# Ejecutar TODOS los tests desde la raíz
mvn clean test
```

### Ver Reportes JaCoCo

Después de ejecutar los tests, los reportes HTML están disponibles en:

```bash
# Document Service
open document-service/target/site/jacoco/index.html

# Trace Service
open trace-service/target/site/jacoco/index.html

# Gateway Service
open gateway-service/target/site/jacoco/index.html

# En Windows
start document-service/target/site/jacoco/index.html
start trace-service/target/site/jacoco/index.html
start gateway-service/target/site/jacoco/index.html

# En Linux
xdg-open document-service/target/site/jacoco/index.html
xdg-open trace-service/target/site/jacoco/index.html
xdg-open gateway-service/target/site/jacoco/index.html
```

---

## 🔍 Verificar Cobertura Mínima

JaCoCo está configurado para **fallar el build** si la cobertura es menor a:

- **Document Service**: 80% line coverage
- **Trace Service**: 70% line coverage
- **Gateway Service**: 70% line coverage

```bash
# Verificar y generar reporte
mvn clean verify

# Solo verificar cobertura (sin compilar de nuevo)
mvn jacoco:check
```

---

## 📝 Detalles de Tests Implementados

### Document Service

**Integration Tests (MinIO):**
- ✅ Upload document to MinIO
- ✅ Download document from MinIO
- ✅ Delete document from MinIO
- ✅ Reject non-PDF files
- ✅ Reject files exceeding max size
- ✅ Handle multiple uploads for same invoice
- ✅ Generate unique filenames

**File Validation Tests:**
- ✅ Detect text files masquerading as PDF
- ✅ Detect HTML files masquerading as PDF
- ✅ Detect ZIP files masquerading as PDF
- ✅ Detect JPEG files masquerading as PDF
- ✅ Reject truncated PDFs
- ✅ Validate PDF signature (%PDF-)

**Ubicación:** `document-service/src/test/java/com/invoices/document_service/`

### Trace Service

**Kafka Consumer Tests:**
- ✅ Consume INVOICE_CREATED events
- ✅ Consume INVOICE_UPDATED events
- ✅ Consume INVOICE_DELETED events
- ✅ Handle multiple events for same invoice
- ✅ Handle events from different clients
- ✅ Store complete event data as JSON
- ✅ Handle high volume (50 events)
- ✅ Handle events with null fields
- ✅ Query events by type

**Service Tests:**
- ✅ Get logs by invoice ID
- ✅ Get logs by client ID
- ✅ Get logs by event type
- ✅ Get all logs with pagination
- ✅ Get log by ID
- ✅ Handle not found scenarios

**Controller Tests:**
- ✅ Filter by invoice ID
- ✅ Filter by client ID
- ✅ Filter by event type
- ✅ Pagination support
- ✅ Custom sorting
- ✅ 404 error handling

**Dead Letter Queue (DLQ):**
- ✅ Retry logic con exponential backoff (3 intentos: 1s, 2s, 4s)
- ✅ Envío a DLQ topic después de fallos
- ✅ Monitoreo y logging de mensajes en DLQ
- ✅ Preservación de mensajes originales para análisis

**Ubicación:** `trace-service/src/test/java/com/invoices/trace_service/`

### Gateway Service

**JWT Authentication Filter Tests (17 tests):**
- ✅ Authenticate with valid token
- ✅ Allow public routes without token (/auth/**, /actuator/**)
- ✅ Reject requests without Authorization header
- ✅ Reject malformed Authorization header
- ✅ Reject expired tokens (401)
- ✅ Reject invalid tokens (401)
- ✅ Extract username and set SecurityContext
- ✅ Continue filter chain for valid tokens
- ✅ Handle tokens with "Bearer " prefix
- ✅ Validate token expiration
- ✅ Multiple concurrent requests
- ✅ Token refresh scenarios
- ✅ Handle edge cases (null, empty, spaces)
- ✅ Verify SecurityContextHolder cleanup
- ✅ Test filter ordering
- ✅ Validate authentication object
- ✅ Error response format

**Gateway Routing Tests (15 tests):**
- ✅ Verify all 5 microservice routes configured
- ✅ Invoice Service route (lb://invoice-service)
- ✅ Client Service route (lb://client-service)
- ✅ Document Service route (lb://document-service)
- ✅ Trace Service route (lb://trace-service)
- ✅ User Service route (lb://user-service)
- ✅ Path predicates correct (/api/invoices/**, etc.)
- ✅ Load balancing enabled (lb://)
- ✅ Route IDs unique
- ✅ No duplicate paths
- ✅ Route priority order
- ✅ StripPrefix filter configured
- ✅ RewritePath filter working
- ✅ All routes reachable
- ✅ Route metadata validation

**CORS Configuration Tests (19 tests):**
- ✅ Allow CORS preflight requests (OPTIONS)
- ✅ Allow localhost:3000 origin
- ✅ Allow localhost:5173 origin (Vite dev)
- ✅ Allow all HTTP methods (GET, POST, PUT, DELETE, PATCH)
- ✅ Allow Authorization header
- ✅ Allow Content-Type header
- ✅ Allow custom headers
- ✅ Credentials enabled (Access-Control-Allow-Credentials: true)
- ✅ Max age 3600 seconds
- ✅ Reject unauthorized origins
- ✅ CORS headers in actual requests
- ✅ Multiple origins support
- ✅ Wildcard path matching (/**)
- ✅ CORS on all endpoints
- ✅ Preflight cache duration
- ✅ Exposed headers configuration
- ✅ CORS error handling
- ✅ OPTIONS request returns 200
- ✅ CORS integration with security filters

**Ubicación:** `gateway-service/src/test/java/com/invoices/gateway_service/`
- `security/JwtAuthenticationFilterTest.java`
- `routing/GatewayRoutingTest.java`
- `cors/CorsConfigurationTest.java`

---

## 🐛 Troubleshooting

### Docker no está corriendo

```
ERROR: Could not start container
```

**Solución:**
- Iniciar Docker Desktop
- Verificar: `docker ps`

### Puerto Kafka ocupado

```
ERROR: Address already in use: bind
```

**Solución:**
```bash
# Encontrar proceso usando el puerto 9093
lsof -i :9093
# Matar el proceso
kill -9 <PID>
```

### Tests de MinIO fallan

```
ERROR: Connection refused
```

**Solución:**
- Verificar que Docker tiene suficiente memoria (mínimo 4GB)
- Limpiar contenedores: `docker system prune -a`

### Cobertura insuficiente

```
ERROR: Rule violated for package: Line coverage ratio is 0.65
```

**Solución:**
- Revisar qué clases no tienen cobertura: abrir `target/site/jacoco/index.html`
- Agregar tests para las clases con baja cobertura

---

## 🎯 Comandos Útiles

```bash
# Ejecutar tests en paralelo (más rápido)
mvn -T 1C clean test

# Ejecutar solo tests de integración
mvn test -Dtest="*IntegrationTest"

# Ejecutar solo tests unitarios
mvn test -Dtest="*Test"

# Saltar tests (NO recomendado)
mvn clean install -DskipTests

# Ver solo errores
mvn test --fail-at-end

# Generar reporte sin ejecutar tests
mvn jacoco:report
```

---

## 📈 Interpretar Reportes JaCoCo

El reporte HTML muestra:

- **Verde**: Línea cubierta por tests
- **Amarillo**: Línea parcialmente cubierta
- **Rojo**: Línea NO cubierta

### Métricas

- **Instructions**: Bytecode instructions cubiertos
- **Branches**: Condicionales (if/switch) cubiertos
- **Lines**: Líneas de código cubiertos
- **Methods**: Métodos cubiertos
- **Classes**: Clases cubiertos

---

## ✅ Checklist de Calidad

Antes de hacer commit:

- [ ] Todos los tests pasan: `mvn clean test`
- [ ] Cobertura cumple mínimo: `mvn jacoco:check`
- [ ] No hay warnings: `mvn clean compile`
- [ ] Tests de integración funcionan: Docker corriendo

---

## 🔗 Referencias

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Testcontainers](https://www.testcontainers.org/)
- [Spring Kafka Test](https://docs.spring.io/spring-kafka/docs/current/reference/html/#testing)
- [AssertJ Documentation](https://assertj.github.io/doc/)
