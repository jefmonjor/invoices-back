# INVOICES BACKEND - Microservices Architecture

Sistema de gestión de facturas construido con arquitectura de microservicios usando Spring Boot 3.4.4, Spring Cloud, Kafka, PostgreSQL y MinIO.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.1-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Kafka](https://img.shields.io/badge/Kafka-7.5.0-black)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)

---

## Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Requisitos Previos](#requisitos-previos)
- [Instalación y Configuración](#instalación-y-configuración)
- [Ejecución](#ejecución)
- [Endpoints Principales](#endpoints-principales)
- [Seguridad y Autenticación](#seguridad-y-autenticación)
- [Testing](#testing)
- [Documentación API](#documentación-api)
- [Arquitectura de Base de Datos](#arquitectura-de-base-de-datos)
- [Patrones y Mejores Prácticas](#patrones-y-mejores-prácticas)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [Contribución](#contribución)
- [Licencia](#licencia)

---

## Arquitectura

### Diagrama de Arquitectura

```
                                    ┌─────────────────┐
                                    │   Frontend      │
                                    │  (React/Vue)    │
                                    └────────┬────────┘
                                             │
                                    ┌────────▼────────┐
                                    │  API Gateway    │
                                    │  (Port: 8080)   │
                                    │   JWT Filter    │
                                    └────────┬────────┘
                                             │
                        ┌────────────────────┼────────────────────┐
                        │                    │                    │
              ┌─────────▼────────┐  ┌───────▼────────┐  ┌───────▼────────┐
              │  User Service    │  │ Invoice Service │  │ Document Service│
              │  (Port: 8082)    │  │  (Port: 8081)   │  │  (Port: 8083)   │
              │  Auth + CRUD     │  │  CRUD + PDF Gen │  │  MinIO Storage  │
              └─────────┬────────┘  └───────┬─────────┘  └────────────────┘
                        │                   │
                        │                   │ Kafka Events
                        │                   │
                        │          ┌────────▼────────┐
                        │          │  Trace Service  │
                        │          │  (Port: 8084)   │
                        │          │  Audit Logs     │
                        │          └─────────────────┘
                        │
              ┌─────────▼────────┐
              │  Eureka Server   │
              │  (Port: 8761)    │
              │ Service Discovery│
              └──────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                          │
├──────────────────┬──────────────────┬──────────────────┬─────────┤
│   PostgreSQL     │      Kafka       │      MinIO       │  Eureka │
│   (4 databases)  │   (Port: 9092)   │  (Port: 9000)    │  :8761  │
│   - userdb       │   Event-Driven   │   S3-Compatible  │ Service │
│   - invoicedb    │   Messaging      │   PDF Storage    │Discovery│
│   - documentdb   │                  │                  │         │
│   - tracedb      │                  │                  │         │
└──────────────────┴──────────────────┴──────────────────┴─────────┘
```

### Microservicios

| Servicio | Puerto | Descripción | Base de Datos |
|----------|--------|-------------|---------------|
| **Gateway Service** | 8080 | API Gateway con JWT validation, CORS, enrutamiento | - |
| **Eureka Server** | 8761 | Service Discovery y registro de servicios | - |
| **Config Server** | 8888 | Configuración centralizada | - |
| **User Service** | 8082 | Autenticación (JWT), gestión de usuarios y clientes | `userdb` |
| **Invoice Service** | 8081 | CRUD de facturas, generación de PDFs, Kafka producer | `invoicedb` |
| **Document Service** | 8083 | Almacenamiento de PDFs en MinIO (S3-compatible) | `documentdb` |
| **Trace Service** | 8084 | Auditoría de eventos (Kafka consumer) | `tracedb` |

---

## Tecnologías

### Backend
- **Java 21** (LTS) - Virtual Threads, Records, Pattern Matching
- **Spring Boot 3.4.4** - Framework principal
- **Spring Cloud 2024.0.1** - Microservices patterns
  - Spring Cloud Gateway - API Gateway
  - Netflix Eureka - Service Discovery
  - OpenFeign - Comunicación síncrona entre servicios
- **Spring Security + JWT** - Autenticación stateless
- **Spring Data JPA** - ORM y persistencia

### Mensajería y Almacenamiento
- **Apache Kafka 7.5.0** - Event-driven architecture
- **PostgreSQL 16** - Base de datos relacional (4 BDs separadas)
- **MinIO** - Almacenamiento de objetos S3-compatible
- **Flyway** - Migraciones de base de datos versionadas

### Generación de Documentos
- **JasperReports 7.0.2** - Generación de PDFs

### Herramientas
- **Lombok** - Reducción de boilerplate
- **Springdoc OpenAPI 2.6.0** - Documentación Swagger
- **Docker & Docker Compose** - Contenedorización

---

## Requisitos Previos

### Software Necesario
- **Java 21** o superior ([Descargar](https://adoptium.net/))
- **Maven 3.9+** ([Descargar](https://maven.apache.org/download.cgi))
- **Docker** y **Docker Compose** ([Descargar](https://www.docker.com/))
- **Git** ([Descargar](https://git-scm.com/))

### Opcional (desarrollo local sin Docker)
- **PostgreSQL 16** ([Descargar](https://www.postgresql.org/download/))
- **Apache Kafka** ([Descargar](https://kafka.apache.org/downloads))
- **MinIO** ([Descargar](https://min.io/download))

---

## Instalación y Configuración

### 1. Clonar el Repositorio

```bash
git clone https://github.com/jefmonjor/invoices-back.git
cd invoices-back
```

### 2. Configurar Variables de Entorno

Copiar el archivo de ejemplo y personalizarlo:

```bash
cp .env.example .env
```

Editar `.env` y cambiar los valores **CHANGE_ME**:

```bash
# Ejemplo de valores para desarrollo local
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
USER_DB_PASSWORD=secure_password_123
INVOICE_DB_PASSWORD=secure_password_456
# ... (ver .env.example para todas las variables)
```

> **IMPORTANTE**: Nunca versionar el archivo `.env` en Git (ya está en `.gitignore`)

### 3. Generar Clave JWT Segura

```bash
# Linux/macOS
openssl rand -base64 64 | tr -d '\n'

# Windows (PowerShell)
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

Copiar el resultado en `JWT_SECRET` del archivo `.env`.

---

## Ejecución

### Opción 1: Docker Compose (Recomendado)

Levanta toda la infraestructura y servicios con un solo comando:

```bash
# Levantar todo el stack
docker-compose up -d

# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f user-service

# Detener todos los servicios
docker-compose down

# Detener y eliminar volúmenes (CUIDADO: borra datos)
docker-compose down -v
```

**Tiempo de inicio**: ~2-3 minutos (primera vez puede tardar más por descargar imágenes)

### Opción 2: Ejecución Local (Desarrollo)

#### Paso 1: Levantar Infraestructura

```bash
# Solo infraestructura (PostgreSQL, Kafka, MinIO, Eureka)
docker-compose up -d postgres kafka zookeeper minio eureka-server
```

#### Paso 2: Compilar Servicios

```bash
# Compilar todos los módulos
mvn clean install -DskipTests

# O compilar individualmente
cd user-service && mvn clean package -DskipTests
cd invoice-service && mvn clean package -DskipTests
# ...
```

#### Paso 3: Ejecutar Servicios

```bash
# Terminal 1: Eureka Server
cd eureka-server
mvn spring-boot:run

# Terminal 2: User Service
cd user-service
mvn spring-boot:run

# Terminal 3: Invoice Service
cd invoice-service
mvn spring-boot:run

# Terminal 4: Document Service
cd document-service
mvn spring-boot:run

# Terminal 5: Trace Service
cd trace-service
mvn spring-boot:run

# Terminal 6: Gateway Service (último)
cd gateway-service
mvn spring-boot:run
```

**Orden de inicio recomendado**:
1. Eureka Server (8761)
2. User Service (8082)
3. Invoice Service (8081)
4. Document Service (8083)
5. Trace Service (8084)
6. Gateway Service (8080)

---

## Endpoints Principales

### Health Checks

```bash
# Gateway
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8082/actuator/health

# Eureka Dashboard
open http://localhost:8761
```

### Autenticación (User Service)

#### Registro de Usuario

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Respuesta**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["ROLE_USER"],
    "enabled": true
  }
}
```

#### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@invoices.com",
    "password": "admin123"
  }'
```

> **Usuario admin por defecto**: `admin@invoices.com` / `admin123`

### Gestión de Facturas (Invoice Service)

#### Crear Factura

```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST http://localhost:8080/api/invoices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "clientEmail": "client@example.com",
    "invoiceDate": "2025-11-13",
    "dueDate": "2025-12-13",
    "notes": "Factura de servicios de consultoría",
    "items": [
      {
        "description": "Consultoría de Software",
        "quantity": 10,
        "unitPrice": 100.00
      },
      {
        "description": "Desarrollo Backend",
        "quantity": 5,
        "unitPrice": 150.00
      }
    ]
  }'
```

**Respuesta**:
```json
{
  "id": 1,
  "invoiceNumber": "INV-2025-0001",
  "clientId": 1,
  "clientEmail": "client@example.com",
  "invoiceDate": "2025-11-13",
  "dueDate": "2025-12-13",
  "subtotal": 1750.00,
  "tax": 332.50,
  "total": 2082.50,
  "status": "PENDING",
  "notes": "Factura de servicios de consultoría",
  "items": [...],
  "createdAt": "2025-11-13T10:30:00Z"
}
```

#### Generar PDF

```bash
curl -X POST http://localhost:8080/api/invoices/generate-pdf \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"invoiceId": 1}'
```

#### Descargar PDF

```bash
curl -X GET http://localhost:8080/api/documents/1/download \
  -H "Authorization: Bearer $TOKEN" \
  -o invoice.pdf
```

### Auditoría (Trace Service)

```bash
# Ver logs de una factura específica
curl -X GET "http://localhost:8080/api/traces?invoiceId=1" \
  -H "Authorization: Bearer $TOKEN"

# Ver logs de un cliente
curl -X GET "http://localhost:8080/api/traces?clientId=1" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Seguridad y Autenticación

### Arquitectura de Seguridad

```
┌─────────────┐         ┌──────────────┐         ┌───────────────┐
│   Cliente   │  POST   │   Gateway    │  JWT    │ User Service  │
│  Frontend   ├────────►│   :8080      ├────────►│    :8082      │
│             │ /login  │              │ Valid?  │               │
└─────────────┘         └──────┬───────┘         └───────────────┘
                               │
                        JWT Token
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   ┌────▼────┐           ┌─────▼─────┐        ┌──────▼──────┐
   │ Invoice │           │ Document  │        │   Trace     │
   │ Service │           │  Service  │        │  Service    │
   └─────────┘           └───────────┘        └─────────────┘
```

### Flujo de Autenticación JWT

1. **Login**: Usuario envía credenciales a `/api/auth/login`
2. **Validación**: User Service valida contra BD (BCrypt)
3. **Generación JWT**: Se genera token firmado con HS256
4. **Retorno**: Token enviado al cliente
5. **Uso**: Cliente incluye token en header `Authorization: Bearer {token}`
6. **Validación**: Gateway valida token antes de enrutar
7. **Propagación**: Gateway añade header `X-Auth-User` para servicios downstream

### Configuración de Roles

#### Roles Disponibles
- `ROLE_USER` - Usuario estándar (crear facturas, ver propias facturas)
- `ROLE_ADMIN` - Administrador (acceso completo)

#### Control de Acceso por Endpoint

| Endpoint | Roles Requeridos | Restricción Adicional |
|----------|------------------|----------------------|
| `POST /api/auth/register` | Público | - |
| `POST /api/auth/login` | Público | - |
| `GET /api/users` | `ROLE_ADMIN` | - |
| `GET /api/users/{id}` | `ROLE_USER` | Solo propio ID o ADMIN |
| `POST /api/invoices` | `ROLE_USER` | - |
| `GET /api/invoices` | `ROLE_ADMIN` | - |
| `GET /api/invoices/{id}` | `ROLE_USER` | Solo propias facturas o ADMIN |
| `DELETE /api/invoices/{id}` | `ROLE_ADMIN` | - |

### Buenas Prácticas Implementadas

- **Stateless Sessions**: No se almacena estado en servidor
- **BCrypt**: Passwords hasheados con factor 10
- **JWT Expiration**: Tokens expiran en 1 hora (configurable)
- **CORS**: Configurado para orígenes específicos
- **HTTPS Ready**: Preparado para TLS en producción
- **Database per Service**: Cada servicio tiene su BD independiente
- **Secrets Management**: Variables de entorno (no hardcoded)

---

## Testing

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Tests de un servicio específico
cd user-service
mvn test

# Tests con cobertura
mvn test jacoco:report
```

### Tests Disponibles

- **Unit Tests**: `src/test/java/**/*Test.java`
- **Integration Tests**: `src/test/java/**/*IntegrationTest.java`
- **Swagger/Postman**: Ver sección de documentación API

---

## Documentación API

### Swagger UI

Cada servicio expone su propia documentación Swagger:

| Servicio | Swagger URL |
|----------|-------------|
| User Service | http://localhost:8082/swagger-ui.html |
| Invoice Service | http://localhost:8081/swagger-ui.html |
| Document Service | http://localhost:8083/swagger-ui.html |
| Trace Service | http://localhost:8084/swagger-ui.html |

### Postman Collection

Importar la colección desde: `postman/Invoices-Backend.postman_collection.json`

**Variables de entorno Postman**:
```json
{
  "baseUrl": "http://localhost:8080",
  "token": "{{token obtenido del login}}"
}
```

---

## Arquitectura de Base de Datos

### Database per Service Pattern

Cada microservicio tiene su propia base de datos PostgreSQL:

```
┌─────────────────────────────────────────────────────────┐
│              PostgreSQL Server (Port 5432)               │
├───────────────┬────────────────┬────────────┬───────────┤
│    userdb     │   invoicedb    │ documentdb │  tracedb  │
│               │                │            │           │
│ - users       │ - invoices     │ - documents│ - audit   │
│ - user_roles  │ - invoice_items│            │   _logs   │
└───────────────┴────────────────┴────────────┴───────────┘
```

### Esquemas

#### User Service (`userdb`)

```sql
users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
)

user_roles (
    user_id BIGINT REFERENCES users(id),
    roles VARCHAR(50)
)
```

#### Invoice Service (`invoicedb`)

```sql
invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    client_id BIGINT NOT NULL,
    client_email VARCHAR(255) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE,
    subtotal DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
)

invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL
)
```

### Migraciones Flyway

Las migraciones se ejecutan automáticamente al iniciar cada servicio:

```
src/main/resources/db/migration/
├── V1__Create_users_table.sql
├── V2__Add_indexes.sql
└── V3__Insert_default_data.sql
```

---

## Patrones y Mejores Prácticas

### Patrones Implementados

- **Database per Service**: Cada servicio tiene su BD independiente
- **API Gateway**: Punto de entrada único
- **Service Discovery**: Eureka para registro dinámico
- **Circuit Breaker**: (Pendiente: Resilience4j)
- **Event Sourcing**: Kafka para auditoría
- **CQRS**: Separación de comandos y consultas
- **Saga Pattern**: (Pendiente para transacciones distribuidas)

### Mejores Prácticas

- **Stateless JWT**: Sin sesiones en servidor
- **Bean Validation**: Validación en DTOs con `@Valid`
- **Global Exception Handling**: `@ControllerAdvice` en cada servicio
- **Flyway Migrations**: Esquema versionado
- **Lombok**: Reducción de boilerplate
- **Logs Estructurados**: `@Slf4j` con niveles apropiados
- **Health Checks**: Actuator en todos los servicios
- **Docker Multi-Stage**: Imágenes optimizadas
- **Environment Variables**: Configuración externalizada

---

## Troubleshooting

### Problema: Servicios no se registran en Eureka

**Síntomas**: Dashboard de Eureka vacío

**Solución**:
```bash
# Verificar que Eureka esté corriendo
curl http://localhost:8761/actuator/health

# Revisar logs del servicio
docker-compose logs user-service | grep "Registering application"

# Verificar conectividad de red
docker network inspect invoices-network
```

### Problema: Error de conexión a PostgreSQL

**Síntomas**: `Connection refused` o `Unknown host`

**Solución**:
```bash
# Verificar que PostgreSQL esté corriendo
docker-compose ps postgres

# Verificar logs
docker-compose logs postgres

# Probar conexión manual
docker exec -it invoices-postgres psql -U postgres -l
```

### Problema: Kafka no recibe eventos

**Síntomas**: Trace Service no registra eventos

**Solución**:
```bash
# Verificar topics de Kafka
docker exec -it invoices-kafka kafka-topics --list --bootstrap-server localhost:9092

# Ver mensajes en el topic
docker exec -it invoices-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic invoice-events \
  --from-beginning

# Revisar consumer group
docker exec -it invoices-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group trace-group
```

### Problema: JWT inválido o expirado

**Síntomas**: 401 Unauthorized en requests

**Solución**:
1. Verificar que el `JWT_SECRET` sea el mismo en todos los servicios
2. Hacer login nuevamente para obtener token fresco
3. Verificar que el token no haya expirado (1 hora por defecto)

```bash
# Decodificar JWT (sin validar firma)
echo "eyJhbGc..." | cut -d'.' -f2 | base64 -d | jq .
```

### Logs Útiles

```bash
# Ver logs en tiempo real
docker-compose logs -f

# Logs de un servicio específico
docker-compose logs -f user-service

# Buscar errores
docker-compose logs | grep ERROR

# Ver últimas 100 líneas
docker-compose logs --tail=100
```

---

## Roadmap

### Completado ✅
- [x] Arquitectura de microservicios
- [x] Autenticación JWT stateless
- [x] CRUD de usuarios
- [x] CRUD de facturas
- [x] Generación de PDFs con JasperReports
- [x] Almacenamiento en MinIO
- [x] Event-driven con Kafka
- [x] Auditoría de eventos
- [x] Docker Compose completo
- [x] Documentación Swagger
- [x] Bases de datos separadas por servicio
- [x] Flyway migrations
- [x] Global exception handling

### En Progreso 🚧
- [ ] Tests unitarios y de integración (>80% cobertura)
- [ ] Circuit Breakers con Resilience4j
- [ ] Rate Limiting en Gateway
- [ ] Monitoreo con Prometheus + Grafana

### Futuro 🔮
- [ ] Kubernetes deployment (Helm charts)
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] ELK Stack para logs centralizados
- [ ] Métricas de negocio
- [ ] Notificaciones (Email/SMS)
- [ ] Reportes avanzados
- [ ] Multi-tenancy
- [ ] GraphQL API

---

## Contribución

### Cómo Contribuir

1. Fork el repositorio
2. Crear rama feature: `git checkout -b feature/nueva-funcionalidad`
3. Commit cambios: `git commit -m 'feat: añadir nueva funcionalidad'`
4. Push a la rama: `git push origin feature/nueva-funcionalidad`
5. Abrir Pull Request

### Convenciones de Commits

Usamos [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: nueva funcionalidad
fix: corrección de bug
docs: cambios en documentación
style: formateo, sin cambios de código
refactor: refactorización de código
test: añadir tests
chore: tareas de mantenimiento
```

### Estándares de Código

- Java 21 features (Records, Pattern Matching, etc.)
- Google Java Style Guide
- SonarQube quality gates
- >80% cobertura de tests

---

## Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

---

## Contacto y Soporte

- **Issues**: [GitHub Issues](https://github.com/jefmonjor/invoices-back/issues)
- **Email**: support@invoices.com
- **Documentación**: [Wiki](https://github.com/jefmonjor/invoices-back/wiki)

---

## Agradecimientos

- Spring Boot Team
- Spring Cloud Team
- Apache Kafka
- PostgreSQL Community
- MinIO Team
- Todos los contribuidores

---

**Desarrollado con ❤️ usando Spring Boot y Java 21**
