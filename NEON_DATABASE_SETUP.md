# 📝 Configuración de Bases de Datos en Neon

## Bases de Datos Requeridas

Tu proyecto necesita **4 bases de datos** (una por cada servicio):

```
✅ userdb      → User Service (usuarios y autenticación)
✅ invoicedb   → Invoice Service (facturas)
❌ documentdb  → Document Service (documentos y PDFs)
❌ tracedb     → Trace Service (auditoría)
```

## Paso a Paso en Neon

### 1. Ir al SQL Editor de Neon

```
https://console.neon.tech → Tu Proyecto → SQL Editor
```

### 2. Crear las 2 bases de datos faltantes

Copia y pega estos comandos en el SQL Editor:

```sql
-- Crear Document DB
CREATE DATABASE documentdb;

-- Crear Trace DB
CREATE DATABASE tracedb;
```

### 3. Obtener las Connection Strings

Después de crear cada base de datos:

1. Ve a **Dashboard**
2. Selecciona cada base de datos del dropdown
3. Copia el **Connection String**

Ejemplo de cómo se ve:
```
postgresql://neondb_owner:npg_XXXX@ep-proud-breeze-abi4429i-pooler.eu-west-2.aws.neon.tech/documentdb?sslmode=require
```

### 4. Guardar las 4 URLs

Deberías tener algo así:

```bash
# User Service
USER_DB_URL=postgresql://neondb_owner:npg_MT7IHNPGYZ9y@ep-proud-breeze-abi4429i-pooler.eu-west-2.aws.neon.tech/userdb?sslmode=require

# Invoice Service
INVOICE_DB_URL=postgresql://neondb_owner:npg_MT7IHNPGYZ9y@ep-proud-breeze-abi4429i-pooler.eu-west-2.aws.neon.tech/invoicedb?sslmode=require

# Document Service
DOCUMENT_DB_URL=postgresql://neondb_owner:npg_MT7IHNPGYZ9y@ep-proud-breeze-abi4429i-pooler.eu-west-2.aws.neon.tech/documentdb?sslmode=require

# Trace Service
TRACE_DB_URL=postgresql://neondb_owner:npg_MT7IHNPGYZ9y@ep-proud-breeze-abi4429i-pooler.eu-west-2.aws.neon.tech/tracedb?sslmode=require
```

**Nota:** Las 4 URLs son iguales excepto por el nombre de la base de datos al final.

---

## ⚠️ Importante

Para el deployment inicial **simplificado** (free tier), solo necesitas:

### Mínimo Requerido:
```
✅ userdb      → Esencial (autenticación)
✅ invoicedb   → Esencial (core business)
```

### Opcional (agregar después):
```
⚠️  documentdb  → Opcional (si vas a usar almacenamiento de archivos)
⚠️  tracedb     → Opcional (si vas a usar auditoría)
```

**Recomendación:** Crea las 4 para tener el sistema completo, pero puedes empezar solo con `userdb` + `invoicedb` si quieres hacer un deployment rápido inicial.

---

## ✅ Verificación

Para verificar que todas las bases de datos existan:

```sql
-- En Neon SQL Editor:
SELECT datname FROM pg_database WHERE datistemplate = false;
```

Deberías ver:
```
neondb      (default)
userdb
invoicedb
documentdb
tracedb
```
