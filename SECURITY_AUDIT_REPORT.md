# AUDITORÍA DE SEGURIDAD Y ARQUITECTURA - BACKEND INVOICES
## Proyecto: invoices-back
**Fecha**: 13 de Noviembre 2025
**Auditor**: Claude Code
**Versión del Proyecto**: 0.0.1-SNAPSHOT
**Stack**: Spring Boot 3.4.4 + Spring Cloud 2024.0.1 + Java 21

---

## RESUMEN EJECUTIVO

### Estado General
El proyecto **invoices-back** presenta una **arquitectura de microservicios bien diseñada** con 7 servicios (3 de infraestructura + 4 de negocio), pero se encuentra en **fase inicial de desarrollo** con **MÚLTIPLES VULNERABILIDADES CRÍTICAS DE SEGURIDAD** que impiden su despliegue en producción.

### Hallazgos Clave
- **🔴 CRÍTICO**: 4 bloqueadores que impiden funcionamiento seguro
- **🟠 ALTO**: 6 problemas de arquitectura y seguridad
- **🟡 MEDIO**: 4 mejoras necesarias antes de producción
- **✅ POSITIVO**: Arquitectura moderna, event-driven, bien documentada

### Veredicto
**NO PRODUCTION-READY** - Se requieren **2-3 meses de desarrollo** para resolver bloqueadores críticos e implementar lógica de negocio.

---

## 1. BLOQUEADORES CRÍTICOS 🔴

Problemas que **IMPIDEN** el funcionamiento seguro o correcto del sistema.

### 1.1 Credenciales Hardcodeadas en Git

**Severidad**: 🔴 CRÍTICA
**Impacto**: Exposición de credenciales de base de datos a cualquier persona con acceso al repositorio

**Ubicaciones Afectadas**:
```
user-service/src/main/resources/application.yml:14-15
invoice-service/src/main/resources/application.yml:14-15
document-service/src/main/resources/application.yml:14-15
trace-service/src/main/resources/application.yml:14-15
```

**Código Vulnerable**:
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/invoicesdb
  username: user
  password: password  # ⚠️ CREDENCIAL EN TEXTO PLANO VERSIONADA EN GIT
```

**Impacto**:
- Cualquier desarrollador con acceso al repo puede ver las credenciales
- Las credenciales están en el historial de Git (no se eliminan con un simple commit)
- Violación de seguridad básica y compliance (GDPR, PCI-DSS)
- Puerta de entrada para ataques a base de datos

**Acción Recomendada** (INMEDIATA):
1. **Rotar credenciales de PostgreSQL** (las actuales están comprometidas)
2. Mover a variables de entorno:
   ```yaml
   datasource:
     url: ${DATABASE_URL}
     username: ${DATABASE_USERNAME}
     password: ${DATABASE_PASSWORD}
   ```
3. Usar Spring Cloud Config con cifrado:
   ```bash
   # Cifrar con config server
   curl http://localhost:8888/encrypt -d "mi-password-secreto"
   ```
4. Añadir `.env` al `.gitignore`
5. Usar HashiCorp Vault o AWS Secrets Manager para producción

**Esfuerzo**: 4 horas
**Prioridad**: P0 (Máxima)

---

### 1.2 Ausencia Total de Seguridad y Autenticación

**Severidad**: 🔴 CRÍTICA
**Impacto**: Todos los endpoints expuestos públicamente sin autenticación

**Hallazgos**:
- ❌ NO existe `spring-boot-starter-security` en ningún `pom.xml`
- ❌ NO hay implementación de JWT o OAuth2
- ❌ NO hay filtros de autenticación en el Gateway
- ❌ NO hay anotaciones `@PreAuthorize` o `@Secured`
- ❌ El Gateway (puerto 8080) enruta todo sin validar tokens

**Impacto**:
- **Acceso no autorizado**: Cualquier usuario puede llamar a cualquier endpoint
- **Sin control de roles**: No hay RBAC (Role-Based Access Control)
- **Sin auditoría**: No se registra quién hace cada operación
- **Violación OWASP A01:2021**: Broken Access Control

**Acción Recomendada** (INMEDIATA):

#### Paso 1: Implementar JWT en Gateway
```xml
<!-- gateway-service/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

#### Paso 2: Crear filtro JWT en Gateway
```java
// gateway-service/src/main/java/com/invoices/gateway_service/security/JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());
        if (token == null || !validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
```

#### Paso 3: Configurar rutas protegidas
```yaml
# gateway-service/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: invoice-service
          uri: lb://invoice-service
          predicates:
            - Path=/api/invoices/**
          filters:
            - JwtAuthentication  # Añadir filtro
```

**Alternativa Recomendada**: Usar **Spring Security OAuth2 Resource Server** + **Keycloak** para autenticación centralizada.

**Esfuerzo**: 2-3 días
**Prioridad**: P0 (Máxima)

---

### 1.3 Base de Datos Compartida (Violación de Patrón)

**Severidad**: 🔴 CRÍTICA (Arquitectura)
**Impacto**: Acoplamiento fuerte entre microservicios, imposibilidad de escalar independientemente

**Hallazgo**:
TODOS los servicios de negocio apuntan a la **misma base de datos**:
```yaml
# user-service, invoice-service, document-service, trace-service
datasource:
  url: jdbc:postgresql://localhost:5432/invoicesdb  # ⚠️ MISMA BD
```

**Violaciones**:
1. **Database per Service Pattern**: Cada microservicio debe tener su BD independiente
2. **Bounded Context (DDD)**: Los contextos no están aislados
3. **Acoplamiento de Datos**: Un cambio en una tabla afecta múltiples servicios
4. **Escalabilidad**: No se puede escalar invoice-service sin escalar user-service

**Impacto**:
- Imposibilidad de escalar servicios independientemente
- Riesgo de corrupción de datos (un servicio modifica datos de otro)
- Pérdida de autonomía de equipos
- Transacciones ACID entre servicios (anti-patrón)
- Imposibilidad de usar diferentes tecnologías de BD por servicio

**Acción Recomendada** (ALTA PRIORIDAD):

#### Diseño de Bases de Datos Separadas:
```yaml
# user-service/application.yml
datasource:
  url: jdbc:postgresql://localhost:5432/userdb

# invoice-service/application.yml
datasource:
  url: jdbc:postgresql://localhost:5432/invoicedb

# document-service/application.yml
datasource:
  url: jdbc:postgresql://localhost:5432/documentdb

# trace-service/application.yml
datasource:
  url: jdbc:postgresql://localhost:5432/tracedb
```

#### Implementar Patrón Saga para Transacciones Distribuidas:
```
Flujo: Crear Factura + Registrar Usuario (si no existe)

1. invoice-service: Crea factura (estado: PENDING)
2. invoice-service: Publica evento "InvoiceCreated" a Kafka
3. user-service: Consume evento, valida usuario
4. user-service: Publica "UserValidated" o "UserNotFound"
5. invoice-service: Consume respuesta
   - Si OK: Cambia factura a CONFIRMED
   - Si ERROR: Compensa (borra factura o marca como FAILED)
```

**Esfuerzo**: 1 semana
**Prioridad**: P0 (Máxima)

---

### 1.4 Config Server Inseguro

**Severidad**: 🔴 CRÍTICA
**Impacto**: Configuraciones sensibles expuestas públicamente en GitHub

**Hallazgo**:
```yaml
# config-server/application.yml
spring:
  cloud:
    config:
      uri: https://github.com/jefmonjor/invoices-back.git  # ⚠️ REPOSITORIO PÚBLICO
```

**Problemas**:
1. Configuraciones en el **mismo repositorio** que el código fuente
2. Repositorio potencialmente **público** en GitHub
3. **Sin autenticación** para acceder al Config Server (puerto 8888)
4. **Sin cifrado** de propiedades sensibles
5. Config Server accesible desde cualquier IP

**Impacto**:
- Credenciales expuestas en GitHub
- Cualquiera puede leer configuraciones en `http://localhost:8888/invoice-service/default`
- Imposibilidad de rotar configuraciones sin modificar código

**Acción Recomendada** (INMEDIATA):

#### Paso 1: Crear Repositorio Privado de Configuraciones
```bash
# Crear repo separado PRIVADO
git init invoices-config
cd invoices-config
mkdir invoice-service user-service document-service trace-service

# invoice-service/application.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/invoicedb
    username: ${DB_USERNAME}
    password: '{cipher}AQA9RN...'  # Cifrado con config server
```

#### Paso 2: Configurar Cifrado en Config Server
```yaml
# config-server/application.yml
encrypt:
  key: ${ENCRYPT_KEY}  # Variable de entorno
```

#### Paso 3: Añadir Autenticación Básica
```xml
<!-- config-server/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```java
// config-server/SecurityConfig.java
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.httpBasic()
            .and()
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }
}
```

#### Paso 4: Actualizar Clientes
```yaml
# invoice-service/application.yml
spring:
  cloud:
    config:
      uri: http://localhost:8888
      username: config-user
      password: ${CONFIG_PASSWORD}
```

**Esfuerzo**: 1 día
**Prioridad**: P0 (Máxima)

---

## 2. PROBLEMAS ALTOS 🟠

Problemas que comprometen seguridad, calidad o mantenibilidad.

### 2.1 Falta Total de Implementación de Lógica de Negocio

**Severidad**: 🟠 ALTA
**Impacto**: El proyecto solo tiene especificaciones OpenAPI, sin código funcional

**Hallazgo**:
Los servicios **SOLO contienen**:
- Clase principal `@SpringBootApplication`
- Archivos de configuración `application.yml`
- Especificaciones OpenAPI (YAML)

**NO EXISTEN**:
- ❌ Controladores REST (`@RestController`)
- ❌ Servicios de negocio (`@Service`)
- ❌ Repositorios JPA (`@Repository`)
- ❌ Entidades de base de datos (`@Entity`)
- ❌ DTOs con validaciones
- ❌ Mappers (ModelMapper, MapStruct)
- ❌ Tests unitarios o de integración

**Impacto**:
- El proyecto no funciona (endpoints devuelven 404)
- Imposible realizar pruebas funcionales
- Sin estimación real de esfuerzo restante

**Acción Recomendada**:

#### Para cada servicio, implementar:

**1. Entidades JPA**:
```java
// user-service/domain/User.java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    private UserRole role;  // ADMIN, USER, CLIENT
}
```

**2. Repositorios**:
```java
// user-service/repository/UserRepository.java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
```

**3. Servicios**:
```java
// user-service/service/UserService.java
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public UserDTO getUserById(UUID id) {
        return userRepository.findById(id)
            .map(UserMapper::toDTO)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

**4. Controladores con Validación**:
```java
// user-service/controller/UserController.java
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userService.createUser(request));
    }
}
```

**Esfuerzo**: 4-6 semanas (para los 4 servicios)
**Prioridad**: P1 (Alta)

---

### 2.2 Sin Validación de Datos

**Severidad**: 🟠 ALTA
**Impacto**: Vulnerabilidad a inyecciones SQL, XSS, datos corruptos

**Hallazgo**:
- NO hay validaciones `@Valid` o `@Validated`
- NO hay DTOs con anotaciones de Bean Validation
- NO hay sanitización de inputs

**Impacto**:
- **SQL Injection**: Si no se validan inputs en queries nativas
- **XSS**: Si se retornan datos sin escapar
- **Datos inconsistentes**: Campos nulos o vacíos en BD
- **Violación OWASP A03:2021**: Injection

**Acción Recomendada**:

```java
// DTOs con validación
public record CreateInvoiceRequest(
    @NotNull(message = "Client ID is required")
    UUID clientId,

    @NotEmpty(message = "Items cannot be empty")
    @Valid
    List<InvoiceItemRequest> items,

    @PastOrPresent(message = "Invoice date cannot be in the future")
    LocalDate invoiceDate,

    @DecimalMin(value = "0.0", inclusive = false, message = "Total must be positive")
    BigDecimal total
) {}

// Controller con validación
@PostMapping
public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
    // Spring validará automáticamente
}
```

**Esfuerzo**: 3 días
**Prioridad**: P1 (Alta)

---

### 2.3 Sin Manejo Global de Errores

**Severidad**: 🟠 ALTA
**Impacto**: Exposición de stacktraces, experiencia de usuario pobre

**Hallazgo**:
- NO hay clases `@ControllerAdvice`
- NO hay jerarquía de excepciones personalizadas
- Errores devuelven stacktraces completos al cliente

**Impacto**:
- **Información sensible expuesta**: Rutas de archivos, versiones, credenciales
- **Experiencia inconsistente**: Cada endpoint devuelve errores en formato diferente
- **Dificultad de debugging**: Sin logs estructurados

**Acción Recomendada**:

```java
// Jerarquía de excepciones
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(UUID id) {
        super(ErrorCode.USER_NOT_FOUND, "User not found: " + id);
    }
}

// Handler global
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                Instant.now()
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));

        return ResponseEntity.badRequest()
            .body(new ValidationErrorResponse(errors));
    }
}
```

**Esfuerzo**: 2 días
**Prioridad**: P1 (Alta)

---

### 2.4 Sin Configuración de CORS

**Severidad**: 🟠 ALTA
**Impacto**: Frontend no podrá consumir APIs o aceptará peticiones de cualquier origen

**Hallazgo**:
- NO hay configuración de CORS en Gateway
- NO hay headers CORS en respuestas

**Impacto**:
- Frontend en dominio diferente no puede hacer llamadas
- O peor: configuración permisiva (`allowedOrigins: "*"`) expone APIs públicamente

**Acción Recomendada**:

```java
// gateway-service/config/CorsConfig.java
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Dominios permitidos (NO usar "*" en producción)
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",      // React dev
            "https://invoices.miempresa.com"  // Producción
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
```

**Esfuerzo**: 2 horas
**Prioridad**: P1 (Alta)

---

### 2.5 JPA Configurado en Modo Peligroso

**Severidad**: 🟠 ALTA
**Impacto**: Riesgo de pérdida de datos en producción

**Hallazgo**:
```yaml
# Todos los servicios
jpa:
  hibernate:
    ddl-auto: update  # ⚠️ PELIGROSO EN PRODUCCIÓN
  show-sql: true      # ⚠️ EXPONE DATOS EN LOGS
```

**Problemas**:
1. **`ddl-auto: update`**: Modifica esquema automáticamente (puede borrar columnas)
2. **`show-sql: true`**: Expone queries con datos sensibles en logs
3. **Sin migraciones versionadas**: No hay control de cambios en BD

**Impacto**:
- Pérdida de datos al hacer refactoring de entidades
- Credenciales o PII expuestas en logs
- Imposibilidad de rollback de esquema

**Acción Recomendada**:

#### Paso 1: Cambiar configuración por ambiente
```yaml
# application.yml (desarrollo)
jpa:
  hibernate:
    ddl-auto: validate  # Solo valida, no modifica
  show-sql: false

# application-prod.yml (producción)
jpa:
  hibernate:
    ddl-auto: validate
  show-sql: false
  properties:
    hibernate:
      format_sql: false
```

#### Paso 2: Implementar Flyway
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

```sql
-- user-service/src/main/resources/db/migration/V1__Create_users_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
```

**Esfuerzo**: 1 semana
**Prioridad**: P1 (Alta)

---

### 2.6 Eureka Dashboard Sin Seguridad

**Severidad**: 🟠 MEDIA
**Impacto**: Exposición de información de infraestructura

**Hallazgo**:
```yaml
# eureka-server/application.yml
server:
  port: 8761  # ⚠️ Dashboard accesible sin autenticación
```

**Impacto**:
- Cualquiera en la red puede ver:
  - IPs de todos los servicios
  - Puertos expuestos
  - Metadatos de servicios
  - Estado de salud
- Información útil para atacantes (reconocimiento)

**Acción Recomendada**:

```xml
<!-- eureka-server/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```java
// eureka-server/SecurityConfig.java
@EnableWebSecurity
public class EurekaSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/eureka/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic()
            .and()
            .csrf().disable();

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("eureka-admin")
            .password(passwordEncoder().encode(System.getenv("EUREKA_PASSWORD")))
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
```

**Esfuerzo**: 4 horas
**Prioridad**: P2 (Media)

---

## 3. ASPECTOS POSITIVOS ✅

El proyecto tiene una **base arquitectónica sólida**. Estos puntos deben mantenerse:

### 3.1 Arquitectura de Microservicios Bien Diseñada
- **Separación clara de responsabilidades**:
  - Config Server: Configuración centralizada
  - Eureka: Service Discovery
  - Gateway: Punto de entrada único
  - 4 servicios de negocio desacoplados

- **Stack moderno**:
  - Spring Boot 3.4.4 (última versión estable)
  - Java 21 (LTS con Virtual Threads)
  - Spring Cloud 2024.0.1

### 3.2 Especificaciones OpenAPI Completas
- **Todos los servicios** tienen especificaciones OpenAPI 3.0.3
- **Swagger UI configurado** (invoice-service)
- **Generación de código** automática con `openapi-generator-maven-plugin`
- **Uso de Java Records** (`useRecords: true`) para inmutabilidad

**Ejemplo de calidad**:
```yaml
# invoice-service/openapi/invoice-api.yaml
paths:
  /api/invoices/{id}:
    get:
      operationId: getInvoiceById
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Invoice found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/InvoiceDTO'
        '404':
          description: Invoice not found
```

### 3.3 Service Discovery con Eureka
- **Balanceo de carga automático** con `lb://service-name`
- **Registro automático** de servicios
- **Alta disponibilidad** (se pueden levantar múltiples instancias)

```yaml
# Gateway enruta usando Eureka
routes:
  - id: invoice-service
    uri: lb://invoice-service  # ✅ Load balancing automático
```

### 3.4 Arquitectura Event-Driven con Kafka
- **Comunicación asíncrona** entre invoice-service y trace-service
- **Desacoplamiento temporal**: Los servicios no necesitan estar disponibles simultáneamente
- **Escalabilidad**: Kafka maneja alto throughput
- **Auditoría**: Registro de eventos de negocio

**Flujo diseñado**:
```
invoice-service → genera factura
                → publica evento "InvoiceGenerated" a Kafka
                                → trace-service consume
                                → registra en BD de auditoría
```

### 3.5 Tecnologías Modernas

**MinIO para Almacenamiento**:
- Alternativa open-source a AWS S3
- Compatible con API de S3
- Ideal para almacenar PDFs de facturas

**JasperReports para PDFs**:
- Librería robusta para generación de reportes
- Soporte para templates complejos
- Versión 7.0.2 (reciente)

**Lombok**:
- Reduce boilerplate en entidades y DTOs
- `@Data`, `@Builder`, `@AllArgsConstructor`

### 3.6 Proyecto Maven Multi-Módulo
- **Gestión centralizada** de dependencias
- **Versionado consistente** (Spring Boot 3.4.4)
- **Build unificado** (`mvn clean install`)

### 3.7 Preparado para Docker
Aunque no existen Dockerfiles aún, la arquitectura es **Cloud Native**:
- Configuración externalizada
- Servicios stateless (RESTful)
- Service discovery dinámico
- Configuración por perfiles (`application-{profile}.yml`)

---

## 4. CHECKLIST DE IMPLEMENTACIÓN POR FASES

### FASE 0: Seguridad Crítica (1-2 semanas) 🔥
**Objetivo**: Resolver bloqueadores que impiden desarrollo seguro

- [ ] **Rotar credenciales de PostgreSQL**
  - [ ] Cambiar passwords en BD
  - [ ] Crear usuarios separados por servicio
  - [ ] Configurar permisos mínimos (principio de least privilege)

- [ ] **Mover credenciales a variables de entorno**
  - [ ] Crear `.env.example` con placeholders
  - [ ] Añadir `.env` a `.gitignore`
  - [ ] Actualizar `application.yml` para usar `${VAR}`
  - [ ] Documentar variables requeridas en README

- [ ] **Implementar autenticación JWT en Gateway**
  - [ ] Añadir dependencias de Spring Security + JWT
  - [ ] Crear `JwtTokenProvider` y `JwtAuthenticationFilter`
  - [ ] Configurar rutas públicas vs protegidas
  - [ ] Añadir endpoint `/auth/login` en user-service

- [ ] **Separar bases de datos por servicio**
  - [ ] Crear BDs: `userdb`, `invoicedb`, `documentdb`, `tracedb`
  - [ ] Migrar esquemas (si existen datos)
  - [ ] Actualizar `application.yml` de cada servicio
  - [ ] Probar conectividad independiente

- [ ] **Asegurar Config Server**
  - [ ] Crear repositorio privado `invoices-config`
  - [ ] Mover configuraciones al nuevo repo
  - [ ] Configurar cifrado de propiedades
  - [ ] Añadir autenticación básica
  - [ ] Actualizar clientes con credenciales

- [ ] **Asegurar Eureka Server**
  - [ ] Añadir Spring Security
  - [ ] Crear credenciales de admin
  - [ ] Actualizar clientes con credenciales

**Entregables**:
- Sistema con autenticación funcional
- Credenciales en variables de entorno
- BDs separadas por servicio
- Config Server privado y cifrado

---

### FASE 1: Base de Desarrollo (3-4 semanas)
**Objetivo**: Implementar estructura base para desarrollo

- [ ] **Configurar perfiles de Spring**
  - [ ] `application-dev.yml` (H2, logs verbosos)
  - [ ] `application-test.yml` (H2, datos de prueba)
  - [ ] `application-prod.yml` (PostgreSQL, logs mínimos)

- [ ] **Implementar Flyway en todos los servicios**
  - [ ] Añadir dependencia `flyway-core`
  - [ ] Crear `db/migration/V1__Initial_schema.sql`
  - [ ] Configurar `ddl-auto: validate`
  - [ ] Probar migraciones en local

- [ ] **Crear entidades JPA para cada servicio**
  - [ ] User Service: `User`, `Client`
  - [ ] Invoice Service: `Invoice`, `InvoiceItem`
  - [ ] Document Service: `Document`
  - [ ] Trace Service: `AuditLog`
  - [ ] Añadir anotaciones: `@Entity`, `@Id`, `@Column`, etc.
  - [ ] Configurar relaciones: `@OneToMany`, `@ManyToOne`

- [ ] **Crear repositorios JPA**
  - [ ] Extender `JpaRepository<Entity, ID>`
  - [ ] Añadir queries personalizadas si es necesario
  - [ ] Probar con tests de integración

- [ ] **Crear DTOs y Mappers**
  - [ ] DTOs con validaciones Bean Validation
  - [ ] Mappers con MapStruct o ModelMapper
  - [ ] Records para inmutabilidad (`record UserDTO(...)`)

- [ ] **Implementar manejo global de excepciones**
  - [ ] Crear jerarquía de excepciones personalizadas
  - [ ] Implementar `@RestControllerAdvice`
  - [ ] Estandarizar respuestas de error
  - [ ] Configurar logging de excepciones

- [ ] **Configurar CORS en Gateway**
  - [ ] Crear `CorsConfig.java`
  - [ ] Definir orígenes permitidos por ambiente
  - [ ] Probar desde frontend local

**Entregables**:
- Esquema de BD versionado con Flyway
- Entidades y repositorios funcionales
- DTOs con validación
- Manejo de errores estandarizado

---

### FASE 2: Lógica de Negocio (4-6 semanas)
**Objetivo**: Implementar funcionalidades core

- [ ] **User Service**
  - [ ] Endpoint: `POST /api/users` (crear usuario)
  - [ ] Endpoint: `GET /api/users/{id}` (obtener usuario)
  - [ ] Endpoint: `PUT /api/users/{id}` (actualizar usuario)
  - [ ] Endpoint: `GET /api/users/clients/{id}` (obtener cliente)
  - [ ] Validación de email único
  - [ ] Hash de passwords (si se maneja autenticación local)

- [ ] **Invoice Service**
  - [ ] Endpoint: `POST /api/invoices` (crear factura)
  - [ ] Endpoint: `GET /api/invoices/{id}` (obtener factura)
  - [ ] Endpoint: `GET /api/invoices?clientId={id}` (listar facturas de cliente)
  - [ ] Endpoint: `POST /api/invoices/generate-pdf` (generar PDF)
  - [ ] Cálculo automático de totales
  - [ ] Integración con JasperReports
  - [ ] Publicación de evento a Kafka al crear factura

- [ ] **Document Service**
  - [ ] Endpoint: `POST /api/documents` (subir PDF a MinIO)
  - [ ] Endpoint: `GET /api/documents/{id}` (descargar PDF)
  - [ ] Endpoint: `DELETE /api/documents/{id}` (eliminar PDF)
  - [ ] Validación de tipo de archivo (solo PDF)
  - [ ] Generación de URLs firmadas (presigned URLs)

- [ ] **Trace Service**
  - [ ] Configurar Kafka Consumer
  - [ ] Escuchar eventos de invoice-service
  - [ ] Registrar auditoría en BD
  - [ ] Endpoint: `GET /api/traces?invoiceId={id}` (listar trazas)
  - [ ] Endpoint: `GET /api/traces?userId={id}` (auditoría por usuario)

- [ ] **Implementar comunicación entre servicios**
  - [ ] Invoice Service llama a User Service (Feign Client)
  - [ ] Invoice Service publica a Kafka
  - [ ] Trace Service consume de Kafka
  - [ ] Document Service es llamado desde Invoice Service

**Entregables**:
- Todas las APIs funcionales según especificaciones OpenAPI
- Flujo completo: Crear usuario → Crear factura → Generar PDF → Auditar

---

### FASE 3: Validación y Testing (2-3 semanas)
**Objetivo**: Garantizar calidad y robustez

- [ ] **Tests Unitarios**
  - [ ] Tests de servicios con Mockito
  - [ ] Tests de repositorios con `@DataJpaTest`
  - [ ] Cobertura mínima: 70%

- [ ] **Tests de Integración**
  - [ ] Tests de controladores con `@WebMvcTest`
  - [ ] Tests end-to-end con `@SpringBootTest`
  - [ ] Testcontainers para PostgreSQL y Kafka

- [ ] **Tests de Contratos (Contract Testing)**
  - [ ] Spring Cloud Contract para APIs
  - [ ] Validar que implementación cumple OpenAPI

- [ ] **Validaciones exhaustivas**
  - [ ] Todos los DTOs con `@Valid`
  - [ ] Reglas de negocio (ej: total de factura > 0)
  - [ ] Validaciones de autorización (usuario solo ve sus facturas)

- [ ] **Logging estructurado**
  - [ ] Configurar Logback con JSON
  - [ ] Añadir `traceId` para trazabilidad
  - [ ] Logs de auditoría (quién hizo qué)

**Entregables**:
- Suite de tests con >70% cobertura
- Validaciones exhaustivas en todos los endpoints
- Logging estructurado para producción

---

### FASE 4: Documentación y DevOps (1-2 semanas)
**Objetivo**: Preparar para deployment

- [ ] **Crear Dockerfiles**
  - [ ] Dockerfile multi-stage para cada servicio
  - [ ] Optimizar tamaño de imágenes (Alpine, JLink)
  - [ ] Probar builds locales

- [ ] **Crear docker-compose.yml**
  - [ ] Servicios: PostgreSQL, Kafka, Zookeeper, MinIO
  - [ ] Redes separadas (frontend, backend, infrastructure)
  - [ ] Volúmenes persistentes
  - [ ] Variables de entorno centralizadas

- [ ] **Documentar README.md**
  - [ ] Descripción del proyecto
  - [ ] Arquitectura (diagrama)
  - [ ] Requisitos (Java 21, Docker, etc.)
  - [ ] Instrucciones de instalación
  - [ ] Variables de entorno requeridas
  - [ ] Comandos para levantar servicios
  - [ ] Endpoints disponibles

- [ ] **Crear Postman Collection**
  - [ ] Colección con todos los endpoints
  - [ ] Variables de entorno ({{baseUrl}}, {{token}})
  - [ ] Scripts de tests automáticos

- [ ] **Configurar Swagger UI agregado**
  - [ ] Swagger UI en Gateway que muestre TODOS los servicios
  - [ ] Documentación centralizada

**Entregables**:
- Proyecto desplegable con `docker-compose up`
- Documentación completa en README
- Postman Collection para pruebas

---

### FASE 5: Producción y Resiliencia (2-3 semanas)
**Objetivo**: Preparar para producción real

- [ ] **Implementar Resilience4j**
  - [ ] Circuit Breakers en llamadas entre servicios
  - [ ] Retry policies para Kafka
  - [ ] Rate limiting en Gateway
  - [ ] Timeouts configurados

- [ ] **Monitoreo y Observabilidad**
  - [ ] Spring Boot Actuator en todos los servicios
  - [ ] Prometheus para métricas
  - [ ] Grafana dashboards
  - [ ] ELK Stack para logs centralizados

- [ ] **Health Checks**
  - [ ] Health checks personalizados (BD, Kafka, MinIO)
  - [ ] Liveness y Readiness probes para Kubernetes

- [ ] **Seguridad adicional**
  - [ ] HTTPS en Gateway (certificados SSL)
  - [ ] Rate limiting por IP
  - [ ] Request validation (tamaño máximo, headers)
  - [ ] Helmet para headers de seguridad

- [ ] **Backups y Disaster Recovery**
  - [ ] Estrategia de backups de PostgreSQL
  - [ ] Replicación de MinIO
  - [ ] Kafka replication factor > 1

- [ ] **CI/CD Pipeline**
  - [ ] GitHub Actions o Jenkins
  - [ ] Tests automáticos en cada PR
  - [ ] Build de imágenes Docker
  - [ ] Deploy automático a staging

**Entregables**:
- Sistema con circuit breakers y retry
- Monitoreo con Prometheus + Grafana
- Pipeline CI/CD funcional

---

## 5. TABLA DE DECISIONES ARQUITECTÓNICAS

| **Aspecto** | **Opciones Evaluadas** | **Recomendación** | **Justificación** | **Impacto** |
|-------------|------------------------|-------------------|-------------------|-------------|
| **Autenticación** | 1. JWT stateless<br>2. OAuth2 + Keycloak<br>3. Spring Session + Redis | **OAuth2 + Keycloak** (si hay SSO)<br>**JWT** (si es simple) | - Keycloak: Gestión centralizada de usuarios, SSO, roles<br>- JWT: Simple, stateless, escalable sin dependencias | **CRÍTICO**<br>- Keycloak: +1 servicio<br>- JWT: Más simple pero menos features |
| **Autorización** | 1. @PreAuthorize en código<br>2. Gateway Level Auth<br>3. Service Mesh (Istio) | **Gateway Level + @PreAuthorize** | - Gateway: Filtro inicial (token válido)<br>- @PreAuthorize: Control granular en servicios | **ALTO**<br>- Defensa en profundidad<br>- Validación en múltiples capas |
| **Bases de Datos** | 1. PostgreSQL separadas<br>2. MongoDB para documents<br>3. BD compartida (actual) | **PostgreSQL separadas** | - Autonomía de servicios<br>- Escalabilidad independiente<br>- Aislamiento de fallos | **CRÍTICO**<br>- 4 instancias PostgreSQL<br>- Mayor complejidad operacional |
| **Comunicación Sync** | 1. Feign Client<br>2. RestTemplate<br>3. WebClient (reactive) | **Feign Client** (no reactivo)<br>**WebClient** (si reactivo) | - Feign: Declarativo, simple, integra con Eureka<br>- WebClient: Mejor rendimiento si hay carga alta | **MEDIO**<br>- Feign: Suficiente para MVP<br>- WebClient: Migrar si escalabilidad es crítica |
| **Eventos Asincrónicos** | 1. Kafka (actual)<br>2. RabbitMQ<br>3. AWS SQS/SNS | **Kafka** | - Alto throughput<br>- Event sourcing nativo<br>- Persistencia de eventos<br>- Ya configurado | **ALTO**<br>- Infraestructura adicional<br>- Zookeeper requerido (o KRaft) |
| **Almacenamiento PDFs** | 1. MinIO (actual)<br>2. AWS S3<br>3. Sistema de archivos | **MinIO** (desarrollo/on-prem)<br>**AWS S3** (cloud) | - MinIO: Compatible S3, open-source, local<br>- S3: Managed, durabilidad 99.999999999% | **MEDIO**<br>- MinIO: Control total, CAPEX<br>- S3: Simplicidad, OPEX |
| **Generación PDFs** | 1. JasperReports (actual)<br>2. iText<br>3. Apache PDFBox | **JasperReports** | - Templates visuales (WYSIWYG)<br>- Soporte para diseños complejos<br>- Maduro y estable | **BAJO**<br>- Curva de aprendizaje media<br>- Licencia comercial si es necesario |
| **Migraciones BD** | 1. Flyway<br>2. Liquibase<br>3. Manual (scripts SQL) | **Flyway** | - Simple, versionado SQL<br>- Integración Spring Boot nativa<br>- Menor curva de aprendizaje que Liquibase | **ALTO**<br>- Control de versiones de BD<br>- Rollback manual pero documentado |
| **Config Management** | 1. Config Server (actual)<br>2. Consul<br>3. Variables de entorno | **Config Server + Variables de entorno** | - Config Server: Centralizado, versionado en Git<br>- Env vars: Secretos y credenciales | **ALTO**<br>- Config Server para configuración<br>- Env vars para secretos |
| **Service Discovery** | 1. Eureka (actual)<br>2. Consul<br>3. Kubernetes Services | **Eureka** (VM/Docker)<br>**K8s Services** (si K8s) | - Eureka: Nativo Spring Cloud, simple<br>- K8s: Nativo si se despliega en Kubernetes | **MEDIO**<br>- Eureka: +1 servicio<br>- K8s: Gratis si ya se usa |
| **API Gateway** | 1. Spring Cloud Gateway (actual)<br>2. Kong<br>3. NGINX + Lua | **Spring Cloud Gateway** | - Nativo Spring Cloud<br>- Filtros en Java<br>- Integra con Eureka | **MEDIO**<br>- Suficiente para <10k req/s |
| **Testing** | 1. JUnit 5 + Mockito<br>2. Testcontainers<br>3. Spock (Groovy) | **JUnit 5 + Mockito + Testcontainers** | - JUnit 5: Estándar Java<br>- Testcontainers: Tests con BD real<br>- Mockito: Mocking simple | **ALTO**<br>- Tests confiables<br>- CI/CD sin mocks frágiles |
| **Logging** | 1. Logback + JSON<br>2. Log4j2<br>3. ELK Stack | **Logback + JSON + ELK** | - Logback: Default Spring Boot<br>- JSON: Parseable por ELK<br>- ELK: Centralización y búsqueda | **ALTO**<br>- Logs estructurados<br>- Debugging rápido |
| **Monitoreo** | 1. Prometheus + Grafana<br>2. Datadog<br>3. New Relic | **Prometheus + Grafana** | - Open-source<br>- Integración nativa con Actuator<br>- Dashboards customizables | **ALTO**<br>- Visibilidad de métricas<br>- Alerting proactivo |
| **Resiliencia** | 1. Resilience4j<br>2. Hystrix (deprecated)<br>3. Sentinel | **Resilience4j** | - Hystrix está deprecado<br>- Resilience4j: Moderno, sin Netflix<br>- Circuit breaker + retry + rate limiter | **CRÍTICO**<br>- Evita cascading failures<br>- SLA más alto |
| **Deployment** | 1. Docker Compose<br>2. Kubernetes<br>3. VM manual | **Docker Compose** (dev/staging)<br>**Kubernetes** (producción) | - Compose: Simple, rápido para desarrollo<br>- K8s: Escalabilidad, auto-healing, producción | **ALTO**<br>- Compose: Suficiente <100 usuarios<br>- K8s: Para escala real |

---

## 6. RUTA CRÍTICA PARA INTEGRACIÓN CON FRONTEND

### Objetivo
Dejar el backend **listo para consumo por frontend** en el menor tiempo posible.

### Ruta Crítica (MVP en 4-6 semanas)

```
SEMANA 1-2: Seguridad + Estructura Base
├─ Implementar JWT en Gateway (5 días)
├─ Separar bases de datos (3 días)
├─ Mover credenciales a .env (1 día)
├─ Configurar CORS (2 horas)
└─ Crear entidades JPA básicas (2 días)

SEMANA 3-4: Funcionalidades Core
├─ User Service
│  ├─ POST /api/users (crear usuario)
│  ├─ GET /api/users/{id}
│  └─ POST /api/auth/login (retorna JWT)
├─ Invoice Service
│  ├─ POST /api/invoices (crear factura)
│  ├─ GET /api/invoices/{id}
│  └─ GET /api/invoices?clientId={id}
└─ Manejo global de errores

SEMANA 5-6: PDFs + Documentación
├─ Invoice Service: POST /api/invoices/generate-pdf
├─ Document Service: GET /api/documents/{id}
├─ Crear docker-compose.yml funcional
├─ Documentar endpoints en README
└─ Crear Postman Collection
```

### Endpoints Mínimos para MVP

#### 1. Autenticación (User Service)
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response 200:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER"
  }
}
```

#### 2. Gestión de Usuarios
```http
POST /api/users
Authorization: Bearer {token}
Content-Type: application/json

{
  "email": "newuser@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "role": "CLIENT"
}

Response 201:
{
  "id": "uuid",
  "email": "newuser@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "role": "CLIENT"
}
```

#### 3. Crear Factura
```http
POST /api/invoices
Authorization: Bearer {token}
Content-Type: application/json

{
  "clientId": "uuid-del-cliente",
  "invoiceDate": "2025-11-13",
  "items": [
    {
      "description": "Producto A",
      "quantity": 2,
      "unitPrice": 100.00
    }
  ]
}

Response 201:
{
  "id": "uuid",
  "invoiceNumber": "INV-2025-001",
  "clientId": "uuid-del-cliente",
  "invoiceDate": "2025-11-13",
  "total": 200.00,
  "status": "PENDING"
}
```

#### 4. Generar PDF
```http
POST /api/invoices/generate-pdf
Authorization: Bearer {token}
Content-Type: application/json

{
  "invoiceId": "uuid-de-la-factura"
}

Response 200:
{
  "documentId": "uuid-del-documento",
  "downloadUrl": "/api/documents/uuid-del-documento",
  "generatedAt": "2025-11-13T10:30:00Z"
}
```

#### 5. Descargar PDF
```http
GET /api/documents/{documentId}
Authorization: Bearer {token}

Response 200:
Content-Type: application/pdf
Content-Disposition: attachment; filename="invoice-INV-2025-001.pdf"

[Binary PDF content]
```

### Prioridades para Frontend

**P0 (Bloqueante)**:
1. Autenticación JWT funcionando
2. CORS configurado correctamente
3. Endpoints documentados con Swagger
4. Manejo de errores estandarizado (JSON)

**P1 (Alta)**:
5. Crear usuario
6. Crear factura
7. Listar facturas de un cliente

**P2 (Media)**:
8. Generar PDF
9. Descargar PDF
10. Auditoría de operaciones

---

## 7. PRIORIDADES INMEDIATAS Y ENTREGABLES DE MVP

### Definición de MVP
**Mínimo Producto Viable**: Sistema de facturación que permite:
- Registrar usuarios y clientes
- Crear facturas
- Generar PDFs de facturas
- Almacenar y descargar PDFs
- Autenticación y autorización básica

### Sprint 0: Seguridad Urgente (1 semana)

**Objetivo**: Sistema no vulnerable a ataques básicos

**Tareas**:
- [ ] Rotar credenciales de PostgreSQL
- [ ] Mover credenciales a `.env`
- [ ] Implementar JWT básico en Gateway
- [ ] Configurar CORS para desarrollo

**Entregable**: Sistema sin credenciales hardcodeadas, con autenticación básica

**Criterios de Aceptación**:
- `git grep -i "password"` no retorna credenciales
- Endpoints protegidos devuelven 401 sin token
- Frontend en localhost:3000 puede hacer llamadas

---

### Sprint 1: Bases de Datos (1 semana)

**Objetivo**: BDs separadas con esquema inicial

**Tareas**:
- [ ] Crear 4 bases de datos PostgreSQL
- [ ] Implementar Flyway en los 4 servicios
- [ ] Crear migración V1 con esquema inicial
- [ ] Crear entidades JPA

**Entregable**: Cada servicio con su BD independiente

**Criterios de Aceptación**:
- Cada servicio arranca y crea sus tablas
- No hay tablas compartidas entre servicios
- Flyway ejecuta migraciones correctamente

---

### Sprint 2: User Service (1 semana)

**Objetivo**: Gestión de usuarios funcional

**Tareas**:
- [ ] Implementar UserRepository
- [ ] Implementar UserService (CRUD)
- [ ] Implementar UserController
- [ ] Endpoint POST /api/auth/login (retorna JWT)
- [ ] Hash de passwords con BCrypt
- [ ] Tests unitarios

**Entregable**: API de usuarios funcional

**Criterios de Aceptación**:
- Puedo crear usuario: `POST /api/users`
- Puedo obtener usuario: `GET /api/users/{id}`
- Puedo hacer login: `POST /api/auth/login`
- Login retorna JWT válido
- Passwords hasheados en BD

---

### Sprint 3: Invoice Service (1 semana)

**Objetivo**: CRUD de facturas

**Tareas**:
- [ ] Implementar Invoice + InvoiceItem entities
- [ ] Implementar InvoiceRepository
- [ ] Implementar InvoiceService
- [ ] Implementar InvoiceController
- [ ] Cálculo automático de totales
- [ ] Validación de cliente existente (llamada a user-service)
- [ ] Tests

**Entregable**: API de facturas funcional

**Criterios de Aceptación**:
- Puedo crear factura con ítems
- Total se calcula automáticamente
- Factura se asocia a cliente válido
- Error 404 si cliente no existe

---

### Sprint 4: Generación de PDFs (1 semana)

**Objetivo**: Generar y almacenar PDFs

**Tareas**:
- [ ] Crear template JasperReports para factura
- [ ] Implementar servicio de generación de PDF
- [ ] Endpoint POST /api/invoices/generate-pdf
- [ ] Integración con document-service (subir PDF a MinIO)
- [ ] MinIO configurado en docker-compose
- [ ] Tests de integración

**Entregable**: PDFs generados y almacenados

**Criterios de Aceptación**:
- Genero PDF desde invoice-service
- PDF se sube a MinIO
- Obtengo URL de descarga
- PDF descargable desde document-service

---

### Sprint 5: Integración y Documentación (1 semana)

**Objetivo**: Sistema integrado end-to-end

**Tareas**:
- [ ] Integrar Kafka entre invoice-service y trace-service
- [ ] Implementar trace-service consumer
- [ ] Crear docker-compose.yml completo
- [ ] Actualizar README con instrucciones
- [ ] Crear Postman Collection
- [ ] Configurar Swagger UI agregado en Gateway
- [ ] Pruebas end-to-end

**Entregable**: Sistema completo desplegable con Docker

**Criterios de Aceptación**:
- `docker-compose up` levanta todos los servicios
- Flujo completo funciona:
  1. Creo usuario
  2. Hago login
  3. Creo factura
  4. Genero PDF
  5. Descargo PDF
  6. Veo auditoría en traces
- Swagger UI muestra todos los endpoints

---

## 8. RECOMENDACIONES FINALES

### 8.1 Lecciones Aprendidas (según tu experiencia)

**TU EXPERIENCIA**:
> "Al principio, desarrollé APIs usando sesiones y roles hardcodeados. Funcionaba… hasta que aparecieron clientes, roles nuevos y microservicios independientes. Migré a JWT stateless y control de roles por endpoint."

**APLICADO A ESTE PROYECTO**:

#### ❌ NO HACER (Anti-patrones)
1. **NO hardcodear roles en código**:
   ```java
   // ❌ MAL
   if (user.getRole().equals("ADMIN")) {
       // lógica
   }
   ```

2. **NO usar sesiones en microservicios**:
   ```yaml
   # ❌ MAL
   spring:
     session:
       store-type: jdbc  # Sesiones con estado
   ```

3. **NO compartir estado entre servicios**:
   ```java
   // ❌ MAL
   @Autowired
   private UserService userService;  // Inyectando servicio de otro microservicio
   ```

#### ✅ HACER (Mejores prácticas)

1. **Autenticación Stateless con JWT**:
   ```java
   // ✅ BIEN
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) {
           http
               .sessionManagement()
               .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // Sin sesiones
               .and()
               .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
           return http.build();
       }
   }
   ```

2. **Control de Roles por Endpoint (RBAC)**:
   ```java
   // ✅ BIEN
   @RestController
   @RequestMapping("/api/invoices")
   public class InvoiceController {

       @GetMapping
       @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
       public List<InvoiceDTO> listInvoices() {
           // Cualquier usuario autenticado
       }

       @DeleteMapping("/{id}")
       @PreAuthorize("hasRole('ADMIN')")
       public void deleteInvoice(@PathVariable UUID id) {
           // Solo administradores
       }

       @GetMapping("/{id}")
       @PreAuthorize("@invoiceSecurityService.canAccessInvoice(#id, authentication)")
       public InvoiceDTO getInvoice(@PathVariable UUID id) {
           // Control granular: solo el dueño o admin
       }
   }
   ```

3. **Roles Dinámicos (Base de Datos)**:
   ```java
   // ✅ BIEN - Roles en BD, no hardcodeados
   @Entity
   public class User {
       @Id
       private UUID id;

       @ManyToMany(fetch = FetchType.EAGER)
       @JoinTable(
           name = "user_roles",
           joinColumns = @JoinColumn(name = "user_id"),
           inverseJoinColumns = @JoinColumn(name = "role_id")
       )
       private Set<Role> roles;  // Roles configurables
   }

   @Entity
   public class Role {
       @Id
       private UUID id;

       @Column(unique = true)
       private String name;  // ADMIN, USER, CLIENT, ACCOUNTANT, etc.

       @ManyToMany
       private Set<Permission> permissions;  // Permisos granulares
   }
   ```

4. **Flexibilidad para Nuevos Roles**:
   ```sql
   -- Añadir nuevo rol sin cambiar código
   INSERT INTO roles (id, name) VALUES (uuid_generate_v4(), 'ACCOUNTANT');

   INSERT INTO permissions (id, resource, action) VALUES
       (uuid_generate_v4(), 'invoices', 'READ'),
       (uuid_generate_v4(), 'invoices', 'EXPORT');

   INSERT INTO role_permissions (role_id, permission_id)
   VALUES (...);  -- Asociar permisos
   ```

---

### 8.2 Arquitectura Stateless vs Stateful

| Aspecto | Stateful (Sesiones) | **Stateless (JWT)** ⭐ |
|---------|---------------------|------------------------|
| **Escalabilidad** | Requiere sticky sessions o sesiones compartidas (Redis) | Horizontal sin límites |
| **Microservicios** | Complejo (compartir sesiones entre servicios) | Natural (cada servicio valida JWT) |
| **Rendimiento** | Latencia de acceso a sesión (BD/Redis) | Validación local (sin I/O) |
| **Logout** | Simple (borrar sesión) | Complejo (blacklist de tokens) |
| **Seguridad** | Session fixation, CSRF | XSS (si token en localStorage) |
| **Recomendación** | Solo para apps monolíticas | **Microservicios siempre** |

**Decisión para este proyecto**: **JWT Stateless** ✅

---

### 8.3 Estrategia de Roles Recomendada

#### Opción 1: Roles Simples (Suficiente para MVP)
```java
enum UserRole {
    ADMIN,      // Acceso total
    USER,       // Usuario estándar (puede crear facturas)
    CLIENT      // Solo puede ver sus propias facturas
}
```

#### Opción 2: RBAC con Permisos (Recomendado para Producción)
```
User → Roles → Permissions

Ejemplo:
  User: john@example.com
    ├─ Role: ACCOUNTANT
    │    ├─ Permission: invoices:read
    │    ├─ Permission: invoices:create
    │    ├─ Permission: invoices:export
    │    └─ Permission: reports:generate
    └─ Role: USER
         └─ Permission: profile:edit
```

**Implementación**:
```java
@PreAuthorize("hasPermission(#invoiceId, 'Invoice', 'DELETE')")
public void deleteInvoice(UUID invoiceId) {
    // Control granular de permisos
}
```

---

### 8.4 Checklist de Seguridad OWASP

Antes de producción, verificar:

- [ ] **A01 - Broken Access Control**
  - [ ] Todos los endpoints protegidos con JWT
  - [ ] Control de acceso a recursos propios (user solo ve sus facturas)
  - [ ] Validación de autorización en cada operación

- [ ] **A02 - Cryptographic Failures**
  - [ ] Passwords hasheados con BCrypt (factor 12+)
  - [ ] JWT firmados con RS256 (clave privada secreta)
  - [ ] HTTPS en producción (TLS 1.3)
  - [ ] Credenciales cifradas en Config Server

- [ ] **A03 - Injection**
  - [ ] Uso de JPA/Hibernate (sin SQL nativo)
  - [ ] Si SQL nativo: PreparedStatements
  - [ ] Validación de inputs con Bean Validation

- [ ] **A04 - Insecure Design**
  - [ ] Rate limiting en Gateway
  - [ ] Timeouts configurados
  - [ ] Circuit breakers implementados

- [ ] **A05 - Security Misconfiguration**
  - [ ] `ddl-auto: validate` en producción
  - [ ] `show-sql: false` en producción
  - [ ] Actuator endpoints protegidos
  - [ ] Eureka con autenticación

- [ ] **A06 - Vulnerable Components**
  - [ ] Dependencias actualizadas (sin CVEs)
  - [ ] OWASP Dependency Check en CI/CD

- [ ] **A07 - Identification and Authentication Failures**
  - [ ] Fuerza de password validada
  - [ ] Account lockout tras intentos fallidos
  - [ ] Tokens con expiración (1h recomendado)

- [ ] **A08 - Software and Data Integrity Failures**
  - [ ] Validación de archivos PDF (tipo MIME, tamaño)
  - [ ] Firma de JWTs verificada

- [ ] **A09 - Security Logging Failures**
  - [ ] Logs de login fallidos
  - [ ] Logs de cambios críticos (crear factura, borrar usuario)
  - [ ] Auditoría en trace-service

- [ ] **A10 - Server-Side Request Forgery**
  - [ ] Validación de URLs en document-service
  - [ ] No permitir URLs arbitrarias para MinIO

---

### 8.5 Métricas de Éxito del Proyecto

**MVP Exitoso Cuando**:
- [ ] Frontend puede autenticarse y recibir JWT
- [ ] Frontend puede crear usuario
- [ ] Frontend puede crear factura con ítems
- [ ] Frontend puede generar y descargar PDF
- [ ] Sistema levanta con `docker-compose up`
- [ ] 0 credenciales hardcodeadas en Git
- [ ] Swagger UI documenta todos los endpoints
- [ ] Tests con >60% cobertura

**Producción-Ready Cuando**:
- [ ] HTTPS configurado
- [ ] BDs separadas por servicio
- [ ] Resilience4j implementado
- [ ] Monitoreo con Prometheus + Grafana
- [ ] Logs centralizados en ELK
- [ ] CI/CD pipeline funcional
- [ ] Tests con >80% cobertura
- [ ] Documentación completa (arquitectura, runbooks)
- [ ] Backups automáticos de BD

---

## 9. RESUMEN DE PRIORIDADES

### Prioridad P0 (CRÍTICO - Semana 1)
1. ✅ Rotar y mover credenciales a `.env`
2. ✅ Implementar JWT básico en Gateway
3. ✅ Separar bases de datos por servicio
4. ✅ Asegurar Config Server

### Prioridad P1 (ALTO - Semanas 2-4)
5. ✅ Implementar user-service (CRUD + login)
6. ✅ Implementar invoice-service (CRUD)
7. ✅ Configurar CORS
8. ✅ Manejo global de errores
9. ✅ Validaciones con Bean Validation

### Prioridad P2 (MEDIO - Semanas 5-6)
10. ✅ Generación de PDFs con JasperReports
11. ✅ Integración con MinIO
12. ✅ Kafka entre invoice y trace service
13. ✅ Docker Compose completo
14. ✅ Documentación y Postman Collection

### Prioridad P3 (BAJO - Post-MVP)
15. Resilience4j (circuit breakers)
16. Prometheus + Grafana
17. ELK Stack
18. CI/CD Pipeline
19. Kubernetes deployment

---

## 10. CONTACTO Y SIGUIENTE PASOS

### Acción Inmediata Recomendada

**PASO 1**: Revisar este documento con el equipo
**PASO 2**: Priorizar según recursos disponibles
**PASO 3**: Comenzar con Sprint 0 (Seguridad Urgente)
**PASO 4**: Crear épicas en JIRA/GitHub Issues basadas en los sprints

### Preguntas Clave a Responder

1. **¿Cuál es el timeline esperado para MVP?**
   - Si es <2 meses: Enfocarse solo en P0 y P1
   - Si es 3-4 meses: Incluir P2

2. **¿Habrá deployment en cloud o on-premise?**
   - Cloud: Considerar AWS S3, RDS, ECS
   - On-premise: Docker Compose suficiente

3. **¿Cuántos usuarios concurrentes esperados?**
   - <100: Configuración actual suficiente
   - >1000: Considerar Kubernetes, autoscaling

4. **¿Existe equipo frontend?**
   - Si: Coordinar especificación de APIs (Swagger)
   - No: Considerar crear frontend simple (React)

---

## CONCLUSIÓN

El proyecto **invoices-back** tiene una **arquitectura sólida** pero requiere **2-3 meses de desarrollo** para alcanzar un MVP funcional y seguro. Los bloqueadores críticos de seguridad deben resolverse **inmediatamente** antes de continuar con funcionalidades.

**Recomendación final**: Seguir la **ruta crítica de 6 semanas** para tener un sistema mínimo funcionando que el frontend pueda consumir, luego iterar con funcionalidades adicionales.

---

**Fecha del Reporte**: 2025-11-13
**Próxima Revisión Recomendada**: Tras completar Sprint 0 (Seguridad)
