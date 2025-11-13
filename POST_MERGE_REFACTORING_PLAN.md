# 🏗️ Plan de Refactorización Post-Merge - Clean Architecture

## Contexto

Después de mergear PR #2, el `invoice-service` tendrá **DOS estructuras en paralelo**:

### Estructura VIEJA (a eliminar):
```
invoice-service/src/main/java/com/invoices/invoice_service/
├── controller/
│   └── InvoiceController.java
├── service/
│   ├── InvoiceService.java
│   └── PdfGenerationService.java
├── entity/
│   ├── Invoice.java
│   ├── InvoiceItem.java
│   └── InvoiceStatus.java
├── repository/
│   ├── InvoiceRepository.java
│   └── InvoiceItemRepository.java
├── mapper/
│   ├── InvoiceMapper.java
│   └── InvoiceItemMapper.java
└── dto/
    ├── InvoiceDTO.java
    ├── InvoiceItemDTO.java
    └── ... (otros DTOs se mantienen)
```

### Estructura NUEVA (Clean Architecture - mantener):
```
invoice-service/src/main/java/com/invoices/invoice_service/
├── domain/                    # ← Lógica de negocio pura
│   ├── entities/
│   ├── usecases/
│   ├── ports/
│   └── exceptions/
├── infrastructure/            # ← Implementaciones técnicas
│   ├── persistence/
│   ├── external/
│   └── config/
└── presentation/              # ← REST API
    ├── controllers/
    └── mappers/
```

---

## 📋 Plan de Ejecución (Estimado: 4-6 horas)

### FASE 1: Pre-validación (30 min)

#### ✅ Checkpoint 1.1: Verificar que PR #2 está mergeada
```bash
git checkout master
git pull origin master
git log --oneline -5
# Debe aparecer: "feat: implementar Clean Architecture en invoice-service..."
```

#### ✅ Checkpoint 1.2: Compilar y ejecutar tests
```bash
cd invoice-service
mvn clean install
mvn test

# Verificar coverage
mvn jacoco:report
open target/site/jacoco/index.html
# Debe mostrar >90% coverage
```

#### ✅ Checkpoint 1.3: Verificar que ambas estructuras existen
```bash
# Debe mostrar AMBOS directorios
ls -la src/main/java/com/invoices/invoice_service/ | grep -E "controller|domain"

# Output esperado:
# drwxr-xr-x controller    (viejo)
# drwxr-xr-x domain        (nuevo)
```

---

### FASE 2: Identificar dependencias externas (1 hora)

#### 🔍 Tarea 2.1: Buscar referencias a clases viejas
```bash
cd /home/user/invoices-back

# Buscar imports de estructura vieja en otros servicios
grep -r "import com.invoices.invoice_service.controller.InvoiceController" \
  --include="*.java" \
  --exclude-dir=invoice-service

grep -r "import com.invoices.invoice_service.service.InvoiceService" \
  --include="*.java" \
  --exclude-dir=invoice-service

grep -r "import com.invoices.invoice_service.entity.Invoice" \
  --include="*.java" \
  --exclude-dir=invoice-service
```

**Output esperado:**
- ✅ Si NO hay resultados → Perfecto, no hay dependencias externas
- ⚠️ Si HAY resultados → Anotar archivos que requieren actualización

#### 🔍 Tarea 2.2: Buscar referencias en OpenFeign clients
```bash
# Buscar Feign clients que apunten a invoice-service
grep -r "@FeignClient.*invoice" \
  --include="*.java" \
  gateway-service/ user-service/ document-service/ trace-service/
```

**Acción:** Si existen, verificar que usen DTOs (no entities directamente)

---

### FASE 3: Migrar funcionalidad faltante (2-3 horas)

#### 📦 Tarea 3.1: Comparar funcionalidad entre estructuras

**Crear checklist de funcionalidad:**

| Funcionalidad | Estructura Vieja | Clean Architecture | Estado |
|---------------|------------------|-------------------|--------|
| GET /api/invoices | ✅ InvoiceController | ✅ presentation.controllers.InvoiceController | ✅ Existe |
| POST /api/invoices | ✅ InvoiceController | ⚠️ Verificar | ? |
| PUT /api/invoices/{id} | ✅ InvoiceController | ⚠️ Verificar | ? |
| DELETE /api/invoices/{id} | ✅ InvoiceController | ⚠️ Verificar | ? |
| POST /api/invoices/generate-pdf | ✅ InvoiceController | ✅ GeneratePdfUseCase | ✅ Existe |

**Comando para comparar endpoints:**
```bash
cd invoice-service

# Endpoints en estructura vieja
grep -E "@GetMapping|@PostMapping|@PutMapping|@DeleteMapping" \
  src/main/java/com/invoices/invoice_service/controller/InvoiceController.java

# Endpoints en Clean Architecture
grep -E "@GetMapping|@PostMapping|@PutMapping|@DeleteMapping" \
  src/main/java/com/invoices/invoice_service/presentation/controllers/InvoiceController.java
```

#### 🔧 Tarea 3.2: Migrar funcionalidad faltante

**Si falta algún endpoint:**

1. **Crear UseCase** en `domain/usecases/`
   ```java
   // Ejemplo: CreateInvoiceUseCase.java
   public class CreateInvoiceUseCase {
       private final InvoiceRepository repository;

       public Invoice execute(InvoiceRequest request) {
           // Lógica de negocio
       }
   }
   ```

2. **Implementar tests** en `test/domain/usecases/`
   ```java
   // Ejemplo: CreateInvoiceUseCaseTest.java
   @Test
   void shouldCreateInvoice() {
       // Test con mocks
   }
   ```

3. **Exponer en controller** (`presentation/controllers/InvoiceController.java`)
   ```java
   @PostMapping
   public ResponseEntity<InvoiceDTO> create(@RequestBody InvoiceDTO dto) {
       Invoice invoice = createInvoiceUseCase.execute(mapper.toRequest(dto));
       return ResponseEntity.ok(mapper.toDTO(invoice));
   }
   ```

**Comando para verificar cobertura:**
```bash
mvn test jacoco:report
# Verificar que coverage sigue >90%
```

---

### FASE 4: Actualizar dependencias externas (30 min)

#### 🔄 Tarea 4.1: Actualizar imports en otros servicios

**Si en FASE 2 encontraste dependencias:**

```bash
# Ejemplo: Actualizar en gateway-service
cd gateway-service/src/main/java

# Reemplazar imports viejos por nuevos
# Viejo: import com.invoices.invoice_service.entity.Invoice;
# Nuevo: import com.invoices.invoice_service.domain.entities.Invoice;
```

**⚠️ IMPORTANTE:** Solo DTOs deben cruzar fronteras de servicios, NO entities.

#### 🔄 Tarea 4.2: Verificar que DTOs siguen en su lugar

```bash
# Los DTOs NO deben moverse, deben permanecer en:
ls invoice-service/src/main/java/com/invoices/invoice_service/dto/

# Output esperado:
# InvoiceDTO.java
# InvoiceItemDTO.java
# CreateInvoiceRequest.java
# UpdateInvoiceRequest.java
# GeneratePdfRequest.java
# etc.
```

✅ Los DTOs son la **interfaz pública** del servicio, se mantienen donde están.

---

### FASE 5: Eliminar código viejo (1 hora)

#### 🗑️ Tarea 5.1: Backup antes de eliminar (seguridad)

```bash
cd invoice-service

# Crear branch de backup
git checkout -b backup/old-structure-before-cleanup
git checkout master
```

#### 🗑️ Tarea 5.2: Eliminar directorios viejos

```bash
cd invoice-service/src/main/java/com/invoices/invoice_service

# ELIMINAR estructura vieja
rm -rf controller/
rm -rf service/
rm -rf entity/
rm -rf repository/
rm -rf mapper/

# VERIFICAR que solo queda Clean Architecture
ls -la

# Output esperado:
# drwxr-xr-x domain/
# drwxr-xr-x infrastructure/
# drwxr-xr-x presentation/
# drwxr-xr-x dto/         (se mantiene)
# drwxr-xr-x config/      (se mantiene)
# drwxr-xr-x exception/   (se mantiene o mover a domain/exceptions)
# drwxr-xr-x kafka/       (se mantiene)
# drwxr-xr-x client/      (se mantiene)
```

#### 🗑️ Tarea 5.3: Limpiar archivos redundantes

**Revisar duplicados:**
```bash
# InvoiceStatus puede estar duplicado
find . -name "InvoiceStatus.java"

# Si está en entity/ y domain/entities/, eliminar el de entity/
rm entity/InvoiceStatus.java  # (ya eliminado en paso anterior)
```

---

### FASE 6: Compilar y validar (30 min)

#### ✅ Tarea 6.1: Compilar proyecto completo

```bash
cd /home/user/invoices-back

# Limpiar y compilar TODO
mvn clean install -DskipTests

# Si hay errores de compilación:
# - Revisar imports rotos
# - Actualizar referencias a clases eliminadas
# - Verificar que DTOs estén accesibles
```

#### ✅ Tarea 6.2: Ejecutar todos los tests

```bash
# Tests de invoice-service
cd invoice-service
mvn test

# Tests de otros servicios (verificar que no se rompieron)
cd ../user-service && mvn test
cd ../document-service && mvn test
cd ../trace-service && mvn test
cd ../gateway-service && mvn test
```

**Criterio de éxito:**
- ✅ Todos los tests pasan
- ✅ Coverage en invoice-service >90%
- ✅ No hay regresión en otros servicios

#### ✅ Tarea 6.3: Validar con Swagger

```bash
# Levantar invoice-service
cd invoice-service
mvn spring-boot:run

# En otra terminal, abrir Swagger
open http://localhost:8081/swagger-ui.html
```

**Verificar endpoints:**
- [ ] GET /api/invoices
- [ ] POST /api/invoices
- [ ] GET /api/invoices/{id}
- [ ] PUT /api/invoices/{id}
- [ ] DELETE /api/invoices/{id}
- [ ] POST /api/invoices/generate-pdf

---

### FASE 7: Commit y push (15 min)

#### 📝 Tarea 7.1: Revisar cambios

```bash
git status
git diff --stat
```

#### 📝 Tarea 7.2: Commit de refactorización

```bash
git add .

git commit -m "$(cat <<'EOF'
refactor: migrar invoice-service completamente a Clean Architecture

Elimina estructura vieja (controller/service/entity/repository/mapper)
y consolida todo en arquitectura hexagonal (domain/infrastructure/presentation).

Cambios:
- Eliminar: controller/, service/, entity/, repository/, mapper/
- Mantener: domain/, infrastructure/, presentation/
- Mantener: dto/ (interfaz pública del servicio)
- Actualizar: imports en clases dependientes
- Validar: tests siguen pasando (90%+ coverage)

Beneficios:
- Arquitectura limpia y escalable
- Lógica de negocio independiente de frameworks
- Facilita testing y mantenimiento
- Cumple principios SOLID

Resolves: Fase 8 del roadmap (Testing)
EOF
)"
```

#### 📝 Tarea 7.3: Push a master

```bash
git push origin master
```

---

### FASE 8: Validación final (30 min)

#### 🎯 Tarea 8.1: Tests de integración end-to-end

```bash
# Levantar toda la infraestructura
cd /home/user/invoices-back
docker-compose up -d

# Esperar que todo esté listo (2-3 min)
sleep 120

# Test 1: Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@invoices.com","password":"admin123"}'

# Guardar token
TOKEN="<token_obtenido>"

# Test 2: Crear factura
curl -X POST http://localhost:8080/api/invoices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "clientEmail": "test@example.com",
    "invoiceDate": "2025-11-13",
    "dueDate": "2025-12-13",
    "items": [
      {"description": "Test Item", "quantity": 1, "unitPrice": 100.00}
    ]
  }'

# Test 3: Generar PDF
curl -X POST http://localhost:8080/api/invoices/generate-pdf \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"invoiceId": 1}'

# Test 4: Descargar PDF
curl -X GET http://localhost:8080/api/documents/1/download \
  -H "Authorization: Bearer $TOKEN" \
  -o test_invoice.pdf

# Verificar archivo
file test_invoice.pdf
# Output esperado: test_invoice.pdf: PDF document...
```

#### 🎯 Tarea 8.2: Verificar logs de Kafka

```bash
# Verificar que eventos se publican
docker-compose logs -f trace-service | grep "Invoice"

# Verificar topic de Kafka
docker exec -it invoices-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic invoice-events \
  --from-beginning \
  --max-messages 5
```

#### 🎯 Tarea 8.3: Verificar métricas finales

```bash
# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Eureka dashboard
open http://localhost:8761
# Verificar que invoice-service está registrado
```

---

## ✅ Checklist Final

### Pre-refactorización:
- [x] PR #2 mergeada en master
- [x] Ambas estructuras (vieja y nueva) coexisten
- [x] Tests pasan con 90%+ coverage

### Durante refactorización:
- [ ] Backup creado (`backup/old-structure-before-cleanup`)
- [ ] Dependencias externas identificadas
- [ ] Funcionalidad faltante migrada
- [ ] Estructura vieja eliminada
- [ ] Proyecto compila sin errores
- [ ] Todos los tests pasan

### Post-refactorización:
- [ ] Commit y push completados
- [ ] Tests end-to-end exitosos
- [ ] Kafka events funcionando
- [ ] PDFs se generan correctamente
- [ ] Swagger endpoints accesibles
- [ ] Health checks OK

---

## 🚨 Rollback Plan (si algo sale mal)

### Opción 1: Restaurar desde backup
```bash
git checkout backup/old-structure-before-cleanup
git checkout -b hotfix/restore-old-structure
git push origin hotfix/restore-old-structure
# Crear PR para restaurar
```

### Opción 2: Revertir commit
```bash
git revert HEAD
git push origin master
```

### Opción 3: Reset a commit anterior (PELIGROSO)
```bash
# Solo en emergencias
git reset --hard <commit_before_refactoring>
git push --force origin master  # ⚠️ Requiere permisos
```

---

## 📊 Métricas de Éxito

| Métrica | Antes | Después | Estado |
|---------|-------|---------|--------|
| Arquitectura | MVC tradicional | Clean Architecture | ⏳ |
| Test Coverage | 0% | 90%+ | ⏳ |
| Directorios src/ | 10 | 7 (consolidados) | ⏳ |
| Duplicación de código | Alta | Baja | ⏳ |
| Independencia de frameworks | Baja | Alta | ⏳ |
| Facilidad de testing | Baja | Alta | ⏳ |

---

## 📞 Soporte

Si encuentras problemas durante la refactorización:
1. Revisar logs: `mvn test` para ver qué tests fallan
2. Verificar imports: buscar `import com.invoices.invoice_service.entity` residuales
3. Consultar backup: `git diff backup/old-structure-before-cleanup`
4. Pedir ayuda al equipo en caso de bloqueos

---

## 🎓 Referencias

- **Clean Architecture:** https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- **Hexagonal Architecture:** https://alistair.cockburn.us/hexagonal-architecture/
- **SOLID Principles:** https://www.digitalocean.com/community/conceptual_articles/s-o-l-i-d-the-first-five-principles-of-object-oriented-design

---

**Estimado total:** 4-6 horas
**Prioridad:** Alta (bloquea desarrollo de nuevas features)
**Riesgo:** Medio (mitigado con tests y backup)
**Beneficio:** Alto (arquitectura escalable a largo plazo)

**¡Éxito con la refactorización!** 🚀
