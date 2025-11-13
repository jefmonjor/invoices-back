# 🚀 Instrucciones para Mergear PRs

## Resumen

Tienes **2 PRs listas** para mergear a master:

| # | PR | Base | Archivos | Riesgo | Acción |
|---|----|----- |----------|--------|--------|
| 1 | Documentación | master | 3 MD | 🟢 Bajo | Mergear inmediatamente |
| 2 | Clean Architecture + Tests | master | 26 Java | 🟡 Medio | Mergear + Refactorización |

---

## 📋 Orden de Ejecución

### Paso 1: Mergear PR #1 (Documentación) - 5 minutos

#### Opción A: Desde GitHub UI
1. Ve a: https://github.com/jefmonjor/invoices-back/pulls
2. Encuentra PR: **"docs: añadir documentación técnica completa y mejores prácticas"**
3. Revisa archivos:
   - `RESUMEN_EJECUTIVO.md`
   - `BUENAS_PRACTICAS_Y_RECOMENDACIONES.md`
   - `PLAN_ACCION_EJECUTIVO.md`
4. Click **"Merge pull request"**
5. Confirmar merge
6. ✅ **Listo!**

#### Opción B: Desde terminal
```bash
# Si ya creaste la PR desde GitHub CLI
gh pr merge <PR_NUMBER> --squash
```

---

### Paso 2: Mergear PR #2 (Clean Architecture) - 10 minutos

#### Opción A: Desde GitHub UI
1. Ve a: https://github.com/jefmonjor/invoices-back/pulls
2. Encuentra PR: **"feat: implementar Clean Architecture en invoice-service con 90%+ test coverage"**
3. Revisa cambios (26 archivos):
   - **Domain layer:** entities, usecases, ports
   - **Infrastructure:** persistence, external services
   - **Presentation:** controllers, DTOs
   - **Tests:** 4 test suites (734 líneas)
4. Click **"Merge pull request"**
5. Confirmar merge
6. ✅ **Listo!**

#### Opción B: Desde terminal
```bash
gh pr merge <PR_NUMBER> --squash
```

---

### Paso 3: Ejecutar Refactorización Post-Merge - 4-6 horas

**⚠️ IMPORTANTE:** Después de mergear PR #2, el código tendrá DOS estructuras en paralelo.
Debes ejecutar la refactorización para consolidar a Clean Architecture.

#### 🔧 Ejecutar plan de refactorización:

```bash
# Actualizar master local
git checkout master
git pull origin master

# Seguir plan detallado
cat POST_MERGE_REFACTORING_PLAN.md
```

**El plan incluye:**
1. Pre-validación (compilar, tests)
2. Identificar dependencias
3. Migrar funcionalidad faltante
4. Eliminar código viejo
5. Validar y commitear

**Tiempo estimado:** 4-6 horas

---

## 🎯 Si NO quieres usar GitHub UI

### Crear y Mergear PRs desde Terminal

#### Paso 1: Crear PR #1
```bash
cd /home/user/invoices-back

gh pr create \
  --head claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ \
  --base master \
  --title "docs: añadir documentación técnica completa y mejores prácticas" \
  --body-file PR1_BODY.md \
  --label documentation,enhancement
```

#### Paso 2: Crear PR #2
```bash
gh pr create \
  --head claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy \
  --base master \
  --title "feat: implementar Clean Architecture en invoice-service con 90%+ test coverage" \
  --body-file PR2_BODY.md \
  --label enhancement,architecture,testing
```

#### Paso 3: Listar PRs
```bash
gh pr list
```

#### Paso 4: Mergear PRs
```bash
# Mergear PR #1
gh pr merge <PR_NUMBER_1> --squash --delete-branch

# Mergear PR #2
gh pr merge <PR_NUMBER_2> --squash --delete-branch
```

---

## ⚡ Método Rápido (Script Automático)

### Opción: Usar script para crear PRs
```bash
./create_prs.sh
```

Esto creará ambas PRs automáticamente. Luego debes mergearlas manualmente desde GitHub UI o con:
```bash
gh pr merge <PR_NUMBER> --squash
```

---

## 🔍 Verificación Post-Merge

### Después de mergear ambas PRs:

```bash
# Actualizar master local
git checkout master
git pull origin master

# Verificar que todo está en master
git log --oneline -5

# Debe mostrar:
# - "feat: implementar Clean Architecture..."
# - "docs: añadir documentación técnica..."

# Verificar archivos
ls -la *.md | grep -E "RESUMEN|BUENAS|PLAN"

# Verificar estructura de invoice-service
ls -la invoice-service/src/main/java/com/invoices/invoice_service/
# Debe mostrar: domain/, infrastructure/, presentation/
```

---

## 📊 Estado Post-Merge

### Antes de PRs:
```
✅ 7/9 Fases completas (78%)
⚠️  Test coverage: 0%
📄 Documentación: Básica
```

### Después de PR #1:
```
✅ 7/9 Fases completas (78%)
⚠️  Test coverage: 0%
📄 Documentación: Completa (+4,243 líneas)
```

### Después de PR #2 (sin refactorización):
```
✅ 8/9 Fases completas (89%)
✅ Test coverage: 90%+ (invoice-service)
📄 Documentación: Completa
⚠️  Código duplicado: Sí (estructura vieja + nueva)
```

### Después de Refactorización:
```
✅ 8/9 Fases completas (89%)
✅ Test coverage: 90%+ (invoice-service)
✅ Clean Architecture consolidada
📄 Documentación: Completa
⚠️  Código duplicado: No
```

---

## 🚨 Troubleshooting

### Error: "gh: command not found"
**Solución:** Crear PRs desde GitHub UI manualmente
1. PR #1: https://github.com/jefmonjor/invoices-back/compare/master...claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ
2. PR #2: https://github.com/jefmonjor/invoices-back/compare/master...claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy

### Error: "Merge conflicts"
**Solución:** No debería haber conflictos (ya validado)
- PR #1: Solo agrega archivos MD nuevos
- PR #2: Agrega archivos en directorios nuevos (domain/, infrastructure/, presentation/)

Si hay conflictos:
```bash
git checkout master
git pull origin master
git checkout claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy
git merge master
# Resolver conflictos si aparecen
git push origin claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy
```

### Error: "Permission denied" al mergear
**Solución:** Verifica permisos de GitHub
- Debes ser colaborador del repo con permisos de write
- O pedir a alguien con permisos que mergee

---

## 📞 Siguiente Paso

Una vez mergeadas ambas PRs:
1. ✅ Verificar que los commits están en master
2. 🔧 Ejecutar plan de refactorización: `POST_MERGE_REFACTORING_PLAN.md`
3. ✅ Validar que tests pasan (90%+ coverage)
4. 🎉 Celebrar Fase 8 completa!

---

## 📚 Referencias

- **Plan de Refactorización:** `POST_MERGE_REFACTORING_PLAN.md`
- **Resumen de PRs:** `PR_SUMMARY.md`
- **Quick Start:** `QUICK_START_PRS.md`
- **Instrucciones Completas:** `PR_INSTRUCTIONS.md`

---

**¡Éxito con el merge!** 🚀
