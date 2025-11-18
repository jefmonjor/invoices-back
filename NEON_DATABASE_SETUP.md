# 📝 Configuración de Base de Datos en Neon

Esta guía te ayudará a configurar tu base de datos PostgreSQL en **Neon** para el monolito de gestión de facturas.

---

## 📊 Arquitectura de Base de Datos

El monolito usa **UNA SOLA base de datos consolidada** con todas las tablas:

```
📦 invoices (database)
├── 👥 users              # Usuarios y autenticación
├── 🎭 user_roles         # Roles de usuarios
├── 🏢 companies          # Empresas emisoras
├── 👤 clients            # Clientes receptores
├── 📄 invoices           # Facturas
├── 📋 invoice_items      # Ítems de facturas
├── 📎 documents          # Documentos PDF
└── 🔍 audit_logs         # Logs de auditoría
```

**✅ Ventajas:**
- Una sola connection string
- Joins entre tablas posibles
- Transacciones ACID completas
- Menor complejidad
- Mayor rendimiento

**vs. Microservicios (obsoleto):**
- ~~4 databases separadas~~
- ~~4 connection strings~~
- ~~Joins imposibles~~
- ~~Transacciones distribuidas complejas~~

---

## 🚀 Parte 1: Crear Cuenta en Neon

### Paso 1: Registro

1. Visita: **https://neon.tech/**
2. Click en **"Sign Up"**
3. Opciones:
   - GitHub (recomendado)
   - Google
   - Email + Password

### Paso 2: Verificar Email

Si usaste email, verifica tu cuenta:
```
1. Revisa tu bandeja de entrada
2. Click en el link de verificación
3. Regresa a Neon Console
```

---

## 💾 Parte 2: Crear Base de Datos

### Paso 1: Crear Proyecto

```bash
# 1. En Neon Console: https://console.neon.tech/

# 2. Click "Create Project"

# 3. Configurar:
#    - Project name: invoices-monolith
#    - Region: AWS eu-west-2 (Londres - recomendado)
#    - PostgreSQL version: 16
#    - Compute size: 0.25 vCPU (free tier)

# 4. Click "Create Project"
```

### Paso 2: Verificar Base de Datos Creada

Neon crea automáticamente una database por defecto con el nombre de tu proyecto.

**Opción A:** Usar la database por defecto
```sql
-- Database creada: neondb (renombrarla a 'invoices')
-- O simplemente usarla como está
```

**Opción B:** Crear database específica (recomendado)

En el SQL Editor de Neon:
```sql
-- Crear la base de datos principal
CREATE DATABASE invoices;

-- Verificar
\l
```

### Paso 3: Obtener Connection String

```bash
# 1. En el Dashboard del proyecto

# 2. Seleccionar "invoices" en el dropdown de databases

# 3. Copiar el "Connection String" - debería verse así:
postgresql://neondb_owner:npg_XXXXXXXXXXXX@ep-proud-breeze-a1b2c3d4.eu-west-2.aws.neon.tech/invoices?sslmode=require
```

**⚠️ Importante:**
- Asegúrate de que la URL termine con `/invoices?sslmode=require`
- Si usaste el default `neondb`, cambia a `/neondb?sslmode=require`
- El `?sslmode=require` es **obligatorio** en Neon

---

## 🔧 Parte 3: Configurar Variables de Entorno

### Variables Requeridas

```bash
# Database Connection (una sola!)
SPRING_DATASOURCE_URL=postgresql://neondb_owner:npg_XXXX@ep-proud-breeze-a1b2c3d4.eu-west-2.aws.neon.tech/invoices?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=npg_XXXXXXXXXXXX
```

### Archivo .env (desarrollo local)

Crear archivo `.env` en la raíz:

```bash
# Neon PostgreSQL - Monolito
SPRING_DATASOURCE_URL=postgresql://neondb_owner:npg_XXXX@ep-xxx.eu-west-2.aws.neon.tech/invoices?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=npg_XXXXXXXXXXXX
```

### Docker Compose (desarrollo local)

El `docker-compose.yml` ya está configurado para usar PostgreSQL local:

```yaml
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_DB: invoices  # Una sola DB
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: postgres
```

**Para desarrollo local:** Usa PostgreSQL local con Docker

**Para producción/staging:** Usa Neon

---

## 🗄️ Parte 4: Esquema de Base de Datos

### Tablas Creadas Automáticamente por Flyway

El monolito usa **Flyway** para gestionar el esquema. Al arrancar, Flyway ejecuta las migraciones automáticamente:

```
invoices-monolith/src/main/resources/db/migration/
├── V1__create_users_tables.sql
├── V2__create_invoices_tables.sql
├── V3__create_documents_tables.sql
└── V4__create_audit_logs_tables.sql
```

### Tablas Principales

#### 1. Módulo User (Autenticación)

```sql
-- users: Usuarios del sistema
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- user_roles: Roles de usuarios (ADMIN, USER, etc.)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### 2. Módulo Invoice (Facturas)

```sql
-- companies: Empresas emisoras de facturas
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50) UNIQUE NOT NULL,
    address TEXT,
    phone VARCHAR(50),
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- clients: Clientes receptores de facturas
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- invoices: Facturas
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    company_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal DECIMAL(15, 2) NOT NULL,
    tax_amount DECIMAL(15, 2) NOT NULL,
    total DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id),
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

-- invoice_items: Ítems de cada factura
CREATE TABLE invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    tax_rate DECIMAL(5, 2) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);
```

#### 3. Módulo Document (Documentos PDF)

```sql
-- documents: PDFs generados
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_object_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    invoice_id BIGINT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL
);
```

#### 4. Módulo Trace (Auditoría)

```sql
-- audit_logs: Logs de auditoría de eventos
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    user_id BIGINT,
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

---

## ✅ Parte 5: Verificación

### 1. Verificar Conexión desde Local

```bash
# Opción 1: Usar psql
psql "postgresql://neondb_owner:npg_XXX@ep-xxx.eu-west-2.aws.neon.tech/invoices?sslmode=require"

# Opción 2: Docker con psql
docker run --rm -it postgres:16-alpine psql "postgresql://neondb_owner:npg_XXX@ep-xxx.eu-west-2.aws.neon.tech/invoices?sslmode=require"
```

### 2. Verificar que las Tablas Existan

```sql
-- Listar todas las tablas
\dt

-- Deberías ver:
--  users
--  user_roles
--  companies
--  clients
--  invoices
--  invoice_items
--  documents
--  audit_logs
--  flyway_schema_history (tabla de Flyway)
```

### 3. Test de Query Básico

```sql
-- Ver cuántos usuarios hay
SELECT COUNT(*) FROM users;

-- Ver las migraciones ejecutadas
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

---

## 🐛 Troubleshooting

### Error: "FATAL: password authentication failed"

**Problema:** Contraseña incorrecta o URL malformada

**Solución:**
1. Ir a Neon Console → Dashboard
2. Regenerar password:
   - Settings → Reset password
   - Copiar nueva password
3. Actualizar `SPRING_DATASOURCE_URL` y `DB_PASSWORD`

---

### Error: "SSL connection is required"

**Problema:** Falta el parámetro `?sslmode=require`

**Solución:**
```bash
# URL incorrecta
postgresql://neondb_owner:xxx@ep-xxx.eu-west-2.aws.neon.tech/invoices

# URL correcta
postgresql://neondb_owner:xxx@ep-xxx.eu-west-2.aws.neon.tech/invoices?sslmode=require
```

---

### Error: "database 'invoices' does not exist"

**Problema:** La database no fue creada

**Solución:**
```sql
-- En Neon SQL Editor:
CREATE DATABASE invoices;

-- Verificar
\l
```

---

### Error: "Flyway migration failed"

**Problema:** Las migraciones Flyway fallaron

**Verificar logs:**
```bash
# Si estás corriendo local
docker logs invoices-monolith | grep Flyway

# Si estás en Fly.io
fly logs -a invoices-monolith | grep Flyway
```

**Soluciones comunes:**
1. Verificar que la database esté vacía la primera vez
2. Verificar que los scripts SQL en `db/migration/` sean válidos
3. Limpiar schema corrupto:
   ```sql
   DROP SCHEMA public CASCADE;
   CREATE SCHEMA public;
   ```

---

### Error: "Connection pool exhausted"

**Problema:** Neon free tier tiene límites de conexiones

**Solución:**
```yaml
# En application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5  # Reducir pool size
      minimum-idle: 2
```

---

## 📊 Limits del Free Tier

### Neon Free Tier Incluye:

```
✅ 1 proyecto
✅ 10 branches (feature branches)
✅ 500 MB de storage
✅ Compute: 0.25 vCPU, 1 GB RAM
✅ Conexiones: hasta 100 simultáneas
✅ Backup automático (7 días)
✅ SSL/TLS incluido
```

**Para 2-10 usuarios básicos:** Suficiente

**Para escalar:** Upgrade a plan pago ($19/mes para 10 GB)

---

## 🔒 Seguridad

### 1. Nunca Commitear Credenciales

```bash
# Asegurarse de que .env esté en .gitignore
echo ".env" >> .gitignore
echo ".env.production" >> .gitignore
echo "**/*.env" >> .gitignore
```

### 2. Usar Secrets en Fly.io

```bash
# Configurar secrets en Fly.io (no variables de entorno públicas)
fly secrets set SPRING_DATASOURCE_URL="postgresql://..." -a invoices-monolith
fly secrets set DB_USERNAME="neondb_owner" -a invoices-monolith
fly secrets set DB_PASSWORD="npg_XXXX" -a invoices-monolith
```

### 3. Rotar Passwords Regularmente

```bash
# En Neon Console:
# Settings → Reset password

# Luego actualizar en Fly.io:
fly secrets set DB_PASSWORD="new_password" -a invoices-monolith
```

### 4. Usar SSL Always

Neon **requiere SSL** por defecto - siempre incluir `?sslmode=require`

---

## 📚 Recursos Adicionales

### Documentación Oficial

- **Neon Docs:** https://neon.tech/docs/
- **Neon QuickStart:** https://neon.tech/docs/get-started-with-neon/signing-up
- **Flyway Docs:** https://flywaydb.org/documentation/

### Dashboards

- **Neon Console:** https://console.neon.tech/
- **SQL Editor:** https://console.neon.tech/ → SQL Editor
- **Monitoring:** https://console.neon.tech/ → Monitoring

### Guías Relacionadas

- [Variables de Entorno](./ENVIRONMENT_VARIABLES.md)
- [Servicios Gratuitos](./FREE_SERVICES_SETUP.md)
- [README Principal](./README.md)

---

## ✅ Checklist Final

Antes de continuar con el deployment, verifica:

- [ ] Cuenta de Neon creada y verificada
- [ ] Proyecto "invoices-monolith" creado
- [ ] Base de datos "invoices" creada
- [ ] Connection string copiada
- [ ] Variables de entorno configuradas localmente
- [ ] Conexión verificada con psql o similar
- [ ] Flyway migrations ejecutadas correctamente
- [ ] Todas las 8 tablas creadas (users, companies, invoices, etc.)
- [ ] Query de prueba funcionando
- [ ] Secrets configurados en Fly.io (si aplica)

---

## 🎯 Siguiente Paso

Una vez completada esta configuración, puedes:

1. **Desarrollo Local:**
   ```bash
   cd invoices-monolith
   mvn spring-boot:run
   ```

2. **Deploy a Fly.io:**
   ```bash
   fly deploy -c invoices-monolith/fly.toml
   ```

3. **Verificar Health:**
   ```bash
   curl https://invoices-monolith.fly.dev/actuator/health
   ```

¡Tu base de datos está lista! 🎉
