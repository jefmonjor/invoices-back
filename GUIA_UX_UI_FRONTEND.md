# 🎨 GUÍA UX/UI - Sistema de Gestión de Facturas

**Para:** Equipo de Diseño y Frontend
**Objetivo:** Diseñar la interfaz de usuario consumiendo el backend de microservicios
**Backend URL:** http://localhost:8080 (API Gateway)

---

## 📋 TABLA DE CONTENIDOS

1. [Flujos de Usuario Principales](#-flujos-de-usuario-principales)
2. [Arquitectura de Pantallas](#-arquitectura-de-pantallas)
3. [Endpoints por Pantalla](#-endpoints-por-pantalla)
4. [Estados y Permisos](#-estados-y-permisos)
5. [Componentes Reutilizables](#-componentes-reutilizables)
6. [Wireframes Sugeridos](#-wireframes-sugeridos)
7. [Paleta de Colores y Estilos](#-paleta-de-colores-y-estilos)

---

## 🔄 FLUJOS DE USUARIO PRINCIPALES

### Flujo 1: Autenticación y Acceso

```
┌─────────────┐
│   Landing   │
│    Page     │
└──────┬──────┘
       │
       ▼
┌─────────────┐      ┌──────────────┐
│    Login    │─────▶│   Register   │
│   Screen    │      │    Screen    │
└──────┬──────┘      └──────────────┘
       │
       │ (Login exitoso)
       ▼
┌─────────────┐
│  Dashboard  │ ◀─── Punto de entrada principal
│    Home     │
└─────────────┘
```

**Endpoints:**
- `POST /api/auth/login` → Obtener JWT token
- `POST /api/auth/register` → Crear cuenta nueva
- `GET /api/users/me` → Obtener perfil del usuario logueado

**Datos a guardar:**
```javascript
localStorage.setItem('token', response.token);
localStorage.setItem('user', JSON.stringify(response.user));
localStorage.setItem('expiresAt', Date.now() + response.expiresIn);
```

**Estados:**
- Formulario de login (email, password)
- Cargando (spinner)
- Error (credenciales inválidas)
- Éxito → Redirect a Dashboard

---

### Flujo 2: Gestión de Facturas (Core del Sistema)

```
┌──────────────────────────────────────────────────────────────┐
│                        DASHBOARD                             │
│                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  Total      │  │  Pending    │  │   Paid      │        │
│  │  Invoices   │  │  Invoices   │  │  Invoices   │        │
│  │    150      │  │     45      │  │    105      │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                              │
│  ┌─────────────────────────────────────────────┐            │
│  │         Lista de Facturas Recientes         │            │
│  │  [+] Nueva Factura                          │            │
│  ├─────────────────────────────────────────────┤            │
│  │ #2025-001 | Cliente A | $1,500 | PAID     ●│            │
│  │ #2025-002 | Cliente B | $2,300 | PENDING  ●│            │
│  │ #2025-003 | Cliente C | $850   | DRAFT    ●│            │
│  └─────────────────────────────────────────────┘            │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────┴───────────────────┐
        │                                       │
        ▼                                       ▼
┌─────────────────┐                   ┌─────────────────┐
│  Ver Factura    │                   │  Crear Factura  │
│    Detalle      │                   │     Nueva       │
│                 │                   │                 │
│ - Ver PDF       │                   │ Paso 1: Empresa │
│ - Editar        │                   │ Paso 2: Cliente │
│ - Eliminar      │                   │ Paso 3: Items   │
│ - Cambiar       │                   │ Paso 4: Revisar │
│   Estado        │                   │ Paso 5: Crear   │
│ - Auditoría     │                   └─────────────────┘
└─────────────────┘
```

**Endpoints principales:**
- `GET /api/invoices` → Listar todas las facturas (con filtros, paginación)
- `GET /api/invoices/{id}` → Obtener factura específica
- `POST /api/invoices` → Crear nueva factura
- `PUT /api/invoices/{id}` → Actualizar factura
- `DELETE /api/invoices/{id}` → Eliminar factura
- `POST /api/invoices/{id}/generate-pdf` → Generar y descargar PDF

---

### Flujo 3: Creación de Factura (Wizard/Stepper)

```
┌────────────────────────────────────────────────────────────┐
│              CREAR NUEVA FACTURA - Wizard                  │
└────────────────────────────────────────────────────────────┘

PASO 1: Datos de la Empresa Emisora
┌────────────────────────────────────────────────────────────┐
│ Seleccionar Empresa:                                       │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ ▼ TRANSOLIDO S.L. (CIF: B12345678)                    │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ Nombre: TRANSOLIDO S.L.                                    │
│ CIF/NIF: B12345678                                         │
│ Dirección: Calle Mayor 123, 28001 Madrid                   │
│ Email: facturacion@transolido.com                          │
│                                                            │
│                                    [Cancelar] [Siguiente ▶]│
└────────────────────────────────────────────────────────────┘
                            │
                            ▼
PASO 2: Datos del Cliente
┌────────────────────────────────────────────────────────────┐
│ Seleccionar Cliente:                                       │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ ▼ SERSFRITRUCKS S.A. (CIF: A87654321)                 │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ Nombre: SERSFRITRUCKS S.A.                                 │
│ CIF/NIF: A87654321                                         │
│ Dirección: Avenida Industrial 456, 08001 Barcelona         │
│ Email: contabilidad@sersfritrucks.com                      │
│                                                            │
│                              [◀ Anterior] [Siguiente ▶]    │
└────────────────────────────────────────────────────────────┘
                            │
                            ▼
PASO 3: Items de la Factura
┌────────────────────────────────────────────────────────────┐
│ Número de Factura: 2025-001                    Fecha: [▼]  │
│ Fecha de Vencimiento: [▼] (30 días)                        │
│                                                            │
│ Items de la Factura:                        [+ Añadir Item]│
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Descripción          | Cant | Precio | IVA%  | Total  │ │
│ ├────────────────────────────────────────────────────────┤ │
│ │ Servicio consultoría |  10  | €150   | 21%   | €1,815 │ │
│ │ Desarrollo software  |   5  | €200   | 21%   | €1,210 │ │
│ │ Soporte técnico      |  20h | €50    | 21%   | €1,210 │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ Subtotal:                                        €3,000.00 │
│ IVA (21%):                                         €630.00 │
│ ─────────────────────────────────────────────────────────  │
│ TOTAL:                                           €3,630.00 │
│                                                            │
│                              [◀ Anterior] [Siguiente ▶]    │
└────────────────────────────────────────────────────────────┘
                            │
                            ▼
PASO 4: Revisión y Confirmación
┌────────────────────────────────────────────────────────────┐
│                    RESUMEN DE LA FACTURA                   │
│                                                            │
│ Número: 2025-001               Fecha: 13/11/2025           │
│ Vencimiento: 13/12/2025        Estado: DRAFT               │
│                                                            │
│ De: TRANSOLIDO S.L. (B12345678)                            │
│ Para: SERSFRITRUCKS S.A. (A87654321)                       │
│                                                            │
│ Items: 3                       Subtotal: €3,000.00         │
│                                IVA (21%): €630.00          │
│                                ─────────────────           │
│                                TOTAL: €3,630.00            │
│                                                            │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ ✓ He revisado los datos y son correctos               │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│                       [◀ Anterior] [Crear Factura ✓]      │
└────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────┐
│                    ✓ FACTURA CREADA                        │
│                                                            │
│ La factura #2025-001 ha sido creada exitosamente.         │
│                                                            │
│ ¿Qué deseas hacer?                                         │
│                                                            │
│  [📄 Ver Factura]  [📥 Descargar PDF]  [↩ Volver al Inicio]│
└────────────────────────────────────────────────────────────┘
```

**Endpoints del wizard:**
- `GET /api/companies` → Listar empresas emisoras
- `GET /api/clients` → Listar clientes
- `POST /api/invoices` → Crear factura (enviar todo en un solo request)
- `POST /api/invoices/{id}/generate-pdf` → Generar PDF inmediatamente después de crear

---

### Flujo 4: Detalle de Factura

```
┌──────────────────────────────────────────────────────────────┐
│                  FACTURA #2025-001                      [✕]  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────┐                           Estado: ● PENDING │
│  │  LOGO      │  TRANSOLIDO S.L.                            │
│  │  EMPRESA   │  CIF: B12345678                             │
│  └────────────┘  Calle Mayor 123, 28001 Madrid              │
│                  facturacion@transolido.com                  │
│                                                              │
│  Para:                                                       │
│  SERSFRITRUCKS S.A.                                          │
│  CIF: A87654321                                              │
│  Avenida Industrial 456, 08001 Barcelona                     │
│  contabilidad@sersfritrucks.com                              │
│                                                              │
│  Fecha Emisión: 13/11/2025    Vencimiento: 13/12/2025       │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Descripción            Cant   Precio   IVA    Total  │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ Servicio consultoría   10    €150     21%   €1,815  │   │
│  │ Desarrollo software     5    €200     21%   €1,210  │   │
│  │ Soporte técnico        20h    €50     21%   €1,210  │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│                                    Subtotal:     €3,000.00   │
│                                    IVA (21%):      €630.00   │
│                                    ───────────────────────   │
│                                    TOTAL:        €3,630.00   │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              ACCIONES                                  │ │
│  │                                                        │ │
│  │  [📥 Descargar PDF]  [✏️ Editar]  [🗑️ Eliminar]        │ │
│  │                                                        │ │
│  │  Cambiar Estado:                                       │ │
│  │  ○ DRAFT    ○ PENDING    ○ PAID    ○ CANCELLED        │ │
│  │                                          [Actualizar]  │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              AUDITORÍA Y TRAZABILIDAD                  │ │
│  │                                                        │ │
│  │  📝 13/11/2025 10:30 - Factura creada (admin@...)     │ │
│  │  📝 13/11/2025 11:45 - Estado cambiado a PENDING      │ │
│  │  📝 14/11/2025 09:15 - PDF generado                   │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│                                              [Volver ↩]      │
└──────────────────────────────────────────────────────────────┘
```

**Endpoints:**
- `GET /api/invoices/{id}` → Obtener datos de la factura
- `POST /api/invoices/{id}/generate-pdf` → Generar PDF (descarga automática)
- `PUT /api/invoices/{id}` → Editar factura
- `DELETE /api/invoices/{id}` → Eliminar factura
- `GET /api/traces?invoiceId={id}` → Obtener auditoría de la factura

---

### Flujo 5: Gestión de Usuarios (Solo Admin)

```
┌──────────────────────────────────────────────────────────────┐
│                   GESTIÓN DE USUARIOS                        │
│                                                              │
│  Buscar: [🔍_____________________]        [+ Nuevo Usuario] │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Nombre          Email                 Rol      Estado │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │ Admin User      admin@invoices.com   ADMIN    ● Activo│  │
│  │ John Doe        john@example.com     USER     ● Activo│  │
│  │ Jane Smith      jane@example.com     USER     ○ Inact │  │
│  │                                            [Ver] [✏️] [🗑️]│
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│                                       Página 1 de 5  [< >]   │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼ (Crear Usuario)
┌──────────────────────────────────────────────────────────────┐
│                      NUEVO USUARIO                           │
│                                                              │
│  Nombre:          [___________________________]              │
│  Apellido:        [___________________________]              │
│  Email:           [___________________________]              │
│  Contraseña:      [___________________________]              │
│  Confirmar Pass:  [___________________________]              │
│                                                              │
│  Rol:             ○ ADMIN    ● USER                          │
│                                                              │
│  Estado:          ● Activo   ○ Inactivo                      │
│                                                              │
│                                  [Cancelar] [Crear Usuario]  │
└──────────────────────────────────────────────────────────────┘
```

**Endpoints:**
- `GET /api/users` → Listar usuarios (solo admin)
- `POST /api/users` → Crear usuario (solo admin)
- `PUT /api/users/{id}` → Actualizar usuario (solo admin)
- `DELETE /api/users/{id}` → Eliminar usuario (solo admin)
- `GET /api/users/me` → Ver perfil propio (cualquier usuario)

---

## 🗂️ ARQUITECTURA DE PANTALLAS

### Pantallas Principales (15 pantallas)

```
invoices-frontend/
│
├── Auth/
│   ├── LoginScreen.jsx                  # Login con email/password
│   ├── RegisterScreen.jsx               # Registro de nueva cuenta
│   └── ForgotPasswordScreen.jsx         # Recuperar contraseña (futuro)
│
├── Dashboard/
│   └── DashboardScreen.jsx              # Home con resumen y estadísticas
│
├── Invoices/
│   ├── InvoiceListScreen.jsx            # Lista de facturas con filtros
│   ├── InvoiceDetailScreen.jsx          # Detalle de factura individual
│   ├── InvoiceCreateWizard.jsx          # Wizard de creación (5 pasos)
│   │   ├── Step1CompanySelect.jsx       # Seleccionar empresa
│   │   ├── Step2ClientSelect.jsx        # Seleccionar cliente
│   │   ├── Step3AddItems.jsx            # Agregar items
│   │   ├── Step4Review.jsx              # Revisar datos
│   │   └── Step5Confirmation.jsx        # Confirmación
│   ├── InvoiceEditScreen.jsx            # Editar factura existente
│   └── InvoicePDFViewer.jsx             # Visualizar PDF generado
│
├── Users/ (Solo Admin)
│   ├── UserListScreen.jsx               # Lista de usuarios
│   ├── UserCreateScreen.jsx             # Crear usuario
│   └── UserEditScreen.jsx               # Editar usuario
│
├── Profile/
│   ├── ProfileScreen.jsx                # Ver/editar perfil propio
│   └── ChangePasswordScreen.jsx         # Cambiar contraseña
│
└── Audit/
    └── AuditLogScreen.jsx               # Ver logs de auditoría
```

---

## 🔌 ENDPOINTS POR PANTALLA

### LoginScreen
```javascript
// Endpoint
POST /api/auth/login

// Request
{
  "username": "admin@invoices.com",
  "password": "admin123"
}

// Response
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": 1,
    "email": "admin@invoices.com",
    "firstName": "Admin",
    "lastName": "User",
    "roles": ["ROLE_ADMIN"]
  }
}

// Código ejemplo
const handleLogin = async (email, password) => {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: email, password })
    });

    if (!response.ok) throw new Error('Login failed');

    const data = await response.json();

    // Guardar token
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));

    // Redirect a dashboard
    navigate('/dashboard');
  } catch (error) {
    setError('Credenciales inválidas');
  }
};
```

### DashboardScreen
```javascript
// Endpoints necesarios
GET /api/invoices?page=0&size=5               // Facturas recientes
GET /api/invoices?status=PENDING              // Facturas pendientes
GET /api/invoices?status=PAID                 // Facturas pagadas

// Calcular métricas en frontend
const metrics = {
  totalInvoices: invoices.length,
  pendingInvoices: invoices.filter(i => i.status === 'PENDING').length,
  paidInvoices: invoices.filter(i => i.status === 'PAID').length,
  totalAmount: invoices.reduce((sum, i) => sum + i.totalAmount, 0)
};
```

### InvoiceListScreen
```javascript
// Endpoint con filtros y paginación
GET /api/invoices?page=0&size=20&sortBy=issueDate&sortDir=desc

// Con filtros
GET /api/invoices?status=PENDING&clientId=1&page=0&size=20

// Response
{
  "content": [
    {
      "id": 1,
      "invoiceNumber": "2025-001",
      "companyId": 1,
      "clientId": 1,
      "issueDate": "2025-11-13",
      "dueDate": "2025-12-13",
      "status": "PENDING",
      "subtotal": 3000.00,
      "taxAmount": 630.00,
      "totalAmount": 3630.00,
      "items": [...]
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0
}
```

### InvoiceDetailScreen
```javascript
// Endpoint
GET /api/invoices/1

// Response
{
  "id": 1,
  "invoiceNumber": "2025-001",
  "company": {
    "id": 1,
    "name": "TRANSOLIDO S.L.",
    "taxId": "B12345678",
    "address": "Calle Mayor 123, 28001 Madrid",
    "email": "facturacion@transolido.com"
  },
  "client": {
    "id": 1,
    "name": "SERSFRITRUCKS S.A.",
    "taxId": "A87654321",
    "address": "Avenida Industrial 456, 08001 Barcelona",
    "email": "contabilidad@sersfritrucks.com"
  },
  "issueDate": "2025-11-13",
  "dueDate": "2025-12-13",
  "status": "PENDING",
  "items": [
    {
      "id": 1,
      "description": "Servicio de consultoría",
      "quantity": 10,
      "unitPrice": 150.00,
      "taxRate": 21.0,
      "total": 1815.00
    }
  ],
  "subtotal": 3000.00,
  "taxAmount": 630.00,
  "totalAmount": 3630.00
}

// Generar PDF
POST /api/invoices/1/generate-pdf
Response: application/pdf (blob)

// Auditoría
GET /api/traces?invoiceId=1
Response: [
  {
    "id": 1,
    "eventType": "INVOICE_CREATED",
    "invoiceId": 1,
    "invoiceNumber": "2025-001",
    "createdAt": "2025-11-13T10:30:00",
    "eventData": "{...}"
  }
]
```

### InvoiceCreateWizard
```javascript
// Paso 1: Obtener empresas
GET /api/companies

// Paso 2: Obtener clientes
GET /api/clients

// Paso 5: Crear factura
POST /api/invoices
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

Response: {
  "id": 1,
  "invoiceNumber": "2025-001",
  ...
}

// Generar PDF inmediatamente
POST /api/invoices/1/generate-pdf
```

### UserListScreen (Admin)
```javascript
// Endpoint
GET /api/users?page=0&size=20

// Response
{
  "content": [
    {
      "id": 1,
      "email": "admin@invoices.com",
      "firstName": "Admin",
      "lastName": "User",
      "roles": ["ROLE_ADMIN"],
      "enabled": true
    }
  ],
  "totalElements": 25,
  "totalPages": 2,
  "size": 20,
  "number": 0
}
```

---

## 🔐 ESTADOS Y PERMISOS

### Estados de Factura

```javascript
const INVOICE_STATUS = {
  DRAFT: {
    label: 'Borrador',
    color: 'gray',
    icon: '📝',
    actions: ['edit', 'delete', 'change-status']
  },
  PENDING: {
    label: 'Pendiente',
    color: 'yellow',
    icon: '⏳',
    actions: ['edit', 'generate-pdf', 'change-status']
  },
  PAID: {
    label: 'Pagada',
    color: 'green',
    icon: '✓',
    actions: ['view', 'generate-pdf'] // No se puede editar/eliminar
  },
  CANCELLED: {
    label: 'Cancelada',
    color: 'red',
    icon: '✕',
    actions: ['view'] // Solo lectura
  }
};
```

### Roles y Permisos

```javascript
const PERMISSIONS = {
  ROLE_ADMIN: {
    invoices: ['create', 'read', 'update', 'delete', 'generate-pdf'],
    users: ['create', 'read', 'update', 'delete'],
    audit: ['read'],
    documents: ['upload', 'download', 'delete']
  },
  ROLE_USER: {
    invoices: ['create', 'read', 'update', 'generate-pdf'], // Sin delete
    users: ['read-own'], // Solo su propio perfil
    audit: ['read-own'], // Solo sus propias acciones
    documents: ['upload', 'download'] // Sin delete
  }
};

// Componente de protección de rutas
const ProtectedRoute = ({ children, requiredRole }) => {
  const user = JSON.parse(localStorage.getItem('user'));

  if (!user || !user.roles.includes(requiredRole)) {
    return <Navigate to="/login" />;
  }

  return children;
};

// Uso
<Route path="/users" element={
  <ProtectedRoute requiredRole="ROLE_ADMIN">
    <UserListScreen />
  </ProtectedRoute>
} />
```

---

## 🧩 COMPONENTES REUTILIZABLES

### 1. InvoiceStatusBadge
```jsx
// Componente para mostrar el estado de una factura
<StatusBadge status={invoice.status} />

// Render:
// DRAFT    → [📝 Borrador]     (gris)
// PENDING  → [⏳ Pendiente]    (amarillo)
// PAID     → [✓ Pagada]        (verde)
// CANCELLED → [✕ Cancelada]    (rojo)
```

### 2. InvoiceCard
```jsx
// Card para mostrar factura en lista
<InvoiceCard
  invoice={invoice}
  onView={() => navigate(`/invoices/${invoice.id}`)}
  onEdit={() => navigate(`/invoices/${invoice.id}/edit`)}
  onDelete={() => handleDelete(invoice.id)}
  onDownloadPDF={() => handleDownloadPDF(invoice.id)}
/>
```

### 3. DataTable
```jsx
// Tabla reutilizable con paginación, ordenamiento y filtros
<DataTable
  columns={[
    { field: 'invoiceNumber', header: 'Número' },
    { field: 'clientName', header: 'Cliente' },
    { field: 'totalAmount', header: 'Total', format: 'currency' },
    { field: 'status', header: 'Estado', render: (row) => <StatusBadge status={row.status} /> }
  ]}
  data={invoices}
  totalRecords={totalRecords}
  page={page}
  onPageChange={setPage}
  onSort={handleSort}
  actions={[
    { icon: '👁️', label: 'Ver', onClick: handleView },
    { icon: '✏️', label: 'Editar', onClick: handleEdit },
    { icon: '🗑️', label: 'Eliminar', onClick: handleDelete, confirm: true }
  ]}
/>
```

### 4. WizardStepper
```jsx
// Stepper para proceso de creación de factura
<WizardStepper
  steps={[
    { label: 'Empresa', icon: '🏢' },
    { label: 'Cliente', icon: '👤' },
    { label: 'Items', icon: '📋' },
    { label: 'Revisar', icon: '👁️' },
    { label: 'Confirmar', icon: '✓' }
  ]}
  currentStep={currentStep}
  completedSteps={completedSteps}
/>
```

### 5. AuthGuard (Higher-Order Component)
```jsx
// Protección de componentes que requieren autenticación
export const AuthGuard = ({ children }) => {
  const token = localStorage.getItem('token');
  const expiresAt = localStorage.getItem('expiresAt');

  if (!token || Date.now() > parseInt(expiresAt)) {
    localStorage.clear();
    return <Navigate to="/login" />;
  }

  return children;
};
```

---

## 📐 WIREFRAMES SUGERIDOS

### Dashboard (Responsive)

```
┌─────────────────────────────────────────────────────────────┐
│  ☰ Menu    Invoices App               👤 Admin  🔔  [Logout]│
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📊 DASHBOARD                                               │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   💰 150     │  │   ⏳ 45      │  │   ✓ 105     │     │
│  │  Facturas    │  │  Pendientes  │  │  Pagadas     │     │
│  │  Totales     │  │              │  │              │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                             │
│  ┌──────────────┐  ┌──────────────────────────────────┐   │
│  │ 📈 Gráfica   │  │ 📋 Facturas Recientes            │   │
│  │ de ingresos  │  │                                  │   │
│  │              │  │ [+ Nueva Factura]                │   │
│  │  [Chart]     │  ├──────────────────────────────────┤   │
│  │              │  │ #2025-001 Cliente A  €1,500 ✓   │   │
│  │              │  │ #2025-002 Cliente B  €2,300 ⏳  │   │
│  │              │  │ #2025-003 Cliente C    €850 📝  │   │
│  └──────────────┘  │ #2025-004 Cliente D  €1,200 ✓   │   │
│                    │ #2025-005 Cliente E  €3,500 ⏳  │   │
│                    │                                  │   │
│                    │                [Ver todas →]     │   │
│                    └──────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Lista de Facturas (Desktop)

```
┌─────────────────────────────────────────────────────────────┐
│  ☰ Menu    Facturas                      👤 Admin  [Logout] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📋 FACTURAS                                                │
│                                                             │
│  [+ Nueva Factura]                                          │
│                                                             │
│  Buscar: [🔍_____________]  Estado: [▼ Todos]  Cliente: [▼] │
│  Desde: [📅]  Hasta: [📅]                      [Buscar]     │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ #      │ Cliente        │ Fecha      │ Total  │ Estado│
│  ├─────────────────────────────────────────────────────┤   │
│  │ 2025-001│ Cliente A     │ 13/11/2025 │ €1,500│ ✓     │ │
│  │ 2025-002│ Cliente B     │ 12/11/2025 │ €2,300│ ⏳    │ │
│  │ 2025-003│ Cliente C     │ 11/11/2025 │  €850 │ 📝    │ │
│  │ 2025-004│ Cliente D     │ 10/11/2025 │ €1,200│ ✓     │ │
│  │ 2025-005│ Cliente E     │ 09/11/2025 │ €3,500│ ⏳    │ │
│  │ 2025-006│ Cliente F     │ 08/11/2025 │  €750 │ ✓     │ │
│  │                                     [👁️] [✏️] [🗑️] [📥]  │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│                        Página 1 de 8  [< 1 2 3 ... 8 >]    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Wizard de Creación (Mobile)

```
┌─────────────────────────────┐
│ [←] Nueva Factura           │
├─────────────────────────────┤
│                             │
│ ● ─ ─ ─ ─ ─ ─ ─ ─          │
│ Empresa  Cliente  Items...  │
│                             │
│ PASO 1: Selecciona Empresa  │
│                             │
│ ┌─────────────────────────┐ │
│ │ ▼ TRANSOLIDO S.L.      │ │
│ └─────────────────────────┘ │
│                             │
│ CIF: B12345678              │
│ Dirección:                  │
│ Calle Mayor 123             │
│ 28001 Madrid                │
│                             │
│ Email:                      │
│ facturacion@transolido.com  │
│                             │
│                             │
│ ┌─────────────────────────┐ │
│ │   Siguiente ▶           │ │
│ └─────────────────────────┘ │
│                             │
│ ┌─────────────────────────┐ │
│ │   Cancelar              │ │
│ └─────────────────────────┘ │
│                             │
└─────────────────────────────┘
```

---

## 🎨 PALETA DE COLORES Y ESTILOS

### Colores Principales

```css
/* Paleta de colores profesional para sistema de facturas */

/* Primarios */
--primary-50: #eff6ff;
--primary-100: #dbeafe;
--primary-500: #3b82f6;  /* Azul principal */
--primary-600: #2563eb;
--primary-700: #1d4ed8;

/* Estados */
--success-50: #f0fdf4;
--success-500: #22c55e;  /* Verde - PAID */
--warning-50: #fffbeb;
--warning-500: #f59e0b;  /* Amarillo - PENDING */
--danger-50: #fef2f2;
--danger-500: #ef4444;   /* Rojo - CANCELLED */
--info-50: #f5f5f5;
--info-500: #6b7280;     /* Gris - DRAFT */

/* Neutrales */
--gray-50: #f9fafb;
--gray-100: #f3f4f6;
--gray-200: #e5e7eb;
--gray-500: #6b7280;
--gray-900: #111827;

/* Texto */
--text-primary: #111827;
--text-secondary: #6b7280;
--text-muted: #9ca3af;
```

### Tipografía

```css
/* Fuentes recomendadas */
font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;

/* Tamaños */
--text-xs: 0.75rem;   /* 12px */
--text-sm: 0.875rem;  /* 14px */
--text-base: 1rem;    /* 16px */
--text-lg: 1.125rem;  /* 18px */
--text-xl: 1.25rem;   /* 20px */
--text-2xl: 1.5rem;   /* 24px */
--text-3xl: 1.875rem; /* 30px */
--text-4xl: 2.25rem;  /* 36px */

/* Pesos */
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;
```

### Espaciado y Layout

```css
/* Espaciado */
--spacing-1: 0.25rem;  /* 4px */
--spacing-2: 0.5rem;   /* 8px */
--spacing-3: 0.75rem;  /* 12px */
--spacing-4: 1rem;     /* 16px */
--spacing-6: 1.5rem;   /* 24px */
--spacing-8: 2rem;     /* 32px */
--spacing-12: 3rem;    /* 48px */

/* Bordes */
--radius-sm: 0.25rem;  /* 4px */
--radius-md: 0.5rem;   /* 8px */
--radius-lg: 0.75rem;  /* 12px */
--radius-xl: 1rem;     /* 16px */

/* Sombras */
--shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
--shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
--shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
--shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
```

### Componentes Base

```css
/* Botones */
.btn-primary {
  background: var(--primary-500);
  color: white;
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-md);
  font-weight: var(--font-medium);
  transition: all 0.2s;
}

.btn-primary:hover {
  background: var(--primary-600);
  box-shadow: var(--shadow-md);
}

/* Cards */
.card {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--gray-200);
}

/* Status Badges */
.badge-paid {
  background: var(--success-50);
  color: var(--success-500);
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.badge-pending {
  background: var(--warning-50);
  color: var(--warning-500);
}

.badge-draft {
  background: var(--info-50);
  color: var(--info-500);
}

.badge-cancelled {
  background: var(--danger-50);
  color: var(--danger-500);
}
```

---

## 🔗 FLUJO DE DATOS COMPLETO

```
┌─────────────────────────────────────────────────────────────┐
│                         FRONTEND                            │
│                                                             │
│  ┌─────────────┐  Login  ┌─────────────────────────────┐  │
│  │   Login     │────────▶│  LocalStorage               │  │
│  │   Screen    │         │  - token                     │  │
│  └─────────────┘         │  - user                      │  │
│                          │  - expiresAt                 │  │
│                          └──────────┬───────────────────┘  │
│                                     │                       │
│                                     ▼                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              API Client (Axios/Fetch)                │  │
│  │                                                      │  │
│  │  - Interceptor: Agregar token a headers             │  │
│  │  - Interceptor: Manejar 401 (redirect a login)      │  │
│  │  - Interceptor: Manejar errores globales            │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
└─────────────────────────┼───────────────────────────────────┘
                          │
                          │ HTTP Request
                          │ Authorization: Bearer <token>
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    API GATEWAY (8080)                       │
│                                                             │
│  1. Validar JWT token                                       │
│  2. Verificar CORS                                          │
│  3. Enrutar a microservicio correcto                        │
│                                                             │
└────────┬────────────┬────────────┬────────────┬─────────────┘
         │            │            │            │
         ▼            ▼            ▼            ▼
    ┌────────┐  ┌─────────┐  ┌─────────┐  ┌────────┐
    │ User   │  │ Invoice │  │Document │  │ Trace  │
    │Service │  │ Service │  │ Service │  │Service │
    │ (8082) │  │ (8081)  │  │ (8083)  │  │ (8084) │
    └────┬───┘  └────┬────┘  └────┬────┘  └───┬────┘
         │           │            │           │
         ▼           ▼            ▼           ▼
    ┌────────┐  ┌─────────┐  ┌─────────┐  ┌────────┐
    │userdb  │  │invoicedb│  │documentdb│ │tracedb │
    └────────┘  └────┬────┘  └────┬────┘  └───┬────┘
                     │            │           │
                     │            ▼           │
                     │       ┌─────────┐      │
                     │       │  MinIO  │      │
                     │       │  (PDFs) │      │
                     │       └─────────┘      │
                     │                        │
                     └────────┬───────────────┘
                              │
                              ▼
                         ┌─────────┐
                         │  Kafka  │
                         │ Events  │
                         └─────────┘
```

---

## ✅ CHECKLIST PARA EQUIPO UX/UI

### Fase 1: Diseño (2-3 semanas)

- [ ] **Wireframes de baja fidelidad** (Figma/Sketch)
  - [ ] LoginScreen
  - [ ] Dashboard
  - [ ] InvoiceListScreen
  - [ ] InvoiceDetailScreen
  - [ ] InvoiceCreateWizard (5 pasos)
  - [ ] UserListScreen

- [ ] **Mockups de alta fidelidad**
  - [ ] Aplicar paleta de colores
  - [ ] Definir tipografía
  - [ ] Crear componentes reutilizables
  - [ ] Estados hover, active, disabled

- [ ] **Diseño responsive**
  - [ ] Desktop (1920px, 1440px, 1024px)
  - [ ] Tablet (768px)
  - [ ] Mobile (375px, 414px)

- [ ] **Flujos de usuario**
  - [ ] Diagrama de navegación
  - [ ] User journey maps
  - [ ] Estados de error/éxito

- [ ] **Design system**
  - [ ] Biblioteca de componentes
  - [ ] Guía de estilos
  - [ ] Tokens de diseño

### Fase 2: Prototipo (1 semana)

- [ ] **Prototipo interactivo** (Figma/InVision)
  - [ ] Login → Dashboard
  - [ ] Crear factura (wizard completo)
  - [ ] Ver detalle de factura
  - [ ] Generar PDF

- [ ] **Testing de usabilidad**
  - [ ] 5 usuarios reales
  - [ ] Identificar puntos de fricción
  - [ ] Iterar diseño

### Fase 3: Handoff a Desarrollo (1 semana)

- [ ] **Exportar assets**
  - [ ] Iconos SVG
  - [ ] Imágenes optimizadas
  - [ ] Logos en diferentes tamaños

- [ ] **Documentación**
  - [ ] Guía de implementación
  - [ ] Especificaciones de componentes
  - [ ] Variables CSS / Design tokens

- [ ] **Reunión de handoff**
  - [ ] Presentar diseño a developers
  - [ ] Explicar interacciones
  - [ ] Aclarar dudas

---

## 🚀 RECOMENDACIONES FINALES PARA UX/UI

### 1. **Prioriza estas pantallas primero:**

**Sprint 1 (MVP):**
1. LoginScreen → Para poder autenticarse
2. Dashboard → Landing después del login
3. InvoiceListScreen → Ver facturas existentes
4. InvoiceDetailScreen → Ver detalle y descargar PDF

**Sprint 2:**
5. InvoiceCreateWizard → Crear nuevas facturas

**Sprint 3:**
6. UserListScreen → Gestión de usuarios (admin)

### 2. **Patrones de diseño recomendados:**

- **Material Design** o **Ant Design** para componentes
- **Sidebar navigation** para desktop
- **Bottom navigation** para mobile
- **Cards** para listas de facturas
- **Stepper/Wizard** para creación de facturas
- **Modal dialogs** para confirmaciones
- **Toast notifications** para feedback de acciones

### 3. **Librerías UI recomendadas:**

**React:**
- Material-UI (MUI)
- Ant Design
- Chakra UI
- Tailwind CSS + Headless UI

**Vue:**
- Vuetify
- Element Plus
- Quasar

**Angular:**
- Angular Material
- PrimeNG
- Nebular

### 4. **Herramientas de diseño:**

- **Figma** (recomendado) - Colaborativo, web-based
- **Sketch** - macOS only
- **Adobe XD** - Cross-platform
- **Penpot** - Open source, web-based

### 5. **Testing de usabilidad:**

- **Hotjar** - Heatmaps y grabaciones de sesiones
- **Maze** - Test de prototipos
- **UserTesting** - Usuarios reales
- **Google Analytics** - Métricas de uso

---

## 📊 EJEMPLO DE USER STORY PARA DEVELOPERS

```
Como usuario administrativo
Quiero crear una nueva factura
Para poder facturar a mis clientes

Criterios de aceptación:
✓ Puedo seleccionar la empresa emisora de un dropdown
✓ Puedo seleccionar el cliente de un dropdown o crear uno nuevo
✓ Puedo añadir múltiples ítems con descripción, cantidad, precio e IVA
✓ El sistema calcula automáticamente subtotal, IVA y total
✓ Puedo revisar todos los datos antes de crear la factura
✓ Al crear la factura, se genera automáticamente un número secuencial
✓ Recibo confirmación visual de que la factura fue creada
✓ Puedo descargar el PDF inmediatamente después de crear

Endpoints:
- GET /api/companies
- GET /api/clients
- POST /api/invoices
- POST /api/invoices/{id}/generate-pdf

Diseño: Ver Figma → Invoice Create Wizard
```

---

## 🎯 CONCLUSIÓN

Este documento proporciona toda la información que el equipo UX/UI necesita para diseñar el frontend:

✅ **15 pantallas principales** identificadas
✅ **Flujos de usuario** documentados
✅ **Endpoints específicos** para cada pantalla
✅ **Wireframes de ejemplo** para inspiración
✅ **Paleta de colores** profesional
✅ **Componentes reutilizables** sugeridos
✅ **Estados y permisos** definidos
✅ **Checklist completo** para el proceso de diseño

**El backend está 90%+ listo. El equipo UX/UI puede empezar a diseñar AHORA.**

Para cualquier duda sobre endpoints, respuestas del API o flujos de datos, consultar:
- **README.md** - Guía completa del backend
- **ANALISIS_Y_MEJORAS.md** - Estado actual y puntos de mejora
- **OpenAPI Specs** - Documentación detallada de cada endpoint

**¡Manos a la obra! 🎨**
