# Invoice Service

Microservicio para gestionar facturas, items y generar PDFs con JasperReports.

## Tecnologías

- **Spring Boot 3.4.4** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **PostgreSQL** - Base de datos
- **Flyway** - Migraciones de base de datos
- **Spring Cloud** - Microservicios (Eureka, Config Server, OpenFeign)
- **Apache Kafka** - Mensajería asíncrona
- **JasperReports 7.0.2** - Generación de PDFs
- **OpenAPI/Swagger** - Documentación de API
- **Lombok** - Reducción de boilerplate
- **Bean Validation** - Validación de datos

## Arquitectura

### Estructura del Proyecto

```
invoice-service/
├── src/main/java/com/invoices/invoice_service/
│   ├── client/          # Feign Clients para comunicación con otros servicios
│   ├── config/          # Configuraciones (Feign, Kafka, OpenAPI)
│   ├── controller/      # Controladores REST
│   ├── dto/             # Data Transfer Objects
│   ├── entity/          # Entidades JPA
│   ├── exception/       # Excepciones personalizadas y handlers
│   ├── kafka/           # Productores y eventos de Kafka
│   ├── repository/      # Repositorios JPA
│   └── service/         # Lógica de negocio
└── src/main/resources/
    ├── db/migration/    # Scripts de migración Flyway
    └── application.yml  # Configuración de la aplicación
```

## Características Principales

### 1. Gestión de Facturas

- **Crear facturas** con múltiples items
- **Consultar facturas** por ID o por cliente
- **Actualizar facturas** (fecha de vencimiento, estado, notas)
- **Eliminar facturas**
- **Marcar facturas como pagadas**

### 2. Cálculos Automáticos

- **Subtotal**: Suma de todos los items
- **IVA (19%)**: Calculado sobre el subtotal
- **Total**: Subtotal + IVA
- **Número de factura**: Generado automáticamente en formato `INV-YYYY-NNNN`

### 3. Generación de PDFs

- Genera PDFs de facturas usando **JasperReports**
- Diseño programático (no requiere archivos JRXML)
- Sube automáticamente el PDF al **document-service** vía Feign
- Retorna URL de descarga del documento

### 4. Eventos Kafka

Publica eventos en el topic `invoice-events` para las siguientes operaciones:

- `CREATED`: Factura creada
- `UPDATED`: Factura actualizada
- `PAID`: Factura marcada como pagada
- `CANCELLED`: Factura cancelada

### 5. Integración con Otros Servicios

- **user-service**: Valida que el cliente existe antes de crear factura
- **document-service**: Almacena PDFs generados

## API Endpoints

### Facturas

| Método | Endpoint | Descripción | Roles |
|--------|----------|-------------|-------|
| POST | `/api/invoices` | Crear nueva factura | USER, ADMIN |
| GET | `/api/invoices` | Listar todas las facturas | ADMIN |
| GET | `/api/invoices?clientId={id}` | Listar facturas de un cliente | USER, ADMIN |
| GET | `/api/invoices/{id}` | Obtener factura por ID | USER, ADMIN |
| PUT | `/api/invoices/{id}` | Actualizar factura | USER, ADMIN |
| DELETE | `/api/invoices/{id}` | Eliminar factura | ADMIN |
| POST | `/api/invoices/{id}/pay` | Marcar como pagada | USER, ADMIN |
| POST | `/api/invoices/generate-pdf` | Generar PDF | USER, ADMIN |

## Modelo de Datos

### Invoice (Factura)

```java
{
  "id": 1,
  "invoiceNumber": "INV-2025-0001",
  "clientId": 1,
  "clientEmail": "admin@invoices.com",
  "invoiceDate": "2025-11-13",
  "dueDate": "2025-12-13",
  "subtotal": 1000.00,
  "tax": 190.00,
  "total": 1190.00,
  "status": "PENDING",
  "notes": "Consultoría de software",
  "items": [...],
  "createdAt": "2025-11-13T10:00:00"
}
```

### InvoiceItem (Item de Factura)

```java
{
  "id": 1,
  "description": "Consultoría de Software",
  "quantity": 10,
  "unitPrice": 100.00,
  "total": 1000.00
}
```

### Estados de Factura

- `PENDING`: Pendiente de pago
- `PAID`: Pagada
- `CANCELLED`: Cancelada
- `OVERDUE`: Vencida (no implementado aún)

## Configuración

### Variables de Entorno

```bash
# Database
INVOICE_DB_HOST=localhost
INVOICE_DB_PORT=5432
INVOICE_DB_NAME=invoicedb
INVOICE_DB_USERNAME=invoice_service_user
INVOICE_DB_PASSWORD=password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_INVOICE_TOPIC=invoice-events

# Eureka
EUREKA_SERVER_HOST=localhost
EUREKA_SERVER_PORT=8761
EUREKA_USERNAME=eureka-admin
EUREKA_PASSWORD=password

# JWT
JWT_SECRET=your-secret-key-min-32-chars
JWT_ISSUER=invoices-backend

# Logging
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
```

## Base de Datos

### Tablas

1. **invoices**: Almacena las facturas
   - Campos: id, invoice_number, client_id, client_email, invoice_date, due_date, subtotal, tax, total, status, notes, created_at, updated_at
   - Índices: invoice_number, client_id, invoice_date

2. **invoice_items**: Almacena los items de cada factura
   - Campos: id, invoice_id, description, quantity, unit_price, total
   - Relación: FK a invoices con CASCADE DELETE

## Validaciones

### CreateInvoiceRequest

- `clientId`: Obligatorio
- `clientEmail`: Obligatorio, formato email válido
- `invoiceDate`: Obligatorio
- `items`: Lista no vacía con al menos un item

### CreateInvoiceItemRequest

- `description`: Obligatorio
- `quantity`: Obligatorio, mínimo 1
- `unitPrice`: Obligatorio, mínimo 0.01

## Manejo de Errores

El servicio maneja los siguientes errores:

- **404 Not Found**: Factura o cliente no encontrado
- **400 Bad Request**: Errores de validación
- **500 Internal Server Error**: Errores de generación de PDF o comunicación con servicios externos
- **503 Service Unavailable**: Servicios externos no disponibles

## Swagger/OpenAPI

Documentación interactiva disponible en:
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/api-docs`

## Ejemplo de Uso

### Crear Factura

```bash
curl -X POST http://localhost:8081/api/invoices \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "clientEmail": "cliente@example.com",
    "invoiceDate": "2025-11-13",
    "dueDate": "2025-12-13",
    "items": [
      {
        "description": "Consultoría",
        "quantity": 10,
        "unitPrice": 100.00
      }
    ],
    "notes": "Factura de consultoría"
  }'
```

### Generar PDF

```bash
curl -X POST http://localhost:8081/api/invoices/generate-pdf \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceId": 1
  }'
```

## Desarrollo

### Ejecutar localmente

```bash
# Compilar
mvn clean install

# Ejecutar
mvn spring-boot:run

# O con variables de entorno
INVOICE_DB_HOST=localhost \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
mvn spring-boot:run
```

### Tests

```bash
mvn test
```

## Docker

```bash
# Build
docker build -t invoice-service .

# Run
docker run -p 8081:8081 \
  -e INVOICE_DB_HOST=postgres \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  invoice-service
```

## Observabilidad

### Actuator Endpoints

- Health: `http://localhost:8081/actuator/health`
- Info: `http://localhost:8081/actuator/info`
- Metrics: `http://localhost:8081/actuator/metrics`

## Próximas Mejoras

- [ ] Implementar autenticación/autorización con JWT
- [ ] Agregar paginación a los endpoints de listado
- [ ] Implementar búsqueda avanzada de facturas
- [ ] Agregar soporte para múltiples monedas
- [ ] Implementar recordatorios de facturas vencidas
- [ ] Agregar reportes y estadísticas
- [ ] Mejorar diseño de PDFs con templates personalizables
- [ ] Agregar tests unitarios e integración

## Autor

Invoice Service Team - invoices@example.com

## Licencia

MIT License
