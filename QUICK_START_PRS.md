# 🚀 Quick Start - Crear PRs

## TL;DR - Comandos Rápidos

### Opción 1: Script Automático (Recomendado)
```bash
./create_prs.sh
```

### Opción 2: Comandos Manuales
```bash
# PR #1: Documentación
gh pr create \
  --head claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ \
  --base master \
  --title "docs: añadir documentación técnica completa y mejores prácticas" \
  --body-file PR1_BODY.md \
  --label documentation,enhancement

# PR #2: Clean Architecture + Tests
gh pr create \
  --head claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy \
  --base master \
  --title "feat: implementar Clean Architecture en invoice-service con 90%+ test coverage" \
  --body-file PR2_BODY.md \
  --label enhancement,architecture,testing,needs-discussion
```

### Opción 3: GitHub UI (Sin `gh` CLI)

**PR #1:**
1. Ve a: https://github.com/jefmonjor/invoices-back/compare/master...claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ
2. Click "Create pull request"
3. Título: `docs: añadir documentación técnica completa y mejores prácticas`
4. Copia contenido de `PR1_BODY.md`
5. Labels: `documentation`, `enhancement`
6. Click "Create pull request"

**PR #2:**
1. Ve a: https://github.com/jefmonjor/invoices-back/compare/master...claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy
2. Click "Create pull request"
3. Título: `feat: implementar Clean Architecture en invoice-service con 90%+ test coverage`
4. Copia contenido de `PR2_BODY.md`
5. Labels: `enhancement`, `architecture`, `testing`, `needs-discussion`
6. Click "Create pull request"

---

## ✅ Checklist de Revisión

### PR #1 (Documentación) - 30 min
- [ ] Abrir PR #1
- [ ] Revisar archivos MD se ven bien en GitHub
- [ ] Asignar reviewer (opcional)
- [ ] Mergear (sin esperar aprobación - solo docs)

### PR #2 (Clean Architecture + Tests) - 2-3 horas
- [ ] Abrir PR #2
- [ ] **IMPORTANTE:** Asignar Tech Lead para discusión arquitectónica
- [ ] Revisar localmente:
  ```bash
  git checkout claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy
  cd invoice-service
  mvn clean test
  mvn jacoco:report
  open target/site/jacoco/index.html  # Ver coverage
  ```
- [ ] Decidir estrategia de merge (ver `PR_SUMMARY.md` - Opciones A, B, C)
- [ ] Discutir en equipo antes de mergear
- [ ] Planificar refactorización si se elige Opción A
- [ ] Mergear solo después de consenso del equipo

---

## 📊 Resumen de PRs

| PR | Archivos | Tipo | Riesgo | Tiempo Revisión |
|----|----------|------|--------|-----------------|
| #1 | 3 MD files | Docs | 🟢 Bajo | 30 min |
| #2 | 26 Java files | Code | 🟡 Medio | 2-3 horas |

---

## ✅ Decisión Tomada: Opción A - Clean Architecture

**El equipo ha decidido:** Implementar Clean Architecture completa en invoice-service.

### Plan de ejecución:
1. ✅ Mergear PR #1 (Documentación)
2. ✅ Mergear PR #2 (Clean Architecture + Tests)
3. 🔧 Ejecutar refactorización post-merge (4-6 horas)
   - Eliminar estructura vieja
   - Consolidar a Clean Architecture
   - Validar tests (90%+ coverage)

**Plan detallado en:** `POST_MERGE_REFACTORING_PLAN.md`

---

## 📂 Archivos Creados

```
✅ PR_INSTRUCTIONS.md    - Instrucciones detalladas de cada PR
✅ PR_SUMMARY.md         - Análisis completo con opciones
✅ PR1_BODY.md           - Descripción de PR #1 (lista para copiar)
✅ PR2_BODY.md           - Descripción de PR #2 (lista para copiar)
✅ create_prs.sh         - Script automático para crear PRs
✅ QUICK_START_PRS.md    - Este archivo (referencia rápida)
```

---

## 🆘 Troubleshooting

### Error: `gh: command not found`
**Solución:** Instalar GitHub CLI
```bash
# macOS
brew install gh

# Linux
sudo apt install gh

# Windows
winget install GitHub.cli

# O usa GitHub UI (Opción 3 arriba)
```

### Error: `gh: not logged in`
**Solución:** Autenticarse
```bash
gh auth login
```

### Error: `pull request already exists`
**Solución:** Las PRs ya existen
```bash
# Ver PRs existentes
gh pr list

# Ver en browser
open https://github.com/jefmonjor/invoices-back/pulls
```

### Error: `permission denied`
**Solución:** Verificar permisos
```bash
# Verificar que eres colaborador
gh auth status

# O crear PRs desde GitHub UI
```

---

## 🔗 Links Útiles

- **PRs del Proyecto:** https://github.com/jefmonjor/invoices-back/pulls
- **Issues:** https://github.com/jefmonjor/invoices-back/issues
- **Comparación PR #1:** https://github.com/jefmonjor/invoices-back/compare/master...claude/revisa-que-011CV4nceDGc2JJoj53PvbEZ
- **Comparación PR #2:** https://github.com/jefmonjor/invoices-back/compare/master...claude/clean-architecture-readme-011CV4goSCKxmAXeB3kTR9yy
- **Clean Architecture:** https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html

---

## 📞 Soporte

Si tienes problemas creando las PRs:
1. Revisar `PR_INSTRUCTIONS.md` para instrucciones completas
2. Revisar `PR_SUMMARY.md` para análisis detallado
3. Crear issue en GitHub
4. Contactar al Tech Lead del equipo

---

**¡Éxito con las PRs!** 🎉
