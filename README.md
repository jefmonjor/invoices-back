# Sistema de Gestión de Facturas - Backend Microservicios

**Sistema de facturación empresarial** construido con arquitectura de microservicios, Clean Architecture y Spring Boot 3.4.4 + Java 21.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Clean Architecture](https://img.shields.io/badge/Architecture-Clean-blue.svg)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
[![Code Coverage](https://img.shields.io/badge/Coverage-90%25+-success.svg)](https://www.jacoco.org/)

---

## 📋 Tabla de Contenidos

- [¿Qué es este sistema?](#-qué-es-este-sistema)
- [Características Principales](#-características-principales)
- [Arquitectura](#-arquitectura)
- [Stack Tecnológico](#-stack-tecnológico)
- [Servicios del Sistema](#-servicios-del-sistema)
- [Estructura de Puertos](#-estructura-de-puertos)
- [Base de Datos](#-base-de-datos)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación y Ejecución](#-instalación-y-ejecución)
- [Docker Compose](#-docker-compose)
- [Consumir desde el Frontend](#-consumir-desde-el-frontend)
- [Variables de Entorno](#-variables-de-entorno)
- [Documentación de APIs](#-documentación-de-apis)
- [Testing](#-testing)
- [Estándares de Desarrollo](#-estándares-de-desarrollo)

---

## 🎯 ¿Qué es este sistema?

Sistema **empresarial de gestión de facturas** (invoicing) que permite:

- **Crear, editar y eliminar facturas** con múltiples ítems
- **Generar PDFs profesionales** de facturas con JasperReports
- **Gestionar usuarios, clientes y empresas**
- **Almacenar documentos** en MinIO (compatible S3)
- **Auditar todas las operaciones** con trazabilidad completa
- **Arquitectura escalable** con microservicios independientes
- **Seguridad con JWT** y Spring Security
- **APIs REST documentadas** con OpenAPI 3.0

---

## ✨ Características Principales

### Funcionales
- ✅ **CRUD completo de facturas** (crear, leer, actualizar, eliminar)
- ✅ **Generación automática de PDFs** con plantillas JasperReports
- ✅ **Gestión de usuarios y autenticación** con JWT
- ✅ **Almacenamiento de documentos** en MinIO
- ✅ **Trazabilidad de operaciones** con eventos Kafka
- ✅ **Validación de datos** con Bean Validation
- ✅ **Gestión de clientes y empresas**
- ✅ **Cálculo automático de totales** e impuestos

### Técnicas
- ✅ **Clean Architecture** (Domain, Application, Infrastructure, Presentation)
- ✅ **Microservicios independientes** con Spring Cloud
- ✅ **Service Discovery** con Eureka
- ✅ **API Gateway** con enrutamiento y seguridad centralizada
- ✅ **Mensajería asíncrona** con Apache Kafka
- ✅ **Base de datos por servicio** (Database per Service pattern)
- ✅ **Tests unitarios y de integración** (>90% coverage)
- ✅ **Documentación OpenAPI 3.0** con Swagger UI

---

## 🏗️ Arquitectura

### Arquitectura de Microservicios

```
┌─────────────────────────────────────────────────────────────┐
│                       FRONTEND                              │
│           (React, Angular, Vue, etc.)                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ HTTP/REST (Puerto 8080)
                       ▼
┌──────────────────────────────────────────────────────────────┐
│              API GATEWAY (Puerto 8080)                       │
│    - Enrutamiento de peticiones                             │
│    - Autenticación JWT                                       │
│    - CORS                                                    │
│    - Rate limiting                                           │
└─────┬────────────────┬────────────────┬──────────────┬──────┘
      │                │                │              │
      │                │                │              │
      ▼                ▼                ▼              ▼
┌──────────┐   ┌─────────────┐   ┌──────────┐   ┌──────────┐
│  USER    │   │   INVOICE   │   │ DOCUMENT │   │  TRACE   │
│ SERVICE  │   │   SERVICE   │   │ SERVICE  │   │ SERVICE  │
│ (8082)   │   │   (8081)    │   │ (8083)   │   │ (8084)   │
└────┬─────┘   └──────┬──────┘   └────┬─────┘   └────┬─────┘
     │                │                │              │
     │                │                │              │
     ▼                ▼                ▼              ▼
┌─────────────────────────────────────────────────────────────┐
│                      PostgreSQL                             │
│   userdb  │  invoicedb  │  documentdb  │  tracedb          │
└─────────────────────────────────────────────────────────────┘

             ┌──────────────┐         ┌──────────┐
             │    KAFKA     │         │  MinIO   │
             │  (eventos)   │         │  (PDFs)  │
             └──────────────┘         └──────────┘

┌──────────────────────────────────────────────────────────────┐
│        EUREKA SERVER (Service Discovery - 8761)              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│     CONFIG SERVER (Configuración Centralizada - 8888)        │
└──────────────────────────────────────────────────────────────┘
```

### Clean Architecture (por servicio)

```
┌─────────────────────────────────────────────────────────────┐
│                   FRAMEWORKS & DRIVERS                      │
│         (Spring Boot, JPA, Kafka, JasperReports)           │
│  ┌───────────────────────────────────────────────────────┐ │
│  │            INTERFACE ADAPTERS                         │ │
│  │     (Controllers, Repositories, Kafka Producers)      │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │        APPLICATION BUSINESS RULES               │ │ │
│  │  │         (Use Cases - Casos de Uso)             │ │ │
│  │  │  ┌───────────────────────────────────────────┐ │ │ │
│  │  │  │    ENTERPRISE BUSINESS RULES              │ │ │ │
│  │  │  │    (Entities - Entidades de Dominio)      │ │ │ │
│  │  │  └───────────────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

           Las dependencias fluyen SOLO hacia adentro →→→
```

**Regla de Dependencia:** Las capas internas **NUNCA** dependen de las externas.

---

## 🛠️ Stack Tecnológico

### Backend & Framework
- **Java 21** (LTS) - Lenguaje principal
- **Spring Boot 3.4.4** - Framework de aplicación
- **Spring Cloud 2024.0.1** - Microservicios (Config, Eureka, Gateway)
- **Maven 3.9.9** - Gestión de dependencias

### Base de Datos & Persistencia
- **PostgreSQL 16** - Base de datos relacional
- **Flyway** - Migraciones de base de datos
- **JPA/Hibernate** - ORM
- **Spring Data JPA** - Repositorios

### Mensajería & Eventos
- **Apache Kafka 7.5.0** - Mensajería asíncrona
- **Zookeeper** - Coordinación de Kafka
- **Spring Kafka** - Integración Kafka con Spring

### Almacenamiento & Documentos
- **MinIO** - Almacenamiento de objetos (compatible S3)
- **JasperReports 7.0.2** - Generación de PDFs

### Seguridad
- **Spring Security** - Seguridad y autenticación
- **JWT (jjwt 0.11.5)** - Tokens de autenticación

### API & Documentación
- **OpenAPI 3.0.3** - Especificación de APIs
- **Springdoc OpenAPI 2.6.0** - Documentación automática
- **Swagger UI** - Interfaz de documentación

### Testing & Calidad
- **JUnit 5.11.0** - Framework de testing
- **Mockito** - Mocking
- **AssertJ** - Assertions fluidas
- **H2 Database** - BD en memoria para tests
- **JaCoCo 0.8.11** - Cobertura de código (>90%)
- **Checkstyle** - Estilo de código (Google Java Style)
- **SpotBugs** - Análisis estático
- **ArchUnit** - Tests de arquitectura

### Utilities
- **Lombok** - Reducir boilerplate
- **MapStruct** - Mappers automáticos

---

## 🎯 Servicios del Sistema

### Servicios de Infraestructura

#### 1. **Eureka Server** (Puerto 8761)
**Función:** Service Discovery (registro y descubrimiento de servicios)
- Todos los microservicios se registran automáticamente
- Permite la comunicación entre servicios por nombre
- Dashboard web: `http://localhost:8761`

#### 2. **Config Server** (Puerto 8888)
**Función:** Configuración centralizada
- Gestiona configuración de todos los servicios
- Soporte para múltiples perfiles (dev, prod, test)
- Refresh dinámico de configuración

#### 3. **Gateway Service** (Puerto 8080)
**Función:** Puerta de entrada única para el frontend
- Enrutamiento a microservicios internos
- Autenticación JWT centralizada
- Configuración CORS
- Rate limiting y circuit breaker
- **Este es el único puerto que debe consumir el frontend**

### Servicios de Negocio

#### 4. **User Service** (Puerto 8082)
**Función:** Gestión de usuarios, autenticación y autorización

**Responsabilidades:**
- CRUD de usuarios
- Login y generación de tokens JWT
- Gestión de roles y permisos
- Gestión de clientes (empresas que compran)
- Perfil de usuario

**Base de datos:** `userdb`

#### 5. **Invoice Service** (Puerto 8081)
**Función:** Gestión completa de facturas

**Responsabilidades:**
- CRUD de facturas
- CRUD de ítems de factura
- Generación de PDFs con JasperReports
- Cálculo automático de totales e impuestos
- Cambio de estado de facturas (DRAFT, PENDING, PAID, CANCELLED)
- Publicación de eventos en Kafka
- Validaciones de negocio

**Base de datos:** `invoicedb`

**Clean Architecture:** ✅ Implementado completamente
- Domain Layer (entities, use cases, ports)
- Application Layer (services)
- Infrastructure Layer (persistence, JPA, Jasper)
- Presentation Layer (controllers, DTOs)

#### 6. **Document Service** (Puerto 8083)
**Función:** Almacenamiento y gestión de documentos

**Responsabilidades:**
- Subida de archivos a MinIO
- Descarga de archivos
- Gestión de metadatos de documentos
- Validación de tipos de archivo
- Integración con Invoice Service para PDFs

**Base de datos:** `documentdb`
**Almacenamiento:** MinIO (Puerto 9000, Console 9001)

#### 7. **Trace Service** (Puerto 8084)
**Función:** Trazabilidad y auditoría de operaciones

**Responsabilidades:**
- Consumo de eventos de Kafka
- Registro de todas las operaciones del sistema
- Auditoría de cambios
- Consulta de histórico de operaciones
- Generación de reportes de auditoría

**Base de datos:** `tracedb`
**Kafka:** Consumer del topic `invoice-events`

---

## 🔌 Estructura de Puertos

### Servicios Públicos (Frontend)
| Servicio | Puerto | URL | Descripción |
|----------|--------|-----|-------------|
| **API Gateway** | **8080** | `http://localhost:8080` | **Puerto único para el frontend** |
| Eureka Dashboard | 8761 | `http://localhost:8761` | Dashboard de servicios registrados |

### Servicios Internos (Backend)
| Servicio | Puerto | URL Interna | Swagger UI |
|----------|--------|-------------|------------|
| Config Server | 8888 | `http://localhost:8888` | - |
| User Service | 8082 | `http://localhost:8082` | `http://localhost:8082/swagger-ui.html` |
| Invoice Service | 8081 | `http://localhost:8081` | `http://localhost:8081/swagger-ui.html` |
| Document Service | 8083 | `http://localhost:8083` | `http://localhost:8083/swagger-ui.html` |
| Trace Service | 8084 | `http://localhost:8084` | `http://localhost:8084/swagger-ui.html` |

### Infraestructura
| Componente | Puerto | URL | Descripción |
|------------|--------|-----|-------------|
| PostgreSQL | 5432 | `jdbc:postgresql://localhost:5432` | Base de datos |
| Kafka | 9092 | `localhost:9092` | Mensajería |
| Zookeeper | 2181 | `localhost:2181` | Coordinación Kafka |
| MinIO API | 9000 | `http://localhost:9000` | Almacenamiento S3 |
| MinIO Console | 9001 | `http://localhost:9001` | Dashboard MinIO |

---

## 🗄️ Base de Datos

### Arquitectura: Database per Service

Cada microservicio tiene su propia base de datos independiente para **desacoplamiento total**.

```
PostgreSQL Server (Puerto 5432)
├── userdb         → User Service
├── invoicedb      → Invoice Service
├── documentdb     → Document Service
└── tracedb        → Trace Service
```

### Esquema de Base de Datos

#### userdb (User Service)
```sql
users
├── id (PK)
├── username
├── email
├── password_hash
├── created_at
└── updated_at

roles
├── id (PK)
├── name
└── description

user_roles (Many-to-Many)
├── user_id (FK)
└── role_id (FK)

clients
├── id (PK)
├── name
├── tax_id
├── address
├── phone
└── email
```

#### invoicedb (Invoice Service)
```sql
companies
├── id (PK)
├── name
├── tax_id
├── address
├── phone
├── email
└── logo_url

clients (datos de facturación)
├── id (PK)
├── name
├── tax_id
├── address
└── contact_info

invoices
├── id (PK)
├── invoice_number
├── company_id (FK)
├── client_id (FK)
├── issue_date
├── due_date
├── status (DRAFT, PENDING, PAID, CANCELLED)
├── subtotal
├── tax_amount
├── total_amount
├── created_at
└── updated_at

invoice_items
├── id (PK)
├── invoice_id (FK)
├── description
├── quantity
├── unit_price
├── tax_rate
└── total
```

#### documentdb (Document Service)
```sql
documents
├── id (PK)
├── filename
├── content_type
├── file_size
├── minio_key
├── bucket_name
├── entity_type (INVOICE, USER, etc.)
├── entity_id
├── uploaded_at
└── uploaded_by
```

#### tracedb (Trace Service)
```sql
audit_logs
├── id (PK)
├── event_type (CREATE, UPDATE, DELETE)
├── entity_type (INVOICE, USER, etc.)
├── entity_id
├── user_id
├── timestamp
├── action
├── old_value (JSON)
├── new_value (JSON)
└── ip_address
```

### Migraciones con Flyway

Cada servicio gestiona sus propias migraciones en:
```
src/main/resources/db/migration/
├── V1__Create_initial_schema.sql
├── V2__Add_company_and_client_tables.sql
└── V3__Add_indexes.sql
```

---

## 📦 Requisitos Previos

### Software Requerido

```bash
# Java 21 (LTS)
java -version
# Debe mostrar: openjdk version "21.x.x"

# Maven 3.8+
mvn -version
# Debe mostrar: Apache Maven 3.8.x o superior

# Docker & Docker Compose
docker --version
docker-compose --version

# Git
git --version
```

### Instalación de Requisitos

#### En Ubuntu/Debian
```bash
# Java 21
sudo apt update
sudo apt install openjdk-21-jdk

# Maven
sudo apt install maven

# Docker
sudo apt install docker.io docker-compose

# Agregar usuario al grupo docker
sudo usermod -aG docker $USER
```

#### En macOS
```bash
# Usando Homebrew
brew install openjdk@21
brew install maven
brew install docker
brew install docker-compose
```

#### En Windows
- Descargar Java 21 JDK: https://jdk.java.net/21/
- Descargar Maven: https://maven.apache.org/download.cgi
- Descargar Docker Desktop: https://www.docker.com/products/docker-desktop

---

## 🚀 Instalación y Ejecución

### Opción 1: Docker Compose (Recomendado)

**Levanta todos los servicios con un solo comando:**

```bash
# 1. Clonar repositorio
git clone https://github.com/jefmonjor/invoices-back.git
cd invoices-back

# 2. Copiar variables de entorno
cp .env.example .env
# Editar .env con tus credenciales

# 3. Levantar toda la infraestructura
docker-compose up -d

# 4. Ver logs
docker-compose logs -f

# 5. Verificar que todos los servicios estén UP
docker-compose ps
```

**Espera 2-3 minutos** para que todos los servicios se inicien y se registren en Eureka.

#### Verificar Servicios
```bash
# Eureka Dashboard
http://localhost:8761

# Deberías ver 6 servicios registrados:
# - USER-SERVICE
# - INVOICE-SERVICE
# - DOCUMENT-SERVICE
# - TRACE-SERVICE
# - GATEWAY-SERVICE
# - CONFIG-SERVER
```

### Opción 2: Ejecución Local (Desarrollo)

**Requisito:** PostgreSQL, Kafka y MinIO corriendo (puedes usar docker-compose solo para infraestructura)

#### Paso 1: Levantar infraestructura
```bash
# Levantar solo BD, Kafka y MinIO
docker-compose up -d postgres kafka zookeeper minio
```

#### Paso 2: Compilar todos los servicios
```bash
# Desde la raíz del proyecto
mvn clean install -DskipTests
```

#### Paso 3: Ejecutar servicios en orden

```bash
# Terminal 1: Config Server (primero siempre)
cd config-server
./mvnw spring-boot:run

# Terminal 2: Eureka Server
cd eureka-server
./mvnw spring-boot:run

# Esperar 30 segundos para que Eureka esté listo

# Terminal 3: User Service
cd user-service
./mvnw spring-boot:run

# Terminal 4: Invoice Service
cd invoice-service
./mvnw spring-boot:run

# Terminal 5: Document Service
cd document-service
./mvnw spring-boot:run

# Terminal 6: Trace Service
cd trace-service
./mvnw spring-boot:run

# Terminal 7: Gateway Service (último)
cd gateway-service
./mvnw spring-boot:run
```

#### Verificar que funciona
```bash
# Verificar health de cada servicio
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8082/actuator/health  # User Service
curl http://localhost:8081/actuator/health  # Invoice Service
```

---

## 🐳 Docker Compose

### Comandos Útiles

```bash
# Iniciar todos los servicios
docker-compose up -d

# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f invoice-service

# Detener todos los servicios
docker-compose down

# Detener y eliminar volúmenes (CUIDADO: borra datos)
docker-compose down -v

# Reconstruir imágenes
docker-compose build

# Reconstruir y reiniciar
docker-compose up -d --build

# Ver estado de servicios
docker-compose ps

# Reiniciar un servicio específico
docker-compose restart invoice-service
```

### Orden de Inicio (automático con depends_on)

```
1. postgres, kafka, zookeeper, minio (infraestructura)
2. eureka-server
3. config-server
4. user-service, invoice-service, document-service, trace-service
5. gateway-service
```

### Healthchecks

Todos los servicios tienen healthchecks configurados:
- PostgreSQL: `pg_isready`
- Kafka: `kafka-topics --list`
- MinIO: `curl /minio/health/live`
- Spring Services: `/actuator/health`

---

## 🌐 Consumir desde el Frontend

### URL Base

**El frontend SOLO debe apuntar al Gateway:**

```javascript
const API_BASE_URL = "http://localhost:8080";
```

### Autenticación

#### 1. Login (Obtener Token JWT)

```javascript
// POST http://localhost:8080/api/auth/login
const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: "admin",
    password: "admin123"
  })
});

const data = await response.json();
// Response:
// {
//   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
//   "type": "Bearer",
//   "expiresIn": 3600000
// }

// Guardar token
localStorage.setItem('token', data.token);
```

#### 2. Hacer Peticiones Autenticadas

```javascript
const token = localStorage.getItem('token');

const response = await fetch(`${API_BASE_URL}/api/invoices`, {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
```

### Endpoints Principales

#### User Service (a través del Gateway)

```javascript
// Registro de usuario
POST /api/users/register
Body: { username, email, password }

// Obtener perfil
GET /api/users/me
Headers: { Authorization: Bearer <token> }

// Listar usuarios (admin)
GET /api/users
Headers: { Authorization: Bearer <token> }
```

#### Invoice Service (a través del Gateway)

```javascript
// Listar todas las facturas
GET /api/invoices
Headers: { Authorization: Bearer <token> }

// Obtener factura por ID
GET /api/invoices/{id}
Headers: { Authorization: Bearer <token> }

// Crear factura
POST /api/invoices
Headers: { Authorization: Bearer <token> }
Body: {
  "invoiceNumber": "2025-001",
  "companyId": 1,
  "clientId": 1,
  "issueDate": "2025-11-13",
  "dueDate": "2025-12-13",
  "items": [
    {
      "description": "Servicio de consultoría",
      "quantity": 10,
      "unitPrice": 150.00,
      "taxRate": 21.0
    }
  ]
}

// Actualizar factura
PUT /api/invoices/{id}
Headers: { Authorization: Bearer <token> }
Body: { ... }

// Eliminar factura
DELETE /api/invoices/{id}
Headers: { Authorization: Bearer <token> }

// Generar PDF
POST /api/invoices/{id}/generate-pdf
Headers: { Authorization: Bearer <token> }
Response: application/pdf (binary)
```

#### Document Service (a través del Gateway)

```javascript
// Subir documento
POST /api/documents/upload
Headers: { Authorization: Bearer <token> }
Content-Type: multipart/form-data
Body: FormData with file

// Descargar documento
GET /api/documents/{id}/download
Headers: { Authorization: Bearer <token> }
Response: application/octet-stream
```

#### Trace Service (a través del Gateway)

```javascript
// Obtener auditoría de una factura
GET /api/traces/invoice/{invoiceId}
Headers: { Authorization: Bearer <token> }

// Listar todos los logs de auditoría
GET /api/traces
Headers: { Authorization: Bearer <token> }
```

### Ejemplo Completo: React/TypeScript

```typescript
// api/client.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Interceptor para agregar token automáticamente
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Login
export const login = async (username: string, password: string) => {
  const response = await apiClient.post('/api/auth/login', {
    username,
    password
  });
  return response.data;
};

// Invoices
export const getInvoices = async () => {
  const response = await apiClient.get('/api/invoices');
  return response.data;
};

export const createInvoice = async (invoice: InvoiceDTO) => {
  const response = await apiClient.post('/api/invoices', invoice);
  return response.data;
};

export const downloadInvoicePDF = async (invoiceId: number) => {
  const response = await apiClient.post(
    `/api/invoices/${invoiceId}/generate-pdf`,
    {},
    { responseType: 'blob' }
  );

  // Descargar automáticamente
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `invoice-${invoiceId}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
};
```

### CORS (Cross-Origin Resource Sharing)

El Gateway ya tiene CORS configurado para:
- `http://localhost:3000` (React default)
- `http://localhost:5173` (Vite default)
- `http://localhost:4200` (Angular default)

Si usas otro puerto, agrégalo a la variable de entorno:
```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8000
```

---

## 🔑 Variables de Entorno

### Archivo .env

Crea un archivo `.env` en la raíz del proyecto:

```bash
# ===== POSTGRESQL =====
POSTGRES_ROOT_PASSWORD=postgres_root_pass

# User Service Database
USER_DB_USERNAME=user_service_user
USER_DB_PASSWORD=user_pass_2025

# Invoice Service Database
INVOICE_DB_USERNAME=invoice_service_user
INVOICE_DB_PASSWORD=invoice_pass_2025

# Document Service Database
DOCUMENT_DB_USERNAME=document_service_user
DOCUMENT_DB_PASSWORD=document_pass_2025

# Trace Service Database
TRACE_DB_USERNAME=trace_service_user
TRACE_DB_PASSWORD=trace_pass_2025

# ===== JWT =====
JWT_SECRET=your-super-secret-jwt-key-min-32-chars-base64-encoded-change-in-production
JWT_EXPIRATION_MS=3600000
JWT_ISSUER=invoices-backend

# ===== KAFKA =====
KAFKA_INVOICE_TOPIC=invoice-events
KAFKA_TRACE_GROUP_ID=trace-group

# ===== MINIO =====
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123
MINIO_BUCKET_NAME=invoices-pdfs

# ===== EUREKA =====
EUREKA_USERNAME=eureka-admin
EUREKA_PASSWORD=eureka_pass_2025

# ===== CORS =====
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_MAX_AGE=3600

# ===== SPRING PROFILES =====
SPRING_PROFILES_ACTIVE=dev
```

### Variables Importantes

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `JWT_SECRET` | Clave secreta para firmar tokens JWT (mín 32 chars) | - |
| `JWT_EXPIRATION_MS` | Tiempo de expiración del token en milisegundos | 3600000 (1 hora) |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos para CORS | `http://localhost:3000` |
| `POSTGRES_ROOT_PASSWORD` | Password del usuario root de PostgreSQL | `postgres_root_pass` |
| `MINIO_ACCESS_KEY` | Usuario de MinIO | `minioadmin` |
| `MINIO_SECRET_KEY` | Password de MinIO | `minioadmin123` |

---

## 📚 Documentación de APIs

### OpenAPI 3.0 / Swagger UI

Cada servicio expone su documentación OpenAPI:

```
Invoice Service:
http://localhost:8081/swagger-ui.html
http://localhost:8081/v3/api-docs

User Service:
http://localhost:8082/swagger-ui.html
http://localhost:8082/v3/api-docs

Document Service:
http://localhost:8083/swagger-ui.html
http://localhost:8083/v3/api-docs

Trace Service:
http://localhost:8084/swagger-ui.html
http://localhost:8084/v3/api-docs
```

### Especificaciones YAML

Las especificaciones OpenAPI están en:
```
invoice-service/src/main/resources/openapi/invoice-api.yaml
user-service/src/main/resources/openapi/user-api.yaml
document-service/src/main/resources/openapi/document-api.yaml
trace-service/src/main/resources/openapi/trace-api.yaml
```

### Colección de Postman

Importa la colección de Postman para probar todos los endpoints:
```
postman/Invoices-Backend.postman_collection.json
```

---

## 🧪 Testing

### Ejecutar Todos los Tests

```bash
# Desde la raíz del proyecto
mvn clean test

# Con reporte de cobertura
mvn clean test jacoco:report
```

### Tests por Servicio

```bash
# Invoice Service
cd invoice-service
mvn test

# User Service
cd user-service
mvn test
```

### Ver Reporte de Cobertura (JaCoCo)

```bash
# Generar reporte
cd invoice-service
mvn clean test jacoco:report

# Abrir en navegador
open target/site/jacoco/index.html       # macOS
xdg-open target/site/jacoco/index.html  # Linux
start target/site/jacoco/index.html      # Windows
```

### Tipos de Tests Implementados

#### 1. Tests Unitarios (Unit Tests)
**Ubicación:** `src/test/java/com/invoices/{service}/domain/`

**Qué testean:**
- Entidades de dominio (lógica de negocio)
- Casos de uso (use cases)
- Validaciones

**Ejemplo:**
```java
@ExtendWith(MockitoExtension.class)
class GetInvoiceByIdUseCaseTest {

    @Mock
    private InvoiceRepository repository;

    private GetInvoiceByIdUseCase useCase;

    @Test
    void shouldReturnInvoiceWhenIdIsValid() {
        // Test implementation
    }
}
```

#### 2. Tests de Integración (Integration Tests)
**Ubicación:** `src/test/java/com/invoices/{service}/`

**Qué testean:**
- Flujo completo: Controller → Service → Repository → Database
- Interacciones con base de datos (H2 en memoria)
- Validaciones end-to-end

**Ejemplo:**
```java
@SpringBootTest
@AutoConfigureTestDatabase
class InvoiceServiceIntegrationTest {

    @Autowired
    private InvoiceRepository repository;

    @Test
    void shouldCreateAndRetrieveInvoice() {
        // Test implementation
    }
}
```

#### 3. Tests de Controladores (Controller Tests)
**Ubicación:** `src/test/java/com/invoices/{service}/presentation/`

**Qué testean:**
- Endpoints REST
- Validación de request/response
- Seguridad (JWT)

**Ejemplo:**
```java
@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnInvoiceWhenExists() throws Exception {
        mockMvc.perform(get("/api/invoices/1"))
               .andExpect(status().isOk());
    }
}
```

### Cobertura de Código

**Objetivo:** Mínimo 90% líneas, 85% branches

```bash
# Verificar que se cumple el mínimo
mvn clean verify

# Si no cumple, el build falla automáticamente
```

### Ejecutar Checkstyle (Calidad de Código)

```bash
mvn checkstyle:check
```

### Ejecutar SpotBugs (Análisis Estático)

```bash
mvn spotbugs:check
```

---

## 📐 Estándares de Desarrollo

### Clean Architecture

**Regla de Dependencia:** Las capas internas NUNCA dependen de las externas.

#### Estructura por Servicio (invoice-service como ejemplo)

```
src/main/java/com/invoices/invoice_service/
│
├── domain/                          # ← NÚCLEO DEL NEGOCIO (sin deps externas)
│   ├── entities/                    # Entidades de dominio (lógica pura)
│   │   ├── Invoice.java
│   │   ├── InvoiceItem.java
│   │   ├── Company.java
│   │   └── Client.java
│   ├── usecases/                    # Casos de uso (reglas de negocio)
│   │   ├── CreateInvoiceUseCase.java
│   │   ├── GetInvoiceByIdUseCase.java
│   │   └── GeneratePdfUseCase.java
│   ├── ports/                       # Interfaces (Dependency Inversion)
│   │   ├── InvoiceRepository.java   # Port de salida
│   │   └── PdfGeneratorService.java # Port de salida
│   └── exceptions/                  # Excepciones de dominio
│       ├── InvoiceNotFoundException.java
│       └── InvalidInvoiceStateException.java
│
├── application/                     # ← CAPA DE APLICACIÓN
│   └── services/                    # Servicios de aplicación (orquestación)
│       └── InvoiceApplicationService.java
│
├── infrastructure/                  # ← ADAPTADORES TÉCNICOS
│   ├── persistence/                 # Adaptador de persistencia (JPA)
│   │   ├── entities/                # JPA Entities (modelo de BD)
│   │   │   ├── InvoiceJpaEntity.java
│   │   │   └── InvoiceItemJpaEntity.java
│   │   ├── repositories/            # Implementaciones de ports
│   │   │   ├── JpaInvoiceRepository.java
│   │   │   └── InvoiceRepositoryImpl.java
│   │   └── mappers/                 # Mappers Domain ↔ JPA
│   │       └── InvoiceJpaMapper.java
│   ├── external/                    # Adaptadores externos
│   │   └── jasper/
│   │       └── JasperPdfGeneratorService.java
│   ├── messaging/                   # Kafka producers/consumers
│   │   └── InvoiceEventProducer.java
│   └── config/                      # Configuración de Spring
│       └── UseCaseConfiguration.java
│
└── presentation/                    # ← CAPA DE PRESENTACIÓN
    ├── controllers/                 # REST Controllers
    │   └── InvoiceController.java
    ├── dto/                         # DTOs (contratos de API)
    │   ├── InvoiceDTO.java
    │   └── CreateInvoiceRequest.java
    ├── mappers/                     # Mappers Domain ↔ DTO
    │   └── InvoiceDtoMapper.java
    └── exceptionhandlers/           # Manejo global de excepciones
        └── GlobalExceptionHandler.java
```

### Clean Code - Principios Obligatorios

#### 1. Nombres Significativos
```java
// ❌ MAL
class Usr { }
void getData() { }
int d; // elapsed time in days

// ✅ BIEN
class User { }
void getUserById() { }
int elapsedTimeInDays;
```

#### 2. Funciones Cortas (max 20 líneas)
```java
// ✅ BIEN - Una responsabilidad
public void finalize() {
    if (items.isEmpty()) {
        throw new IllegalStateException("Cannot finalize invoice without items");
    }
    this.status = InvoiceStatus.FINALIZED;
}
```

#### 3. Responsabilidad Única (SRP)
```java
// ✅ BIEN - Cada clase tiene una razón para cambiar
class Invoice { } // Representa una factura
class InvoiceRepository { } // Persiste facturas
class InvoicePdfGenerator { } // Genera PDFs
```

#### 4. Dependency Inversion (Ports & Adapters)
```java
// ✅ BIEN - Depende de abstracción, no de implementación
public class GetInvoiceByIdUseCase {
    private final InvoiceRepository repository; // Port (interfaz)

    public GetInvoiceByIdUseCase(InvoiceRepository repository) {
        this.repository = repository;
    }
}
```

#### 5. Excepciones Específicas
```java
// ❌ MAL
throw new Exception("Not found");

// ✅ BIEN
throw new InvoiceNotFoundException(invoiceId);
```

#### 6. Tests con Patrón AAA (Arrange-Act-Assert)
```java
@Test
void shouldCalculateTotalCorrectly() {
    // ARRANGE
    Invoice invoice = new Invoice(1L, "2025-001", LocalDateTime.now());
    invoice.addItem(new InvoiceItem("Item 1", 2, BigDecimal.valueOf(100)));

    // ACT
    BigDecimal total = invoice.calculateTotalAmount();

    // ASSERT
    assertThat(total).isEqualTo(BigDecimal.valueOf(200));
}
```

### Convenciones de Código

#### Formato
- **Indentación:** 4 espacios (no tabs)
- **Línea máxima:** 120 caracteres
- **Imports:** Ordenados alfabéticamente
- **Checkstyle:** Google Java Style Guide

#### Nomenclatura
- **Clases:** PascalCase (`InvoiceService`)
- **Métodos:** camelCase (`getUserById`)
- **Constantes:** UPPER_SNAKE_CASE (`MAX_RETRIES`)
- **Packages:** lowercase (`com.invoices.domain`)

#### Git Commits (Conventional Commits)
```bash
feat: agregar endpoint para generar PDF
fix: corregir cálculo de impuestos
refactor: separar lógica de validación
test: agregar tests para CreateInvoiceUseCase
docs: actualizar README con nuevos endpoints
chore: actualizar dependencias
```

---

## 🚨 Troubleshooting

### Problema: Servicios no se registran en Eureka

**Solución:**
```bash
# 1. Verificar que Eureka esté corriendo
curl http://localhost:8761

# 2. Ver logs del servicio
docker-compose logs invoice-service

# 3. Esperar 30-60 segundos (el registro es gradual)
```

### Problema: Error de conexión a PostgreSQL

**Solución:**
```bash
# 1. Verificar que PostgreSQL esté corriendo
docker-compose ps postgres

# 2. Verificar logs
docker-compose logs postgres

# 3. Verificar que las BDs existan
docker exec -it invoices-postgres psql -U postgres -c "\l"

# 4. Recrear contenedor si es necesario
docker-compose down postgres
docker-compose up -d postgres
```

### Problema: Token JWT inválido

**Solución:**
```bash
# 1. Verificar que JWT_SECRET sea el mismo en todos los servicios
grep JWT_SECRET .env

# 2. Obtener un nuevo token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 3. Usar el token en Authorization: Bearer <token>
```

### Problema: Kafka no arranca

**Solución:**
```bash
# 1. Zookeeper debe estar corriendo primero
docker-compose up -d zookeeper
sleep 10

# 2. Luego iniciar Kafka
docker-compose up -d kafka

# 3. Verificar que Kafka esté listo
docker exec -it invoices-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Problema: Build Maven falla

**Solución:**
```bash
# 1. Limpiar caché de Maven
mvn clean

# 2. Compilar sin tests
mvn install -DskipTests

# 3. Si falla por dependencias
rm -rf ~/.m2/repository
mvn clean install
```

---

## 📄 Licencia

Este proyecto es un ejemplo de arquitectura limpia para sistemas empresariales.

---

## 👥 Contribuciones

### Cómo Contribuir

1. Fork el repositorio
2. Crea una rama para tu feature (`git checkout -b feature/amazing-feature`)
3. Commit tus cambios (`git commit -m 'feat: add amazing feature'`)
4. Push a la rama (`git push origin feature/amazing-feature`)
5. Abre un Pull Request

### Checklist de PR

- [ ] El código sigue Clean Architecture
- [ ] Tests unitarios agregados (>90% coverage)
- [ ] Checkstyle pasa sin errores
- [ ] SpotBugs no reporta bugs
- [ ] Documentación actualizada
- [ ] Commit messages siguen Conventional Commits

---

## 📞 Soporte

- **Issues:** https://github.com/jefmonjor/invoices-back/issues
- **Documentación:** Ver archivos OpenAPI en `src/main/resources/openapi/`
- **Wiki:** https://github.com/jefmonjor/invoices-back/wiki

---

## 🎯 Roadmap

### Fase Actual: ✅ Sistema Base Completado
- ✅ Microservicios funcionando
- ✅ Clean Architecture implementada
- ✅ Generación de PDFs
- ✅ Tests unitarios y de integración
- ✅ Docker Compose

### Próximas Fases

#### Fase 2: Seguridad Avanzada
- [ ] Refresh tokens
- [ ] OAuth2 / OpenID Connect
- [ ] Rate limiting por usuario
- [ ] Auditoría de seguridad

#### Fase 3: Funcionalidades Avanzadas
- [ ] Notificaciones por email
- [ ] Recordatorios de pago
- [ ] Dashboard de métricas
- [ ] Reportes avanzados
- [ ] Multi-tenant

#### Fase 4: Infraestructura
- [ ] Kubernetes deployment
- [ ] CI/CD con GitHub Actions
- [ ] Monitoreo con Prometheus + Grafana
- [ ] Logging centralizado (ELK Stack)
- [ ] Distributed tracing (Jaeger/Zipkin)

---

## 📊 Métricas del Proyecto

| Métrica | Valor |
|---------|-------|
| **Servicios** | 7 (4 negocio + 3 infraestructura) |
| **Clases Java** | ~200 |
| **Líneas de código** | ~15,000 |
| **Tests** | ~80 casos |
| **Cobertura** | >90% |
| **Endpoints API** | ~30 |
| **Bases de datos** | 4 (userdb, invoicedb, documentdb, tracedb) |
| **Tecnologías** | 20+ |

---

**¡Sistema listo para producción!** 🚀

Para más información técnica, consulta los archivos OpenAPI en cada servicio o visita Swagger UI en desarrollo.
