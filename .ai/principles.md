# Principios de Desarrollo - Transolido

> "Código simple que funciona > Código perfecto que no existe"

## Filosofía
- Pragmatismo sobre perfeccionismo
- Implementar solo lo necesario AHORA
- Refactorizar cuando sea necesario, no antes

## Clean Code
- Nombres descriptivos, no abreviaciones
- Métodos pequeños, una responsabilidad
- Early return, evitar nesting profundo
- Máximo 30 líneas/método, 300 líneas/clase

## Clean Architecture (Hexagonal)
```
Presentation → Application → Domain ← Infrastructure
```
- Domain NUNCA importa de Infrastructure
- Ports (interfaces) en domain
- Adapters (implementaciones) en infrastructure
- Use Cases = @Service con @Transactional

## KISS - Keep It Simple
- Código directo, sin sobre-ingeniería
- No crear abstracciones innecesarias
- Si una solución simple funciona, úsala

## YAGNI - You Aren't Gonna Need It
- No crear "por si acaso"
- No añadir parámetros opcionales anticipados
- No crear interfaces para una sola implementación

## DRY - Don't Repeat Yourself
- Reutilizar lógica común en helpers/utils
- Componentes reutilizables
- Pero: duplicar es mejor que la abstracción incorrecta

## Git Commits
```
feat: nueva funcionalidad
fix: corrección de bug
refactor: sin cambio funcional
docs: documentación
test: tests
chore: mantenimiento
```

## Checklist Pre-Commit
- [ ] Compila sin errores
- [ ] Tests pasan
- [ ] Nombres descriptivos
- [ ] Sin código duplicado
- [ ] Logs apropiados
