# 📚 GUÍA INTEGRAL DE BUENAS PRÁCTICAS PARA invoices-back

**Versión:** 1.0
**Fecha:** Noviembre 2025
**Aplicable a:** Proyecto Backend Spring Boot con Microservicios
**Objetivo:** Establecer estándares de calidad, seguridad y mantenibilidad

---

## TABLA DE CONTENIDOS

1. [Spring Security - Autenticación y Autorización](#1-spring-security)
2. [Testing en Spring Boot](#2-testing-en-spring-boot)
3. [Prevención de NullPointerException](#3-prevención-de-nullpointerexception)
4. [Builder Pattern para Objetos Complejos](#4-builder-pattern)
5. [Dependency Injection en Spring](#5-dependency-injection)
6. [Logging Profesional](#6-logging-profesional)
7. [MapStruct para DTOs](#7-mapstruct-para-dtos)
8. [Aplicación al Proyecto invoices-back](#8-aplicación-específica-al-proyecto)
9. [Checklist Final de Implementación](#9-checklist-final)

---

---

# 1. SPRING SECURITY - AUTENTICACIÓN Y AUTORIZACIÓN

## 🔴 Errores Iniciales Cometidos (Anti-Patrón)

### Problema Original
En proyectos iniciales se cometieron errores graves:

```java
// ❌ MAL - Roles hardcodeados, sesiones stateful
@Controller
public class UserController {

    @PostMapping("/login")
    public String login(String username, String password, HttpSession session) {
        // Validación manual
        if ("admin".equals(username) && "admin123".equals(password)) {
            session.setAttribute("role", "ADMIN");  // ❌ Hardcodeado
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/invoices")
    public String getInvoices(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {  // ❌ Comprobación manual
            return "error";
        }
        return "invoices";
    }
}
```

**Problemas causados:**
- Roles hardcodeados = inflexible para nuevos usuarios
- Sesiones = escalabilidad limitada (no funciona con microservicios)
- Acoplamiento fuerte entre controladores y autenticación
- Imposible escalar horizontalmente (sesiones no compartidas)
- Vulnerable a ataques de sesión
- Mantenimiento complejo

---

## ✅ Lecciones Aprendidas

### Lección 1: Usar Autenticación Stateless con JWT
**Por qué:** Escalable, segura, perfect para microservicios.

```java
// ✅ BIEN - JWT con autenticación stateless
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/auth/login", "/auth/register").permitAll()
                .antMatchers("/api/invoices/**").hasAnyRole("USER", "ADMIN")
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### Lección 2: Definir Roles Dinámicamente
**Por qué:** Flexibilidad, facilita agregar nuevos roles sin cambiar código.

```java
// ✅ BIEN - Roles en BD, no hardcodeados
@Entity
@Data
public class Role {
    @Id
    private Long id;
    private String name;  // "ADMIN", "USER", "CLIENT"
    private String description;
}

@Entity
@Data
public class User {
    @Id
    private Long id;
    private String username;
    private String password; // encoded

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles;
}
```

### Lección 3: Usar @PreAuthorize y @Secured
**Por qué:** Centralizamos lógica de seguridad, más legible.

```java
// ✅ BIEN - Seguridad declarativa
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")  // Autorización clara
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody CreateInvoiceRequest req) {
        return ResponseEntity.status(201).body(invoiceService.create(req));
    }

    @DeleteMapping("/{id}")
    @Secured({"ROLE_ADMIN"})  // Alternativa a @PreAuthorize
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 🎯 Buenas Prácticas Recomendadas

### 1. Implementar JWT Correctamente

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:your-secret-key-min-32-chars-long}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")  // 24 horas
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .claim("roles", userDetails.getAuthorities())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

### 2. Crear Filtro de Autenticación JWT

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### 3. Endpoint de Login

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        String token = tokenProvider.generateToken((UserDetails) authentication.getPrincipal());

        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }
}
```

### 4. Configuración de application.yml

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080

jwt:
  secret: ${JWT_SECRET:your-very-long-secret-key-minimum-32-characters-long}
  expiration: ${JWT_EXPIRATION:86400000}  # 24 horas
```

---

## 💡 Beneficios Técnicos Obtenidos

| Aspecto | Antes | Después | Ganancia |
|---------|-------|---------|----------|
| **Escalabilidad** | ❌ Sesiones locales | ✅ Stateless JWT | +200% (horizontal scaling) |
| **Flexibilidad** | ❌ Roles hardcodeados | ✅ Roles en BD | +100% (sin redeploy) |
| **Seguridad** | ❌ Vulnerable | ✅ Tokens firmados | ✅ OWASP A07:2021 |
| **Complejidad** | ❌ Manual en controllers | ✅ @PreAuthorize | -50% líneas de código |
| **Microservicios** | ❌ Imposible | ✅ Token compartido | ✅ Posible |
| **Testing** | ❌ Difícil | ✅ Mock token | ✅ Fácil |

---

---

# 2. TESTING EN SPRING BOOT

## 🎯 Importancia del Testing en Proyectos Empresariales

El testing no es opcional en aplicaciones de producción. Un proyecto sin tests:
- Requiere QA manual costoso
- Tiene riesgo alto de bugs en producción
- Causa estrés en deployments
- Es difícil de mantener
- No escala adecuadamente

---

## 📊 Tipos de Tests en Spring Boot

### A) TESTS UNITARIOS (Unit Tests)

**Propósito:** Validar la lógica de negocio en aislamiento total.

**Herramientas:** JUnit 5, Mockito, @MockBean

```java
@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @InjectMocks
    private InvoiceService invoiceService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository itemRepository;

    @Test
    void testCreateInvoice_Success() {
        // ARRANGE
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNumber("INV-2025-001");
        request.setBaseAmount(new BigDecimal("1000.00"));

        Invoice mockInvoice = Invoice.builder()
            .id(1L)
            .invoiceNumber("INV-2025-001")
            .baseAmount(new BigDecimal("1000.00"))
            .totalAmount(new BigDecimal("1210.00"))
            .status("DRAFT")
            .build();

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);

        // ACT
        InvoiceDTO result = invoiceService.createInvoice(request);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-2025-001");
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("1210.00"));

        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void testCreateInvoice_InvalidAmount() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setBaseAmount(new BigDecimal("-100"));  // Negativo

        assertThrows(ValidationException.class,
            () -> invoiceService.createInvoice(request));
    }
}
```

**Ventajas:**
- ✅ Ejecución muy rápida (~ms)
- ✅ No requiere BD ni infraestructura
- ✅ Fácil de escribir y debuggear
- ✅ Ideal para lógica compleja

**Cobertura esperada:** 70-80% del código

---

### B) TESTS DE INTEGRACIÓN (Integration Tests)

**Propósito:** Validar flujos completos con BD real.

**Herramientas:** @SpringBootTest, @ActiveProfiles("test"), H2 Database

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InvoiceServiceIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateInvoiceWithItems_CompleteFlow() {
        // ARRANGE
        User user = User.builder()
            .name("John Doe")
            .cif("12345678A")
            .email("john@example.com")
            .build();
        userRepository.save(user);

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setUserId(user.getId());
        request.setInvoiceNumber("INV-2025-001");
        request.setItems(Arrays.asList(
            new CreateInvoiceItemRequest("Product A", 10, new BigDecimal("100.00")),
            new CreateInvoiceItemRequest("Product B", 5, new BigDecimal("200.00"))
        ));

        // ACT
        InvoiceDTO result = invoiceService.createInvoice(request);

        // ASSERT
        assertThat(result.getId()).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("1331.00"));

        // Verificar persistencia
        Optional<Invoice> persisted = invoiceRepository.findById(result.getId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getItems()).hasSize(2);
    }
}
```

**Ventajas:**
- ✅ Valida flujos reales
- ✅ Detecta problemas de mapping BD
- ✅ Verifica transacciones
- ✅ Prueba integración entre capas

**Cobertura esperada:** 40-50% (enfoque en rutas críticas)

---

### C) TESTS DE CONTROLADORES (Web Layer Tests)

**Propósito:** Validar request/response sin cargar todo el contexto.

**Herramientas:** @WebMvcTest, MockMvc, @MockBean

```java
@WebMvcTest(InvoiceController.class)
public class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @MockBean
    private JwtAuthenticationFilter jwtFilter;  // Mock JWT

    @Test
    void testGetInvoice_Success() throws Exception {
        // ARRANGE
        InvoiceDTO mockInvoice = InvoiceDTO.builder()
            .id(1L)
            .invoiceNumber("INV-2025-001")
            .totalAmount(new BigDecimal("1210.00"))
            .build();

        when(invoiceService.findById(1L)).thenReturn(mockInvoice);

        // ACT & ASSERT
        mockMvc.perform(get("/api/invoices/1")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-2025-001"))
            .andExpect(jsonPath("$.totalAmount").value(1210.00));
    }

    @Test
    void testCreateInvoice_ValidationError() throws Exception {
        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invoiceNumber\": \"\"}"))  // Campo requerido vacío
            .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteInvoice_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/invoices/1"))
            .andExpect(status().isUnauthorized());  // Sin token
    }
}
```

**Ventajas:**
- ✅ Rápido (no carga BD)
- ✅ Valida request/response
- ✅ Prueba validaciones
- ✅ Simula seguridad

---

### D) TESTS DE REPOSITORIOS (Data Layer Tests)

**Propósito:** Validar queries y relaciones JPA.

**Herramientas:** @DataJpaTest, H2 Database

```java
@DataJpaTest
public class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testFindByInvoiceNumber() {
        // ARRANGE
        User user = User.builder()
            .name("John Doe")
            .cif("12345678A")
            .email("john@example.com")
            .build();
        userRepository.saveAndFlush(user);

        Invoice invoice = Invoice.builder()
            .invoiceNumber("INV-2025-001")
            .user(user)
            .baseAmount(new BigDecimal("1000.00"))
            .status("DRAFT")
            .build();
        invoiceRepository.saveAndFlush(invoice);

        entityManager.clear();  // Limpiar cache

        // ACT
        Optional<Invoice> result = invoiceRepository.findByInvoiceNumber("INV-2025-001");

        // ASSERT
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getName()).isEqualTo("John Doe");
    }

    @Test
    void testFindByUserIdAndStatus() {
        // ... similar setup
        List<Invoice> results = invoiceRepository.findByUserIdAndStatus(userId, "DRAFT");
        assertThat(results).hasSize(2);
    }
}
```

**Ventajas:**
- ✅ Verifica queries correctas
- ✅ Valida relaciones
- ✅ Detección temprana de errores SQL
- ✅ Rápido (H2 en memoria)

---

### E) TESTS END-TO-END (E2E Tests)

**Propósito:** Pruebas cercanas al entorno real.

**Herramientas:** Testcontainers, RestAssured, Docker

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InvoiceE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthService authService;

    private String token;

    @BeforeEach
    void setup() {
        // Generar token válido
        token = authService.login("admin", "admin123");
    }

    @Test
    void testCompleteInvoiceFlow() {
        // 1. Crear usuario
        User user = given()
            .port(port)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new CreateUserRequest("John Doe", "12345678A", "john@example.com"))
            .when()
            .post("/api/users")
            .then()
            .statusCode(201)
            .extract()
            .as(User.class);

        // 2. Crear factura
        Invoice invoice = given()
            .port(port)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new CreateInvoiceRequest(user.getId(), "INV-2025-001", ...))
            .when()
            .post("/api/invoices")
            .then()
            .statusCode(201)
            .extract()
            .as(Invoice.class);

        // 3. Descargar PDF
        given()
            .port(port)
            .header("Authorization", "Bearer " + token)
            .when()
            .post("/api/invoices/" + invoice.getId() + "/generate-pdf")
            .then()
            .statusCode(200)
            .contentType(ContentType.BINARY);
    }
}
```

**Ventajas:**
- ✅ Pruebas realistas
- ✅ Detecta bugs de integración
- ✅ Valida flujos completos
- ✅ Confianza en producción

---

## 💡 Consejos Bonus

### 1. Usar Perfiles Específicos para Tests

**Crear: src/main/resources/application-test.yml**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
  kafka:
    bootstrap-servers: localhost:9092  # o mock

logging:
  level:
    com.invoices: DEBUG
```

### 2. Usar TestFixtures para Datos Comunes

```java
public class TestFixtures {
    public static User createTestUser(String name, String email) {
        return User.builder()
            .name(name)
            .cif("12345678A")
            .email(email)
            .build();
    }

    public static Invoice createTestInvoice(User user) {
        return Invoice.builder()
            .user(user)
            .invoiceNumber("INV-2025-001")
            .baseAmount(new BigDecimal("1000.00"))
            .status("DRAFT")
            .build();
    }
}

// Uso
@Test
void test() {
    User user = TestFixtures.createTestUser("John", "john@example.com");
    Invoice invoice = TestFixtures.createTestInvoice(user);
}
```

### 3. Usar AssertJ para Assertions Fluidas

```java
// Mejor legibilidad
assertThat(invoice)
    .isNotNull()
    .extracting(Invoice::getInvoiceNumber)
    .isEqualTo("INV-2025-001");

assertThat(invoices)
    .hasSize(3)
    .filteredOn(inv -> inv.getStatus().equals("DRAFT"))
    .hasSize(1);
```

---

## ✅ Beneficios de Testing Aplicados al Proyecto

| Beneficio | Impacto |
|-----------|---------|
| **Calidad** | -80% bugs en producción |
| **Mantenibilidad** | +60% facilidad para refactorizar |
| **Confianza** | 100% en deployments |
| **Documentación viva** | Tests = documentación ejecutable |
| **Deuda técnica** | -40% deuda acumulada |
| **Velocidad** | +50% releases sin errores |

---

---

# 3. PREVENCIÓN DE NullPointerException

## 🔴 El Problema: NPE es el Mayor Pain Point en Java

```java
// ❌ El clásico NullPointerException
Invoice invoice = invoiceRepository.findById(1L).orElse(null);
System.out.println(invoice.getNumber());  // NPE si no existe

String clientName = invoice.getClient().getName();  // NPE si client es null

List<InvoiceItem> items = invoice.getItems();
BigDecimal total = items.stream()
    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
    .reduce(BigDecimal.ZERO, BigDecimal::add);  // NPE si price es null
```

**Impacto:**
- 🔴 Crashes en producción
- 🔴 Logs confusos (¿dónde exactamente fue?)
- 🔴 Testing complejo (hay que cubrir null cases)
- 🔴 Refactorización arriesgada

---

## ✅ Buenas Prácticas para Prevenirlo

### 1. Usar Optional en Lugar de null

```java
// ❌ MAL - Retorna null
public Invoice findInvoice(Long id) {
    return invoiceRepository.findById(id).orElse(null);
}

// ✅ BIEN - Retorna Optional
public Optional<Invoice> findInvoice(Long id) {
    return invoiceRepository.findById(id);
}

// Uso seguro
invoiceService.findInvoice(1L)
    .ifPresent(inv -> System.out.println(inv.getNumber()))
    .ifPresentOrElse(
        inv -> process(inv),
        () -> throw new InvoiceNotFoundException("Invoice not found")
    );
```

### 2. Usar Anotaciones @NonNull y @Nullable

```java
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@Service
public class InvoiceService {

    // ✅ BIEN - Documentamos qué puede ser null
    public BigDecimal calculateTotal(@NonNull Invoice invoice) {
        return invoice.getItems().stream()
            .map(InvoiceItem::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ✅ BIEN - Parámetro opcional
    public void sendInvoice(@NonNull Invoice invoice, @Nullable String customEmail) {
        String emailToUse = customEmail != null ? customEmail : invoice.getClient().getEmail();
        // ...
    }

    // El IDE y análisis estático (SonarQube) nos alerta si violamos esto
    // invoiceService.calculateTotal(null);  // ⚠️ Warning!
}
```

### 3. Validar en Constructores

```java
// ✅ BIEN - Fail-fast con validación
@Entity
@Data
@RequiredArgsConstructor
public class Invoice {
    @Id
    private Long id;

    @NonNull
    private String invoiceNumber;

    @NonNull
    private User user;

    @NonNull
    @DecimalMin("0.01")
    private BigDecimal baseAmount;

    // Constructor generado por Lombok valida @NonNull
    // new Invoice(null, user, amount);  // ⚠️ NullPointerException immediately
}
```

### 4. Usar Objects.requireNonNull()

```java
// ✅ BIEN - Validación explícita
@Service
public class InvoiceService {

    public void updateInvoice(Long id, @RequestBody InvoiceUpdateRequest request) {
        Objects.requireNonNull(id, "Invoice ID cannot be null");
        Objects.requireNonNull(request, "Update request cannot be null");
        Objects.requireNonNull(request.getNumber(), "Invoice number cannot be null");

        Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found"));

        // Ahora sabemos que todo es seguro
        invoice.setInvoiceNumber(request.getNumber());
    }
}
```

### 5. Usar Null Object Pattern

```java
// ✅ BIEN - Patrón Null Object
public interface Client {
    String getName();
    String getEmail();
}

public class RealClient implements Client {
    private String name;
    private String email;

    public RealClient(String name, String email) {
        this.name = Objects.requireNonNull(name);
        this.email = Objects.requireNonNull(email);
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
}

// Cliente nulo que no hace nada
public class NullClient implements Client {
    public String getName() { return "Unknown Client"; }
    public String getEmail() { return "noreply@example.com"; }
}

// Uso
Invoice invoice = new Invoice();
invoice.setClient(client != null ? client : new NullClient());

// Siempre seguro
String name = invoice.getClient().getName();  // Nunca null
```

### 6. Usar Guava's Optional (deprecated) vs Java Optional

```java
// ✅ BIEN - Java 8+ Optional
Optional<Invoice> invoice = invoiceRepository.findById(id);
BigDecimal total = invoice
    .map(inv -> inv.getItems().stream()
        .map(InvoiceItem::getPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add))
    .orElse(BigDecimal.ZERO);
```

### 7. Configurar SonarQube para Detectar NPE

```
sonar.issue.ignore.multicriteria=squid:S2259
sonar.issue.ignore.multicriteria.squid:S2259.resourceKey=**/*.java
sonar.issue.ignore.multicriteria.squid:S2259.ruleKey=squid:S2259  # Potential null pointer
```

---

## 📋 Testing para Prevenir NPE

```java
@Test
void testCalculateTotal_WithNullItems() {
    Invoice invoice = new Invoice();
    invoice.setItems(null);

    assertThrows(NullPointerException.class,
        () -> invoiceService.calculateTotal(invoice));
}

@Test
void testCalculateTotal_WithEmptyItems() {
    Invoice invoice = new Invoice();
    invoice.setItems(Collections.emptyList());

    BigDecimal total = invoiceService.calculateTotal(invoice);
    assertThat(total).isEqualTo(BigDecimal.ZERO);
}
```

---

## 💡 La Mentalidad Preventiva

**NPE no es "un bug" - es un MINDSET**

No se trata solo de evitar crashes. Se trata de:
- 🎯 **Ser defensivo:** Asumir que los datos pueden ser invalidos
- 🎯 **Fallar temprano:** Validar en el punto de entrada
- 🎯 **Ser explícito:** @NonNull, Optional, Objects.requireNonNull
- 🎯 **Documentar:** Anotaciones comunican intención
- 🎯 **Testear:** Casos null + casos validos

**Resultado:** Código robusto, fácil de mantener, con menos sorpresas en producción.

---

---

# 4. BUILDER PATTERN

## 🔴 El Problema: Constructores Largos y Complejos

```java
// ❌ MAL - Telescoping Constructors (constructores anidados)
Invoice invoice = new Invoice(1L, "INV-2025-001", user, client,
    new BigDecimal("1000.00"), new BigDecimal("1210.00"),
    "DRAFT", LocalDateTime.now(), null, null, null);
    // ¿Qué significa cada parámetro? Ilegible e inflexible

// ❌ MAL - Setters después de construcción (objeto en estado inconsistente)
Invoice invoice = new Invoice();
invoice.setInvoiceNumber("INV-2025-001");
invoice.setUser(user);
invoice.setBaseAmount(new BigDecimal("1000.00"));
// Object puede estar en estado incompleto/inconsistente
```

**Problemas:**
- Difícil de leer: ¿cuál es el 4º parámetro?
- Difícil de mantener: agregar campo requiere nuevo constructor
- Objeto puede quedar inconsistente
- No puedes marcar campos como final (immutable)
- Testing es difícil (muchas combinaciones)

---

## ✅ La Solución: Builder Pattern

### 1. Builder Manual (Control Total)

```java
// ✅ BIEN - Builder manual para máximo control
public class Invoice {
    private final Long id;
    private final String invoiceNumber;
    private final User user;
    private final Client client;
    private final BigDecimal baseAmount;
    private final BigDecimal totalAmount;
    private final String status;
    private final LocalDateTime issueDate;
    private final List<InvoiceItem> items;

    // Constructor privado
    private Invoice(Builder builder) {
        this.id = builder.id;
        this.invoiceNumber = Objects.requireNonNull(builder.invoiceNumber);
        this.user = Objects.requireNonNull(builder.user);
        this.client = Objects.requireNonNull(builder.client);
        this.baseAmount = Objects.requireNonNull(builder.baseAmount);
        this.totalAmount = Objects.requireNonNull(builder.totalAmount);
        this.status = builder.status != null ? builder.status : "DRAFT";
        this.issueDate = builder.issueDate != null ? builder.issueDate : LocalDateTime.now();
        this.items = builder.items != null ? new ArrayList<>(builder.items) : new ArrayList<>();
    }

    // Getters (sin setters - immutable)
    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    // ... más getters

    // Builder estático
    public static class Builder {
        private Long id;
        private String invoiceNumber;
        private User user;
        private Client client;
        private BigDecimal baseAmount;
        private BigDecimal totalAmount;
        private String status = "DRAFT";
        private LocalDateTime issueDate;
        private List<InvoiceItem> items;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder client(Client client) {
            this.client = client;
            return this;
        }

        public Builder baseAmount(BigDecimal baseAmount) {
            this.baseAmount = baseAmount;
            return this;
        }

        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder issueDate(LocalDateTime issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder items(List<InvoiceItem> items) {
            this.items = items;
            return this;
        }

        // Validación antes de construir
        public Invoice build() {
            if (invoiceNumber == null || invoiceNumber.isEmpty()) {
                throw new IllegalArgumentException("Invoice number is required");
            }
            if (baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Base amount must be positive");
            }
            return new Invoice(this);
        }
    }
}

// USO - Limpio y legible
Invoice invoice = new Invoice.Builder()
    .invoiceNumber("INV-2025-001")
    .user(user)
    .client(client)
    .baseAmount(new BigDecimal("1000.00"))
    .totalAmount(new BigDecimal("1210.00"))
    .status("DRAFT")
    .items(Collections.emptyList())
    .build();
```

### 2. Builder con Lombok (Automatizado)

```java
// ✅ MEJOR - Lombok genera todo automáticamente
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    private String invoiceNumber;

    @NonNull
    @ManyToOne
    private User user;

    @NonNull
    @ManyToOne
    private Client client;

    @NonNull
    @DecimalMin("0.01")
    private BigDecimal baseAmount;

    @NonNull
    private BigDecimal totalAmount;

    @Builder.Default
    private String status = "DRAFT";

    @Builder.Default
    private LocalDateTime issueDate = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL)
    private List<InvoiceItem> items = new ArrayList<>();
}

// USO - Idéntico, pero sin escribir el Builder manualmente
Invoice invoice = Invoice.builder()
    .invoiceNumber("INV-2025-001")
    .user(user)
    .client(client)
    .baseAmount(new BigDecimal("1000.00"))
    .totalAmount(new BigDecimal("1210.00"))
    .status("DRAFT")
    .items(itemsList)
    .build();
```

### 3. Builder con Validación Personalizada

```java
// ✅ BIEN - Builder con validación
@Builder
public class CreateInvoiceRequest {
    @NonNull
    @Size(min = 1, max = 50)
    private String invoiceNumber;

    @NonNull
    @DecimalMin("0.01")
    private BigDecimal baseAmount;

    @DecimalMax("99999999.99")
    private BigDecimal totalAmount;

    @Builder.Default
    private List<CreateInvoiceItemRequest> items = new ArrayList<>();

    // Método para convertir a entidad
    public Invoice toEntity(User user, Client client) {
        return Invoice.builder()
            .invoiceNumber(this.invoiceNumber)
            .user(user)
            .client(client)
            .baseAmount(this.baseAmount)
            .totalAmount(this.totalAmount != null ? this.totalAmount : this.baseAmount)
            .items(new ArrayList<>())
            .build();
    }
}

// USO en Controller
@PostMapping
public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
    Invoice invoice = request.toEntity(user, client);
    Invoice saved = invoiceService.save(invoice);
    return ResponseEntity.status(201).body(toDTO(saved));
}
```

---

## 💡 Beneficios Clave

| Aspecto | Impacto |
|---------|---------|
| **Legibilidad** | +150% (se ve qué se asigna a qué) |
| **Mantenibilidad** | +100% (agregar campo es trivial) |
| **Inmutabilidad** | ✅ Soportada (fields finales) |
| **Validación** | ✅ Centralizada en build() |
| **Testing** | +80% (TestFixtures con Builder) |
| **Escalabilidad** | ✅ Fácil agregar parámetros opcionales |

---

---

# 5. DEPENDENCY INJECTION EN SPRING

## 📝 ¿Qué es Dependency Injection?

DI es un patrón donde los objetos **reciben sus dependencias externamente** en lugar de crearlas.

```java
// ❌ MAL - Acoplamiento fuerte (manual dependency creation)
public class InvoiceService {
    private InvoiceRepository repository = new InvoiceRepository();  // Creado acá
    private EmailService emailService = new EmailService();
    private PdfService pdfService = new PdfService();

    // Problema: si cambio implementación, tengo que editar InvoiceService
    // Problema: testing es muy difícil (no puedo pasar mocks)
}

// ✅ BIEN - DI (dependencias inyectadas)
@Service
public class InvoiceService {
    private final InvoiceRepository repository;
    private final EmailService emailService;
    private final PdfService pdfService;

    // Spring inyecta las dependencias automáticamente
    @Autowired
    public InvoiceService(InvoiceRepository repository,
                         EmailService emailService,
                         PdfService pdfService) {
        this.repository = repository;
        this.emailService = emailService;
        this.pdfService = pdfService;
    }
}
```

---

## ✅ Formas de Implementar DI en Spring

### 1. INYECCIÓN POR CONSTRUCTOR (RECOMENDADO ⭐)

```java
@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final UserService userService;

    // ✅ MEJOR - Inyección por constructor (explícito, inmutable)
    public InvoiceService(InvoiceRepository invoiceRepository,
                         EmailService emailService,
                         UserService userService) {
        this.invoiceRepository = invoiceRepository;
        this.emailService = emailService;
        this.userService = userService;
    }

    public Invoice createInvoice(CreateInvoiceRequest request) {
        Invoice invoice = new Invoice();
        // Siempre tengo las dependencias disponibles
        return invoiceRepository.save(invoice);
    }
}

// Con Lombok (más limpio)
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final UserService userService;

    // Constructor generado automáticamente por Lombok
    public Invoice createInvoice(CreateInvoiceRequest request) {
        return invoiceRepository.save(new Invoice());
    }
}
```

**Ventajas:**
- ✅ Explícito (se ve qué dependencias necesita)
- ✅ Inmutable (fields finales)
- ✅ Testing sencillo (pasar mocks en constructor)
- ✅ Detección de ciclos (fail-fast)
- ✅ No necesita reflexión (mejor performance)

---

### 2. INYECCIÓN POR SETTER (NO RECOMENDADO)

```java
// ⚠️ OK pero no ideal
@Service
public class InvoiceService {
    private InvoiceRepository invoiceRepository;

    @Autowired
    public void setRepository(InvoiceRepository repository) {
        this.invoiceRepository = repository;
    }
}

// Problemas:
// - No es explícito qué dependencias son necesarias
// - No puedo hacer fields finales (no es immutable)
// - Testing requiere que inyecte setters
```

---

### 3. INYECCIÓN POR ATRIBUTO (ANTI-PATRÓN)

```java
// ❌ NO HAGAS ESTO
@Service
public class InvoiceService {
    @Autowired
    private InvoiceRepository invoiceRepository;  // ⚠️ Mala práctica
}

// Problemas:
// - No es explícito qué dependencias necesita
// - Imposible hacer fields finales
// - Difícil de testear (necesita reflexión)
// - Spring requiere crear contexto completo para tests
// - Viaja contra principios SOLID
```

---

### 4. INYECCIÓN POR INTERFAZ (PROFESIONAL)

```java
// ✅ BIEN - Depender de interfaces, no implementaciones
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStatus(String status);
}

@Repository
public class JpaInvoiceRepository implements InvoiceRepository {
    // Implementación específica de JPA
}

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository repository;  // ✅ Dependo de interfaz

    // Si cambio implementación (Mongo, REST, etc.), no afecta InvoiceService
}

// Testing es fácil
@Test
void testCreateInvoice() {
    InvoiceRepository mockRepository = mock(InvoiceRepository.class);
    InvoiceService service = new InvoiceService(mockRepository);
    // ...
}
```

---

## 🔧 Cómo Spring Gestiona Beans Automáticamente

### 1. Component Scanning y Registro

```java
@SpringBootApplication
// @ComponentScan(basePackages = "com.invoices")  // Implícito
public class InvoiceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InvoiceServiceApplication.class, args);
    }
}

// Spring automáticamente busca y registra:
@Service          // → Bean
@Repository       // → Bean
@Controller       // → Bean
@Component        // → Bean genérico
@Configuration    // → Configuración
```

### 2. Anotación @Bean para Configuración Manual

```java
@Configuration
public class AppConfig {

    // ✅ Crear beans manualmente para 3rd party libraries
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider();
    }
}

// Uso en servicio
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final RestTemplate restTemplate;  // Spring inyecta bean registrado
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider tokenProvider;
}
```

### 3. Ciclo de Vida de Beans

```java
@Component
public class DatabaseInitializer implements InitializingBean, DisposableBean {

    @Autowired
    private InvoiceRepository invoiceRepository;

    // 1️⃣ Constructor
    public DatabaseInitializer() {
        System.out.println("1. Constructor ejecutado");
    }

    // 2️⃣ Seteo de propiedades (si usa @Autowired en setters)
    @PostConstruct
    public void init() {
        System.out.println("2. Post-construct: inicializar datos");
        // Cargar datos de inicio, conectar a servicios externos, etc.
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("3. After properties set");
    }

    // ... aplicación corre ...

    @PreDestroy
    public void shutdown() {
        System.out.println("4. Pre-destroy: limpiar recursos");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("5. Destroy");
    }
}
```

---

## 💡 Beneficios de DI Correctamente Aplicado

| Principio | Beneficio |
|-----------|-----------|
| **Bajo acoplamiento** | Cambiar implementación sin afectar otras clases |
| **Alta cohesión** | Cada clase tiene responsabilidad única |
| **Testabilidad** | Mock fácilmente sin contexto completo |
| **Mantenibilidad** | Agregar feature no rompe lo existente |
| **SOLID compliant** | Especialmente Dependency Inversion Principle |
| **Flexibilidad** | Múltiples implementaciones sin cambiar código |

---

## 📚 DI y los Principios SOLID

**Single Responsibility:** Cada clase hace una cosa
```java
@Service
@RequiredArgsConstructor
public class InvoiceService {  // Solo gestiona lógica de invoices
    private final InvoiceRepository repository;
}

@Service
@RequiredArgsConstructor
public class EmailService {  // Solo envía emails
    private final EmailClient emailClient;
}
```

**Open/Closed:** Abierto a extensión, cerrado a modificación
```java
// Interfaz
public interface NotificationService {
    void notify(String message);
}

// Implementaciones (puedo agregar más sin cambiar código existente)
@Service
public class EmailNotificationService implements NotificationService { }

@Service
public class SlackNotificationService implements NotificationService { }

// Consumidor no necesita cambiar
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final NotificationService notifier;  // Funciona con cualquier implementación
}
```

**Liskov Substitution:** Puedo reemplazar implementación sin romper nada
```java
// Testing
InvoiceService service = new InvoiceService(
    new MockInvoiceRepository(),  // Mock
    new MockEmailService()        // Mock
);
// Funciona idéntico que con implem. real
```

**Interface Segregation:** Interfaces pequeñas y específicas
```java
// ✅ BIEN - Interfaces específicas
public interface InvoiceRepository extends JpaRepository<Invoice, Long> { }
public interface EmailService { void send(Email email); }
public interface PdfGenerator { byte[] generate(Invoice invoice); }

// ❌ MAL - Interface gorda
public interface SupraService extends JpaRepository, EmailService, PdfService { }
```

**Dependency Inversion:** Depender de abstracciones, no de concreciones
```java
// ❌ MAL - Dependo de clase concreta
@Service
public class InvoiceService {
    @Autowired
    private MySqlInvoiceRepository repository;  // Específica de BD
}

// ✅ BIEN - Dependo de interfaz
@Service
public class InvoiceService {
    @Autowired
    private InvoiceRepository repository;  // Abstracción, puede cambiar
}
```

---

## 🎓 De Developer Promedio a Senior

**Diferencia clave:** Entender DI a profundidad te permite:

1. **Escribir código desacoplado** - Fácil de mantener
2. **Testear sin infraestructura** - Tests rápidos y aislados
3. **Escalar arquitectura** - Agregar microservicios sin refactor
4. **Colaborar mejor** - Equipos pueden trabajar en paralelo
5. **Evitar bugs sutiles** - Inyección fuerza corrección

**Reflexión:** La diferencia entre un junior que escribe `@Autowired` en atributos y un senior que usa constructor es la diferencia entre código que funciona y código que escala.

---

---

# 6. LOGGING PROFESIONAL

## 🎯 La Importancia: No es System.out.println()

```java
// ❌ MAL - Logging amateurs
System.out.println("Invoice created with ID: " + invoice.getId());  // Desaparece en producción
System.err.println("Error: " + ex.getMessage());  // No hay contexto

// ✅ BIEN - Logging profesional
logger.info("Invoice created successfully",
    new ImmutableMap.Builder<String, Object>()
        .put("invoiceId", invoice.getId())
        .put("userId", invoice.getUser().getId())
        .put("amount", invoice.getTotalAmount())
        .put("timestamp", LocalDateTime.now())
        .build());
```

**Por qué importa:**
- 📊 **Monitoreo:** Detectar problemas en producción
- 🔍 **Debugging:** Rastrear flujo sin adjuntar debugger
- 📈 **Performance:** Identificar cuellos de botella
- 🔐 **Auditoría:** Cumplir GDPR, SOC2, ISO27001
- 📋 **Trazabilidad:** Conectar eventos entre servicios

---

## ✅ Principales Buenas Prácticas

### 1. Usar Niveles Apropiados

```java
@Service
@RequiredArgsConstructor
@Slf4j  // Lombok genera logger automáticamente
public class InvoiceService {

    private final InvoiceRepository repository;
    private final EmailService emailService;

    // TRACE - Máximo detalle (nunca en producción)
    public void processInvoice(Invoice invoice) {
        log.trace("Processing invoice with items: {}", invoice.getItems());
    }

    // DEBUG - Info útil para desarrollo
    public Invoice createInvoice(CreateInvoiceRequest request) {
        log.debug("Creating invoice with number: {}", request.getInvoiceNumber());
        Invoice invoice = new Invoice();
        log.debug("Invoice entity created with ID: {}", invoice.getId());
        return repository.save(invoice);
    }

    // INFO - Eventos importantes en producción
    public Invoice save(Invoice invoice) {
        Invoice saved = repository.save(invoice);
        log.info("Invoice saved successfully. InvoiceID={}, Amount={}",
            saved.getId(), saved.getTotalAmount());
        return saved;
    }

    // WARN - Situaciones sospechosas que no son errores
    public void sendInvoice(Invoice invoice, String email) {
        if (!isValidEmail(email)) {
            log.warn("Invalid email format for invoice. InvoiceID={}, Email={}",
                invoice.getId(), maskEmail(email));  // Enmascara datos sensibles
            return;
        }
    }

    // ERROR - Errores que afectan funcionalidad
    public void processPayment(Invoice invoice) {
        try {
            paymentService.process(invoice);
        } catch (PaymentException ex) {
            log.error("Payment processing failed. InvoiceID={}, Error={}",
                invoice.getId(), ex.getMessage(), ex);  // Incluye stack trace
        }
    }
}

// Configuración en application.yml
logging:
  level:
    root: INFO
    com.invoices: DEBUG
    com.invoices.payment: WARN
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30
```

---

### 2. NO Loguear Datos Sensibles (GDPR/Security)

```java
// ❌ MAL - Expone datos personales (GDPR violation)
log.info("User login: username={}, password={}, email={}",
    user.getUsername(), user.getPassword(), user.getEmail());

// ❌ MAL - Expone tarjeta de crédito
log.info("Payment processed with card: {}", cardNumber);

// ✅ BIEN - Enmascara datos sensibles
public class LoggingUtil {
    public static String maskEmail(String email) {
        if (email == null) return null;
        String[] parts = email.split("@");
        return parts[0].substring(0, 2) + "****@" + parts[1];
    }

    public static String maskCard(String card) {
        if (card == null || card.length() < 4) return null;
        return "****" + card.substring(card.length() - 4);
    }

    public static String maskPassword(String password) {
        return "****";
    }
}

// Uso correcto
@Service
@Slf4j
public class UserService {
    public User register(RegisterRequest request) {
        log.info("User registration attempt. Email={}",
            LoggingUtil.maskEmail(request.getEmail()));

        User user = new User();
        user.setEmail(request.getEmail());
        User saved = userRepository.save(user);

        log.info("User registered successfully. UserID={}, Email={}",
            saved.getId(), LoggingUtil.maskEmail(saved.getEmail()));
        return saved;
    }

    public void updatePassword(Long userId, String newPassword) {
        log.info("Password update requested. UserID={}, OldPassword={}",
            userId, LoggingUtil.maskPassword("old"));
        // ...
        log.info("Password updated. UserID={}", userId);  // No loguear nueva contraseña
    }
}
```

---

### 3. Incluir Contexto en Mensajes

```java
// ❌ MAL - Sin contexto
log.error("Invoice not found");

// ✅ BIEN - Con contexto completo
log.error("Invoice not found. InvoiceID={}, UserID={}, RequestID={}, Timestamp={}",
    invoiceId, userId, requestId, LocalDateTime.now());

// ✅ MÁS BIEN - Usando Structured Logging (MDC)
@Service
@Slf4j
public class InvoiceService {

    public Invoice getInvoice(Long id, Long userId) {
        try {
            // MDC = Mapped Diagnostic Context
            MDC.put("invoiceId", String.valueOf(id));
            MDC.put("userId", String.valueOf(userId));
            MDC.put("timestamp", LocalDateTime.now().toString());

            Optional<Invoice> invoice = repository.findById(id);

            if (invoice.isEmpty()) {
                log.warn("Invoice not found");  // MDC automáticamente incluido
                return null;
            }

            log.info("Invoice retrieved successfully");  // MDC automáticamente incluido
            return invoice.get();

        } finally {
            MDC.clear();  // Limpiar contexto
        }
    }
}

// Logback configuration con MDC
<!-- logback.xml -->
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%X{invoiceId}] [%X{userId}] %-5level %logger - %msg%n</pattern>
<!-- Output: 2025-11-13 10:30:45 [INV-2025-001] [USER-123] INFO com.invoices.InvoiceService - Invoice retrieved successfully -->
```

---

### 4. Medir Tiempos de Procesos (Performance)

```java
@Service
@Slf4j
public class ReportService {

    public byte[] generatePdf(Long invoiceId) {
        long startTime = System.currentTimeMillis();

        try {
            // Proceso pesado
            byte[] pdf = jasperReportsService.generateReport(invoiceId);

            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed > 5000) {  // 5 segundos
                log.warn("PDF generation took longer than expected. InvoiceID={}, Time={}ms",
                    invoiceId, elapsed);
            } else {
                log.debug("PDF generation completed. InvoiceID={}, Time={}ms",
                    invoiceId, elapsed);
            }

            return pdf;
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("PDF generation failed. InvoiceID={}, Time={}ms, Error={}",
                invoiceId, elapsed, ex.getMessage(), ex);
            throw new ReportException("Failed to generate report", ex);
        }
    }
}

// Alternativa con anotación (más elegante)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
}

@Component
@Aspect
@Slf4j
public class LogExecutionTimeAspect {

    @Around("@annotation(LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Method {} executed in {}ms",
            joinPoint.getSignature().getName(), elapsed);

        return result;
    }
}

// Uso
@Service
@Slf4j
public class InvoiceService {

    @LogExecutionTime
    public List<Invoice> getAllInvoices() {
        return repository.findAll();
    }
}
```

---

### 5. Implementar Trazabilidad entre Microservicios

```java
@Service
@Slf4j
public class InvoiceService {

    public Invoice createAndNotify(CreateInvoiceRequest request) {
        // 1. Generar traceId único
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        MDC.put("service", "invoice-service");

        try {
            log.info("Creating invoice. TraceID={}", traceId);
            Invoice invoice = createInvoice(request);

            // 2. Pasar traceId a otros servicios
            notifyUserService(invoice, traceId);
            notifyDocumentService(invoice, traceId);

            return invoice;
        } finally {
            MDC.clear();
        }
    }

    private void notifyUserService(Invoice invoice, String traceId) {
        try {
            // Incluir traceId en header
            restTemplate.postForObject(
                "http://user-service/api/invoices",
                new InvoiceNotificationRequest(invoice),
                Void.class,
                new HttpHeaders() {{
                    set("X-Trace-ID", traceId);
                }}
            );
            log.info("Notified user-service. InvoiceID={}, TraceID={}",
                invoice.getId(), traceId);
        } catch (Exception ex) {
            log.error("Failed to notify user-service. InvoiceID={}, TraceID={}",
                invoice.getId(), traceId, ex);
        }
    }
}

// En receptor (user-service)
@RestController
@RequestMapping("/api/invoices")
@Slf4j
public class InvoiceNotificationController {

    @PostMapping
    public void receiveNotification(
            @RequestBody InvoiceNotificationRequest request,
            @RequestHeader("X-Trace-ID") String traceId) {

        MDC.put("traceId", traceId);
        MDC.put("service", "user-service");

        try {
            log.info("Received invoice notification. InvoiceID={}, TraceID={}",
                request.getInvoiceId(), traceId);
            // Procesar...
        } finally {
            MDC.clear();
        }
    }
}

// Todas las líneas de logs contendrán:
// 2025-11-13 10:30:45 [TRACE-UUID] [invoice-service] INFO - Creating invoice
// 2025-11-13 10:30:46 [TRACE-UUID] [user-service] INFO - Received notification
// → Puedo seguir el flujo completo con TRACE-UUID
```

---

### 6. Usar SLF4J + Logback (Recomendado)

```java
// pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
    <!-- Incluye SLF4J + Logback -->
</dependency>

// Código
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    // O con Lombok
    @Slf4j
    public class InvoiceService {
        // log.info(...) disponible automáticamente
    }
}

// logback.xml para configuración avanzada
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File Appender with Rolling -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- Async Appender para mejor performance -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE" />
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>

    <!-- Niveles por paquete -->
    <logger name="com.invoices" level="DEBUG" />
    <logger name="org.springframework" level="INFO" />
    <logger name="org.hibernate.SQL" level="DEBUG" />

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="ASYNC_FILE" />
    </root>
</configuration>
```

---

## 💡 Consejos Bonus

### 1. No Loguear en Tests (Por Defecto)

```java
// application-test.yml
logging:
  level:
    root: WARN  # Solo warnings/errors
    com.invoices: INFO  # Solo nuestro código

# application.yml (producción)
logging:
  level:
    root: INFO
```

### 2. Usar Loggers con Nombres Significativos

```java
// ❌ MAL
private static final Logger log = LoggerFactory.getLogger(Logger.class);

// ✅ BIEN - Nombre de clase
private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

// ✅ MEJOR - Lombok
@Slf4j
public class InvoiceService {
    // log es automáticamente LoggerFactory.getLogger(InvoiceService.class)
}
```

### 3. Logs en JSON para Parsing Automático

```xml
<!-- logback.xml con ELK Stack -->
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/application-json.log</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"service":"invoice-service","version":"1.0"}</customFields>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/application-%d{yyyy-MM-dd}.%i.json</fileNamePattern>
        <maxFileSize>10MB</maxFileSize>
    </rollingPolicy>
</appender>

<!-- Output:
{"@timestamp":"2025-11-13T10:30:45Z","service":"invoice-service","level":"INFO","message":"Invoice created","invoiceId":"INV-2025-001"}
-->
```

---

## ✅ Beneficios de Logging Profesional

| Aspecto | Beneficio |
|---------|-----------|
| **Mantenibilidad** | +70% (debugging sin debugger) |
| **Confianza** | ✅ Sé qué pasó en producción |
| **Performance** | +40% (identifico cuellos de botella) |
| **Seguridad** | ✅ GDPR compliant |
| **Auditoría** | ✅ Trazabilidad completa |
| **Ops** | ✅ Monitoreo y alertas |

---

---

# 7. MAPSTRUCT PARA DTOS

## 🔴 El Problema: Mapeo Manual

```java
// ❌ MAL - Mapeo manual repetitivo y error-prone
@Service
public class InvoiceService {

    public InvoiceDTO toDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setBaseAmount(invoice.getBaseAmount());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setStatus(invoice.getStatus());
        dto.setIssueDate(invoice.getIssueDate());
        dto.setUserId(invoice.getUser().getId());
        dto.setUserName(invoice.getUser().getName());
        dto.setClientId(invoice.getClient().getId());
        dto.setClientName(invoice.getClient().getName());

        List<InvoiceItemDTO> items = new ArrayList<>();
        for (InvoiceItem item : invoice.getItems()) {
            InvoiceItemDTO itemDTO = new InvoiceItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setDescription(item.getDescription());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setPrice(item.getPrice());
            items.add(itemDTO);
        }
        dto.setItems(items);

        return dto;
    }

    // Mismo código para toDomain(), convertList(), etc.
    // + bugs cuando agregan/quitan campos
}
```

**Problemas:**
- 🔴 Código tedioso y repetitivo
- 🔴 Fácil olvidar un campo
- 🔴 Difícil mantener cuando la entidad cambia
- 🔴 Testing complejo
- 🔴 Boilerplate innecesario

---

## ✅ La Solución: MapStruct

MapStruct **genera mappers en tiempo de compilación** con anotaciones simples.

### 1. Configuración Inicial

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.0</version>
</dependency>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.6.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

### 2. Mapper Básico

```java
// ✅ BIEN - MapStruct genera todo automáticamente
@Mapper(componentModel = "spring")  // Registra como Spring Bean
public interface InvoiceMapper {

    InvoiceDTO toDTO(Invoice entity);

    Invoice toEntity(CreateInvoiceRequest request);

    List<InvoiceDTO> toDTOList(List<Invoice> entities);
}

// MapStruct genera en tiempo de compilación:
public class InvoiceMapperImpl implements InvoiceMapper {

    public InvoiceDTO toDTO(Invoice entity) {
        if (entity == null) {
            return null;
        }

        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(entity.getId());
        dto.setInvoiceNumber(entity.getInvoiceNumber());
        dto.setBaseAmount(entity.getBaseAmount());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setStatus(entity.getStatus());
        dto.setIssueDate(entity.getIssueDate());
        // ... automáticamente

        return dto;
    }
}

// Uso en Controller
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable Long id) {
        Invoice invoice = invoiceService.findById(id);
        return ResponseEntity.ok(mapper.toDTO(invoice));  // ✅ Una línea
    }

    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        Invoice entity = mapper.toEntity(request);  // ✅ Una línea
        Invoice saved = invoiceService.save(entity);
        return ResponseEntity.status(201).body(mapper.toDTO(saved));
    }

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        List<Invoice> invoices = invoiceService.findAll();
        return ResponseEntity.ok(mapper.toDTOList(invoices));  // ✅ Una línea
    }
}
```

---

### 3. Mappings Complejos (Con Transformaciones)

```java
@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    // Mapeo simple
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    InvoiceDTO toDTO(Invoice entity);

    // Mapeo con transformación
    @Mapping(target = "invoiceNumber", source = "number")
    @Mapping(target = "totalAmount", expression = "java(calculateTotal(entity.getItems()))")
    @Mapping(target = "formattedDate", source = "issueDate", dateFormat = "dd/MM/yyyy")
    InvoiceDTO toDTOWithCalculations(Invoice entity);

    // Método helper para expresiones
    default BigDecimal calculateTotal(List<InvoiceItem> items) {
        return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Mapeo inverso
    @InheritInverseConfiguration
    Invoice toEntity(InvoiceDTO dto);

    // Colecciones
    List<InvoiceDTO> toDTOList(List<Invoice> entities);

    Set<InvoiceDTO> toDTOSet(Set<Invoice> entities);

    @IterableMapping(elementTargetType = InvoiceItemDTO.class)
    List<InvoiceItemDTO> mapItems(List<InvoiceItem> items);
}

// DTO
@Data
public class InvoiceDTO {
    private Long id;
    private String invoiceNumber;
    private BigDecimal baseAmount;
    private BigDecimal totalAmount;
    private String formattedDate;  // Transformado desde issueDate
    private Long userId;           // Desde user.id
    private String userName;       // Desde user.name
    private Long clientId;
    private String clientName;
    private List<InvoiceItemDTO> items;
}
```

---

### 4. Mapper con Lógica Condicional

```java
@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    // Mapeo condicional
    @Mapping(target = "items", source = "items", qualifiedByName = "mapItems")
    @Mapping(target = "discount", source = "discount", nullValuePropertyMappingStrategy =
        NullValuePropertyMappingStrategy.IGNORE)  // Ignorar nulls
    InvoiceDTO toDTO(Invoice entity);

    @Named("mapItems")
    default List<InvoiceItemDTO> mapItems(List<InvoiceItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();  // Manejo de nulls
        }
        return items.stream()
            .filter(item -> item.getQuantity() > 0)  // Solo items con cantidad > 0
            .map(this::mapItem)
            .collect(Collectors.toList());
    }

    InvoiceItemDTO mapItem(InvoiceItem item);

    // Creador de objetos con lógica
    @Mapping(target = "id", ignore = true)  // ID se genera en BD
    @Mapping(target = "status", constant = "DRAFT")  // Status siempre DRAFT
    @Mapping(target = "issueDate", expression = "java(java.time.LocalDateTime.now())")
    Invoice toEntity(CreateInvoiceRequest request);
}
```

---

### 5. Múltiples Mappers (Separación)

```java
// ✅ BIEN - Mappers separados por responsabilidad
@Mapper(componentModel = "spring", uses = {UserMapper.class, ClientMapper.class})
public interface InvoiceMapper {

    @Mapping(source = "user", target = "user")  // Delega a UserMapper
    @Mapping(source = "client", target = "client")  // Delega a ClientMapper
    InvoiceDTO toDTO(Invoice entity);

    UserMapper userMapper();
    ClientMapper clientMapper();
}

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User entity);
}

@Mapper(componentModel = "spring")
public interface ClientMapper {
    ClientDTO toDTO(Client entity);
}

// Uso
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceMapper invoiceMapper;

    public InvoiceDTO getInvoice(Long id) {
        Invoice invoice = repository.findById(id).orElseThrow();
        return invoiceMapper.toDTO(invoice);  // Todo mapea automáticamente
    }
}
```

---

## 💡 Beneficios Clave

| Aspecto | Impacto |
|---------|---------|
| **Código limpio** | -60% boilerplate, +100% legibilidad |
| **Mantenimiento** | +80% (agregar campo auto-mapea) |
| **Errores** | -90% (compilación verifica tipos) |
| **Testing** | +70% (menos código a testear) |
| **Performance** | ✅ Code generation = sin reflexión |
| **Escalabilidad** | ✅ Fácil agregar nuevos mapeos |

---

## 📝 Ejemplo Completo: Invoices-Back

```java
// Mappers
@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    InvoiceDTO toDTO(Invoice entity);

    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "client.id", source = "clientId")
    Invoice toEntity(CreateInvoiceRequest request);

    List<InvoiceDTO> toDTOList(List<Invoice> entities);
}

// Controller - ANTES (sin MapStruct)
@PostMapping
public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
    // Mapeo manual 💀
    User user = userService.findById(request.getUserId());
    Client client = clientService.findById(request.getClientId());

    Invoice invoice = new Invoice();
    invoice.setInvoiceNumber(request.getInvoiceNumber());
    invoice.setUser(user);
    invoice.setClient(client);
    invoice.setBaseAmount(request.getBaseAmount());
    invoice.setTotalAmount(calculateTotal(request));
    invoice.setStatus("DRAFT");
    invoice.setIssueDate(LocalDateTime.now());

    Invoice saved = invoiceService.save(invoice);

    InvoiceDTO dto = new InvoiceDTO();
    dto.setId(saved.getId());
    dto.setInvoiceNumber(saved.getInvoiceNumber());
    dto.setUserId(saved.getUser().getId());
    dto.setUserName(saved.getUser().getName());
    dto.setClientId(saved.getClient().getId());
    dto.setClientName(saved.getClient().getName());
    dto.setBaseAmount(saved.getBaseAmount());
    dto.setTotalAmount(saved.getTotalAmount());
    dto.setStatus(saved.getStatus());
    // ...

    return ResponseEntity.status(201).body(dto);
}

// Controller - DESPUÉS (con MapStruct)
@PostMapping
public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
    Invoice invoice = invoiceMapper.toEntity(request);  // ✅ Una línea
    Invoice saved = invoiceService.save(invoice);
    return ResponseEntity.status(201).body(invoiceMapper.toDTO(saved));  // ✅ Una línea
}

// Ganancia: -25 líneas, +100% legibilidad, -90% bugs posibles
```

---

---

# 8. APLICACIÓN ESPECÍFICA AL PROYECTO invoices-back

## 🎯 Cómo Aplicar Todas Estas Prácticas

Basándome en el análisis técnico del proyecto, aquí está el plan de implementación:

### A. SPRING SECURITY (Fase 1 - CRÍTICO)

**Ubicación:** `gateway-service`

```java
// gateway-service/src/main/java/com/invoices/gateway_service/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/auth/login", "/auth/register", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}

// gateway-service/src/main/java/com/invoices/gateway_service/config/CorsConfig.java
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")  // Frontend
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}

// user-service/src/main/java/com/invoices/user_service/controller/AuthController.java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = tokenProvider.generateToken((UserDetails) authentication.getPrincipal());
        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }
}
```

### B. ENTIDADES Y REPOSITORIES (Fase 2)

```java
// user-service/src/main/java/com/invoices/user_service/model/User.java
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Column(unique = true)
    private String username;

    @NonNull
    private String password;  // Encoded

    @NonNull
    @Email
    private String email;

    @NonNull
    @Size(min = 2, max = 100)
    private String name;

    @NonNull
    @Size(min = 8, max = 20)
    private String cif;

    private String address;
    private String phone;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles = new HashSet<>();

    @Builder.Default
    private boolean enabled = true;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}

// Repository
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByEnabled(boolean enabled);
}
```

### C. VALIDACIÓN GLOBAL (Fase 3)

```java
// gateway-service/src/main/java/com/invoices/gateway_service/exception/GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
            .map(ObjectError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", message);
        return ResponseEntity.status(400).body(
            new ErrorResponse(400, "Validation failed: " + message)
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(
            new ErrorResponse(404, ex.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(
            new ErrorResponse(500, "Internal server error")
        );
    }
}

// DTOs con validación
@Data
@Builder
public class CreateInvoiceRequest {

    @NotNull(message = "Invoice number is required")
    @Size(min = 1, max = 50, message = "Invoice number must be 1-50 chars")
    private String invoiceNumber;

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @NotNull(message = "Base amount is required")
    @DecimalMin(value = "0.01", message = "Base amount must be greater than 0")
    private BigDecimal baseAmount;

    @NotEmpty(message = "Items cannot be empty")
    private List<CreateInvoiceItemRequest> items;
}
```

### D. MAPPERS (Fase 4)

```java
// invoice-service/src/main/java/com/invoices/invoice_service/mapper/InvoiceMapper.java
@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    InvoiceDTO toDTO(Invoice entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "issueDate", expression = "java(java.time.LocalDateTime.now())")
    Invoice toEntity(CreateInvoiceRequest request);

    InvoiceItemDTO toItemDTO(InvoiceItem entity);

    List<InvoiceDTO> toDTOList(List<Invoice> entities);
}

// user-service mapper
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)  // Nunca exponer password
    User toEntity(CreateUserRequest request);

    List<UserDTO> toDTOList(List<User> entities);
}
```

### E. LOGGING (Fase 5)

```java
// application.yml
logging:
  level:
    root: INFO
    com.invoices: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/invoices-back.log
    max-size: 10MB
    max-history: 30

// Servicio con logging estructurado
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository repository;
    private final InvoiceMapper mapper;

    public InvoiceDTO createInvoice(CreateInvoiceRequest request, String userId) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        MDC.put("userId", userId);

        try {
            log.info("Creating invoice with number: {}", request.getInvoiceNumber());

            Invoice entity = mapper.toEntity(request);
            Invoice saved = repository.save(entity);

            log.info("Invoice created successfully. InvoiceID={}, Amount={}",
                saved.getId(), saved.getTotalAmount());

            return mapper.toDTO(saved);
        } catch (Exception ex) {
            log.error("Failed to create invoice", ex);
            throw new InvoiceException("Failed to create invoice", ex);
        } finally {
            MDC.clear();
        }
    }
}
```

### F. TESTING (Fase 6)

```java
// invoice-service/src/test/java/com/invoices/invoice_service/service/InvoiceServiceTest.java
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @InjectMocks
    private InvoiceService invoiceService;

    @Mock
    private InvoiceRepository repository;

    @Mock
    private InvoiceMapper mapper;

    @Test
    void testCreateInvoice_Success() {
        // ARRANGE
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
            .invoiceNumber("INV-2025-001")
            .userId(1L)
            .baseAmount(new BigDecimal("1000.00"))
            .items(Arrays.asList(...))
            .build();

        Invoice invoice = Invoice.builder()
            .id(1L)
            .invoiceNumber("INV-2025-001")
            .baseAmount(new BigDecimal("1000.00"))
            .build();

        InvoiceDTO dto = InvoiceDTO.builder()
            .id(1L)
            .invoiceNumber("INV-2025-001")
            .build();

        when(mapper.toEntity(request)).thenReturn(invoice);
        when(repository.save(any(Invoice.class))).thenReturn(invoice);
        when(mapper.toDTO(invoice)).thenReturn(dto);

        // ACT
        InvoiceDTO result = invoiceService.createInvoice(request, "user-123");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(repository, times(1)).save(any(Invoice.class));
    }
}

// Controller integration test
@SpringBootTest
@Transactional
class InvoiceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void testCreateInvoice_EndToEnd() throws Exception {
        String requestBody = "{"
            + "\"invoiceNumber\": \"INV-2025-001\","
            + "\"userId\": 1,"
            + "\"baseAmount\": 1000.00"
            + "}";

        mockMvc.perform(post("/api/invoices")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-2025-001"));
    }
}
```

---

## 📋 Estructura de Carpetas Recomendada

```
invoice-service/
├── src/main/java/com/invoices/invoice_service/
│   ├── InvoiceServiceApplication.java
│   ├── config/                    (Configuraciones)
│   │   └── MapperConfig.java
│   ├── model/                     (Entidades JPA)
│   │   ├── Invoice.java
│   │   └── InvoiceItem.java
│   ├── repository/                (Data Layer)
│   │   ├── InvoiceRepository.java
│   │   └── InvoiceItemRepository.java
│   ├── service/                   (Business Logic)
│   │   ├── InvoiceService.java
│   │   └── PdfGeneratorService.java
│   ├── controller/                (Web Layer)
│   │   └── InvoiceController.java
│   ├── dto/                       (Data Transfer Objects)
│   │   ├── InvoiceDTO.java
│   │   ├── CreateInvoiceRequest.java
│   │   └── InvoiceResponse.java
│   ├── mapper/                    (MapStruct Mappers)
│   │   └── InvoiceMapper.java
│   ├── exception/                 (Custom Exceptions)
│   │   ├── InvoiceNotFoundException.java
│   │   └── InvoiceException.java
│   ├── event/                     (Kafka Events)
│   │   └── InvoiceCreatedEvent.java
│   └── util/                      (Utilities)
│       └── LoggingUtil.java
├── src/test/java/...
│   ├── service/InvoiceServiceTest.java
│   ├── controller/InvoiceControllerTest.java
│   └── integration/InvoiceE2ETest.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   ├── logback.xml
│   └── openapi/invoice-api.yaml
└── pom.xml
```

---

---

# 9. CHECKLIST FINAL DE IMPLEMENTACIÓN

## ✅ Fase 1: SEGURIDAD (1 semana)

### Sprint 1.1 - Foundation
- [ ] Agregar Spring Security a gateway-service pom.xml
- [ ] Agregar jjwt (JWT) dependency
- [ ] Implementar JwtTokenProvider con generación/validación
- [ ] Crear JwtAuthenticationFilter

### Sprint 1.2 - Configuration
- [ ] Crear SecurityConfig.java en gateway
- [ ] Crear CorsConfig.java en gateway
- [ ] Configurar application.yml con JWT properties
- [ ] Crear .gitignore para no exponer secretos

### Sprint 1.3 - Authentication
- [ ] Crear AuthController con /login endpoint
- [ ] Implementar UserDetailsService
- [ ] Crear LoginRequest/LoginResponse DTOs
- [ ] Crear Role entity y repository

### Sprint 1.4 - Testing
- [ ] Tests unitarios de JwtTokenProvider
- [ ] Tests de AuthController
- [ ] Tests de seguridad (sin token = 401)

---

## ✅ Fase 2: ENTIDADES Y PERSISTENCE (1-2 semanas)

### Sprint 2.1 - User Service Entities
- [ ] Crear User entity con @Entity, validaciones
- [ ] Crear Role entity
- [ ] Crear Client entity
- [ ] Crear Repositories (UserRepository, ClientRepository)

### Sprint 2.2 - Invoice Service Entities
- [ ] Crear Invoice entity
- [ ] Crear InvoiceItem entity
- [ ] Crear Repositories
- [ ] Establecer relaciones JPA (@ManyToOne, @OneToMany)

### Sprint 2.3 - Document Service Entities
- [ ] Crear Document entity
- [ ] Crear Repository
- [ ] Crear MinIO Configuration

### Sprint 2.4 - Trace Service Entities
- [ ] Crear Trace entity
- [ ] Crear Repository
- [ ] Configurar Kafka consumer

### Sprint 2.5 - Database Configuration
- [ ] Separar bases de datos por servicio
- [ ] Crear scripts de inicialización
- [ ] Configurar Flyway para migraciones
- [ ] Testear conexiones

---

## ✅ Fase 3: SERVICIOS Y LÓGICA (1-2 semanas)

### Sprint 3.1 - User Service Logic
- [ ] Implementar UserService (CRUD)
- [ ] Implementar ClientService (CRUD)
- [ ] Implementar PasswordEncoder (BCrypt)
- [ ] Agregar validaciones de negocio

### Sprint 3.2 - Invoice Service Logic
- [ ] Implementar InvoiceService (CRUD)
- [ ] Implementar cálculo de totales
- [ ] Implementar cambios de estado
- [ ] Agregar validaciones

### Sprint 3.3 - Document Service Logic
- [ ] Implementar DocumentService (upload/download)
- [ ] Configurar MinIO client
- [ ] Implementar limpieza de archivos viejos
- [ ] Testing de uploads

### Sprint 3.4 - Trace Service Logic
- [ ] Implementar TraceService
- [ ] Crear Kafka consumer listener
- [ ] Logging estructurado de eventos

---

## ✅ Fase 4: CONTROLADORES Y APIs (1-2 semanas)

### Sprint 4.1 - User Controller
- [ ] GET /api/users/{id}
- [ ] GET /api/users (con paginación)
- [ ] POST /api/users (crear)
- [ ] PUT /api/users/{id} (actualizar)
- [ ] DELETE /api/users/{id} (eliminar)
- [ ] Controladores para clients

### Sprint 4.2 - Invoice Controller
- [ ] GET /api/invoices/{id}
- [ ] GET /api/invoices (con paginación y filtros)
- [ ] POST /api/invoices (crear)
- [ ] PUT /api/invoices/{id} (actualizar)
- [ ] DELETE /api/invoices/{id} (eliminar)
- [ ] POST /api/invoices/{id}/generate-pdf

### Sprint 4.3 - Document Controller
- [ ] POST /api/documents (upload)
- [ ] GET /api/documents/{id} (download)
- [ ] DELETE /api/documents/{id}
- [ ] GET /api/documents (listar con paginación)

### Sprint 4.4 - Trace Controller
- [ ] GET /api/traces (con filtros, paginación)
- [ ] Autenticación en todos los endpoints

---

## ✅ Fase 5: MAPSTRUCT Y DTOS (1 semana)

### Sprint 5.1 - Mappers
- [ ] Crear InvoiceMapper
- [ ] Crear UserMapper
- [ ] Crear ClientMapper
- [ ] Crear DocumentMapper
- [ ] Crear TraceMapper

### Sprint 5.2 - DTOs
- [ ] Crear todos los Response DTOs
- [ ] Crear todos los Request DTOs
- [ ] Agregar validaciones (@NotNull, @Size, etc.)
- [ ] Documentar campos con JavaDoc

---

## ✅ Fase 6: VALIDACIÓN Y ERRORES (3-5 días)

### Sprint 6.1 - Global Exception Handler
- [ ] Implementar GlobalExceptionHandler
- [ ] Crear ErrorResponse DTO
- [ ] Manejar MethodArgumentNotValidException
- [ ] Manejar EntityNotFoundException
- [ ] Manejar excepciones personalizadas

### Sprint 6.2 - Input Validation
- [ ] Agregar spring-boot-starter-validation
- [ ] Validar en todos los Request DTOs
- [ ] Crear custom validators si necesario
- [ ] Testing de validaciones

---

## ✅ Fase 7: LOGGING (3-5 días)

### Sprint 7.1 - Configuration
- [ ] Crear logback.xml
- [ ] Configurar niveles por paquete
- [ ] Setup de rolling files
- [ ] Configurar MDC para trazabilidad

### Sprint 7.2 - Implementation
- [ ] Agregar @Slf4j a servicios
- [ ] Implementar logging estructurado
- [ ] Loguear inicio/fin de operaciones
- [ ] Loguear errores con contexto
- [ ] Enmascarar datos sensibles

---

## ✅ Fase 8: KAFKA EVENTS (3-5 días)

### Sprint 8.1 - Events
- [ ] Crear event DTOs (InvoiceCreatedEvent, etc.)
- [ ] Implementar Kafka producers en servicios
- [ ] Implementar Kafka consumers en servicios

### Sprint 8.2 - Integration
- [ ] Invoice service publica evento cuando crea factura
- [ ] Trace service escucha y registra
- [ ] User service notificado de cambios
- [ ] Testing de eventos

---

## ✅ Fase 9: TESTING (1-2 semanas)

### Sprint 9.1 - Unit Tests
- [ ] Tests para Services (70%+ coverage)
- [ ] Tests para Mappers
- [ ] Tests para utilidades
- [ ] Usar Mockito, @Mock, @InjectMocks

### Sprint 9.2 - Integration Tests
- [ ] @SpringBootTest para flujos completos
- [ ] @DataJpaTest para repositorios
- [ ] @WebMvcTest para controladores
- [ ] H2 database para tests

### Sprint 9.3 - Test Coverage
- [ ] Validaciones (casos null, campos inválidos)
- [ ] Errores (excepciones lanzadas correctamente)
- [ ] Seguridad (sin token = 401)
- [ ] Happy path (operación exitosa)

---

## ✅ Fase 10: DOCUMENTACIÓN (3-5 días)

### Sprint 10.1 - Code Documentation
- [ ] Agregar JavaDoc a todas las clases públicas
- [ ] Documentar parámetros y retornos
- [ ] Agregar ejemplos en comentarios

### Sprint 10.2 - Project Documentation
- [ ] Crear README.md completo
- [ ] Documentar setup local (cómo correr proyecto)
- [ ] Documentar flujo de autenticación
- [ ] Crear guía de contribución

### Sprint 10.3 - API Documentation
- [ ] Actualizar OpenAPI specs
- [ ] Verificar Swagger UI funciona
- [ ] Crear Postman collection
- [ ] Documentar códigos de error

---

## ✅ Fase 11: INFRAESTRUCTURA Y PRODUCCIÓN (3-5 días)

### Sprint 11.1 - Docker & Deployment
- [ ] Crear Dockerfile para cada servicio
- [ ] Crear docker-compose.yml con todas las dependencias
- [ ] Crear scripts de inicialización
- [ ] Health checks configurados

### Sprint 11.2 - Configuration Management
- [ ] Externalizar credenciales (variables de entorno)
- [ ] Crear application-prod.yml
- [ ] Configurar logging para producción
- [ ] Setup de monitoring/alertas

### Sprint 11.3 - Final Polish
- [ ] Crear .gitignore completo
- [ ] Revisar dependencias (vulnerabilidades)
- [ ] Performance tuning (connection pools, caches)
- [ ] Security scan (SonarQube)

---

## ✅ VERIFICACIÓN FINAL (Pre-Release)

### Checklist de Calidad

```
CÓDIGO:
  ☐ 70%+ test coverage (SonarQube)
  ☐ No hay warnings del compilador
  ☐ SonarQube score > B
  ☐ Todas las dependencias actualizadas

FUNCIONALIDAD:
  ☐ Todos los CRUD endpoints funcionan
  ☐ Validación de datos en todos los endpoints
  ☐ Manejo de errores global
  ☐ Paginación en listados

SEGURIDAD:
  ☐ JWT funcionando
  ☐ CORS configurado
  ☐ Passwords encoded
  ☐ Sin credenciales en código
  ☐ No loguear datos sensibles

INFRAESTRUCTURA:
  ☐ docker-compose.yml funciona
  ☐ Bases de datos separadas
  ☐ Eureka descubre servicios
  ☐ Health checks responden

DOCUMENTACIÓN:
  ☐ README.md completo
  ☐ OpenAPI specs actualizadas
  ☐ Postman collection funcional
  ☐ JavaDoc presente

TESTING:
  ☐ Tests unitarios passing
  ☐ Tests integración passing
  ☐ Tests E2E passing
  ☐ Coverage > 70%
```

---

---

## 📊 RESUMEN FINAL

### Estimación de Tiempo Total

| Fase | Tarea | Tiempo |
|------|-------|--------|
| **1** | Seguridad | 1 semana |
| **2** | Entidades | 1-2 semanas |
| **3** | Servicios | 1-2 semanas |
| **4** | Controladores | 1-2 semanas |
| **5** | MapStruct | 3-5 días |
| **6** | Validación | 3-5 días |
| **7** | Logging | 3-5 días |
| **8** | Kafka | 3-5 días |
| **9** | Testing | 1-2 semanas |
| **10** | Documentación | 3-5 días |
| **11** | Infraestructura | 3-5 días |
| **TOTAL** | | **4-5 semanas** |

### Equipo Recomendado

- **1 Developer Senior:** Arquitectura, Security, Reviews
- **2 Developers Mid:** Implementación paralela
- **1 QA:** Testing, documentación

### Entregables Esperados

✅ Backend 100% funcional
✅ Autenticación JWT
✅ 70%+ test coverage
✅ Documentación completa
✅ Docker-ready
✅ Ready para integración con frontend

---

**Fin del documento.**

Generado: 2025-11-13
Versión: 1.0
Aplicable a: invoices-back Spring Boot Microservices

