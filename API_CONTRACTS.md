# 📋 CONTRATOS API BACKEND - INVOICES SYSTEM
**Versión**: 1.0.0  
**Base URL**: `/api`  
**Autenticación**: Bearer Token JWT (Header: `Authorization: Bearer <token>`)

---

## 🔐 1. AUTHENTICATION & USERS

### 1.1 Login
**POST** `/auth/login`

**Request:**
```json
{
  "email": "user@example.com",     // o "username" (alias aceptado)
  "password": "securePassword123"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["ROLE_USER", "ROLE_ADMIN"],
    "enabled": true,
    "currentCompanyId": 5,
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-15T14:30:00"
  }
}
```

### 1.2 Get Current User
**GET** `/users/me`

**Response 200:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["ROLE_USER"],
  "enabled": true,
  "currentCompanyId": 5,
  "createdAt": "2025-01-01T10:00:00",
  "updatedAt": "2025-01-15T14:30:00"
}
```


---

## 🧾 2. INVOICES

### 2.1 List Invoices (Paginated)
**GET** `/invoices?page=0&size=20`

**Response 200:**
```json
{
  "headers": {
    "X-Total-Count": "150"
  },
  "body": [
    {
      "id": 1,
      "companyId": 5,
      "clientId": 10,
      "company": { /* CompanyDTO */ },
      "client": { /* ClientDTO */ },
      "invoiceNumber": "001/2025",
      "settlementNumber": "LIQ-001",
      "issueDate": "2025-01-15T10:00:00",
      "baseAmount": 1000.00,
      "irpfPercentage": 15.00,
      "irpfAmount": 150.00,
      "rePercentage": 0.00,
      "reAmount": 0.00,
      "totalAmount": 850.00,
      "status": "PAID",              // DRAFT | SENT | PAID
      "verifactuStatus": "ACCEPTED",  // PENDING | SENDING | ACCEPTED | REJECTED
      "documentHash": "abc123...",
      "pdfServerPath": "companies/5/invoices/001-2025.pdf",
      "documentJson": "{ /* canonical JSON */ }",
      "verifactuTxId": "TX-12345",
      "pdfIsFinal": true,
      "notes": "Client notes here",
      "createdAt": "2025-01-15T10:00:00",
      "updatedAt": "2025-01-15T14:00:00",
      "items": [
        {
          "id": 1,
          "description": "Service A",
          "quantity": 2,
          "unitPrice": 500.00,
          "taxRate": 21.00,
          "total": 1000.00,
          "vehiclePlate": "1234ABC"
        }
      ],
      "verifactuError": null
    }
  ]
}
```

### 2.2 Get Invoice by ID
**GET** `/invoices/{id}`

**Response 200:** Same as item in list above

### 2.3 Create Invoice
**POST** `/invoices`

**Request:**
```json
{
  "companyId": 5,
  "clientId": 10,
  "invoiceNumber": "",              // Opcional: Backend auto-genera si vacío
  "settlementNumber": "LIQ-001",    // Opcional
  "irpfPercentage": 15.00,
  "rePercentage": 0.00,
  "notes": "Optional notes",
  "items": [
    {
      "description": "Service A",
      "quantity": 2,
      "unitPrice": 500.00,
      "taxRate": 21.00,
      "vehiclePlate": "1234ABC"      // Opcional
    }
  ]
}
```

**Validaciones:**
- `companyId`: Required
- `clientId`: Required
- `items`: Min 1 item required
- `irpfPercentage`: 0-100
- `rePercentage`: 0-100
- `invoiceNumber`: Max 50 chars, auto-generated if null/empty

**Response 201:** InvoiceDTO (same structure as GET)

### 2.4 Update Invoice
**PUT** `/invoices/{id}`

**Request:** Same as Create (all fields)

**Response 200:** InvoiceDTO

### 2.5 Delete Invoice
**DELETE** `/invoices/{id}`

**Response 204:** No Content

### 2.6 Download PDF
**GET** `/invoices/{id}/pdf?version=draft`

**Query Params:**
- `version`: `draft` | `final`
  - `draft`: PDF sin QR (siempre disponible)
  - `final`: PDF con QR Verifactu (solo si verifactuStatus = "ACCEPTED")

**Response 200:**
- Content-Type: `application/pdf`
- Headers: `Content-Disposition: attachment; filename="Factura_001_2025_draft.pdf"`
- Body: Binary PDF

**Response 403:** Si `version=final` pero invoice no verificada
```json
{
  "error": "PDF final solo disponible para facturas verificadas"
}
```

### 2.7 Get Canonical JSON
**GET** `/invoices/{id}/canonical`

**Response 200:**
```json
{
  "canonicalJson": "{ /* Canonical representation for Verifactu */ }",
  "documentHash": "abc123..."
}
```

### 2.8 Get Verification Status
**GET** `/invoices/{id}/verification-status`

**Response 200:**
```json
{
  "invoiceId": 1,
  "verifactuStatus": "ACCEPTED",
  "verifactuTxId": "TX-12345",
  "qrPayload": "https://www2.agenciatributaria.gob.es/wlpl/TIKE-CONT/verificar?csv=XXX",
  "documentHash": "abc123...",
  "lastVerificationAttempt": "2025-01-15T14:00:00",
  "error": null
}
```


---

## 👥 3. CLIENTS

### 3.1 List Clients
**GET** `/clients`

**Response 200:**
```json
[
  {
    "id": 10,
    "businessName": "Cliente ABC S.L.",
    "taxId": "B12345678",
    "address": "Calle Mayor 123",
    "city": "Madrid",
    "postalCode": "28001",
    "province": "Madrid",
    "country": "España",
    "phone": "+34 600 000 000",
    "email": "contacto@clienteabc.com",
    "companyId": 5
  }
]
```

### 3.2 Get Client by ID
**GET** `/clients/{id}`

**Response 200:** Same as list item

### 3.3 Create Client
**POST** `/clients`

**Request:**
```json
{
  "businessName": "Cliente ABC S.L.",
  "taxId": "B12345678",             // Validación NIF/CIF española
  "address": "Calle Mayor 123",
  "city": "Madrid",
  "postalCode": "28001",
  "province": "Madrid",
  "country": "España",
  "phone": "+34 600 000 000",
  "email": "contacto@clienteabc.com"
}
```

**Validaciones:**
- `taxId`: Validación NIF/CIF española (con @ValidNif)
- Todos los strings se sanitizan automáticamente (XSS protection)

**Response 201:** ClientDTO

### 3.4 Update Client
**PUT** `/clients/{id}`

**Request:** Same as Create

**Response 200:** ClientDTO

### 3.5 Delete Client
**DELETE** `/clients/{id}`

**Response 204:** No Content

---

## 🏢 4. COMPANIES

### 4.1 Get My Companies
**GET** `/companies/my`

**Response 200:**
```json
[
  {
    "id": 5,
    "businessName": "Mi Empresa S.L.",
    "taxId": "B98765432",
    "address": "Avenida Principal 1",
    "city": "Barcelona",
    "postalCode": "08001",
    "province": "Barcelona",
    "phone": "+34 600 111 111",
    "email": "admin@miempresa.com",
    "iban": "ES9121000418450200051332",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2025-01-15T10:00:00",
    "role": "ADMIN",                // Role del usuario actual
    "isDefault": true               // Si es la empresa actual activa
  }
]
```

### 4.2 Switch Company
**POST** `/companies/switch/{companyId}`

**Response 200:**
```json
{
  "message": "Company switched successfully",
  "newCompanyId": 7
}
```

### 4.3 Create Company
**POST** `/companies`

**Request:**
```json
{
  "businessName": "Nueva Empresa S.L.",
  "taxId": "B11111111",
  "address": "Calle Nueva 10",
  "city": "Valencia",
  "postalCode": "46001",
  "province": "Valencia",
  "phone": "+34 600 222 222",
  "email": "info@nuevaempresa.com",
  "iban": "ES9121000418450200051332"
}
```

**Response 201:** CompanyDto

### 4.4 Update Company
**PUT** `/companies/{id}`

**Request:** Same as Create

**Response 200:** CompanyDto

### 4.5 Get Company Users
**GET** `/companies/{id}/users`

**Response 200:**
```json
{
  "users": [
    {
      "userId": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "role": "ADMIN",
      "joinedAt": null
    }
  ]
}
```

### 4.6 Update User Role
**PATCH** `/companies/{companyId}/users/{userId}/role`

**Request:**
```json
{
  "role": "USER"                    // "ADMIN" | "USER"
}
```

**Response 200:** Success message

### 4.7 Remove User from Company
**DELETE** `/companies/{companyId}/users/{userId}`

**Response 204:** No Content

**Business Rules:**
- Cannot remove last ADMIN from company
- Cannot change role of last ADMIN to USER

---

## 📄 5. DOCUMENTS

### 5.1 Upload Document
**POST** `/documents`

**Content-Type:** `multipart/form-data`

**Form Fields:**
- `file`: PDF file (required)
- `invoiceId`: Long (optional)
- `uploadedBy`: String (optional, default: "system")

**Response 201:**
```json
{
  "documentId": 100,
  "filename": "factura-001-2025.pdf",
  "downloadUrl": "/api/documents/100/download",
  "uploadedAt": "2025-01-15T10:00:00"
}
```

**Validations:**
- File type: Must be PDF (`application/pdf`)
- File size: Max configurable (default: 10MB)
- Empty files rejected

### 5.2 Get Document Metadata
**GET** `/documents/{id}`

**Response 200:**
```json
{
  "id": 100,
  "filename": "factura-001-2025.pdf",
  "originalFilename": "Factura_ABC.pdf",
  "contentType": "application/pdf",
  "fileSize": 245760,
  "invoiceId": 1,
  "uploadedBy": "user@example.com",
  "createdAt": "2025-01-15T10:00:00",
  "downloadUrl": "/api/documents/100/download"
}
```

### 5.3 Download Document
**GET** `/documents/{id}/download`

**Response 200:**
- Content-Type: `application/pdf`
- Headers: `Content-Disposition: attachment; filename="factura-001-2025.pdf"`
- Body: Binary PDF

### 5.4 Get Documents by Invoice
**GET** `/documents?invoiceId=1`

**Response 200:**
```json
[
  { /* DocumentDTO */ },
  { /* DocumentDTO */ }
]
```

### 5.5 Delete Document
**DELETE** `/documents/{id}`

**Response 204:** No Content


---

## ✅ 6. VERIFACTU

### 6.1 Get Verifactu Metrics
**GET** `/verifactu/metrics`

**Response 200:**
```json
{
  "totalInvoices": 150,
  "acceptedToday": 12,
  "pendingVerification": 3,
  "acceptedYesterday": 15,
  "rejectedCount": 2,
  "averageVerificationTime": 2500,
  "lastBatchTime": "2025-01-15T14:00:00"
}
```

### 6.2 Submit Invoice for Verification
**POST** `/verifactu/invoices/{invoiceId}/submit`

**Response 200:**
```json
{
  "success": true,
  "message": "Invoice submitted for verification",
  "invoiceId": 1,
  "status": "SENDING"
}
```

### 6.3 Get Batch Summary
**GET** `/verifactu/batch/summary`

**Response 200:**
```json
{
  "batchId": "BATCH-001",
  "totalInvoices": 50,
  "accepted": 48,
  "rejected": 2,
  "pending": 0,
  "startTime": "2025-01-15T10:00:00",
  "endTime": "2025-01-15T10:05:00"
}
```

---

## 📊 7. ANALYTICS & METRICS

### 7.1 Get Company Metrics
**GET** `/companies/{id}/metrics`

**Response 200:**
```json
{
  "totalInvoices": 150,
  "paidInvoices": 120,
  "pendingInvoices": 30,
  "totalRevenue": 125000.00,
  "pendingRevenue": 25000.00,
  "totalClients": 45,
  "activeUsers": 3,
  "periodStart": "2024-01-15",
  "periodEnd": "2025-01-15"
}
```

### 7.2 Get Dashboard
**GET** `/dashboard`

**Response 200:**
```json
{
  "type": "COMPANY_ADMIN",          // COMPANY_ADMIN | COMPANY_USER | PLATFORM_ADMIN
  "companyMetrics": {
    "totalInvoices": 150,
    "totalRevenue": 125000.00,
    "pendingInvoices": 30
  },
  "recentInvoices": [
    { /* InvoiceDTO */ }
  ],
  "topClients": [
    {
      "clientId": 10,
      "businessName": "Cliente ABC",
      "totalRevenue": 15000.00,
      "invoiceCount": 12
    }
  ]
}
```

---

## 🔍 8. SEARCH

### 8.1 Global Search
**GET** `/search?q=ABC&type=all`

**Query Params:**
- `q`: Search query (required)
- `type`: `all` | `invoices` | `clients` | `companies`

**Response 200:**
```json
{
  "invoices": [
    { /* InvoiceDTO */ }
  ],
  "clients": [
    { /* ClientDTO */ }
  ],
  "companies": [
    { /* CompanyDTO */ }
  ],
  "totalResults": 15
}
```

---

## ⚠️ 9. ERROR RESPONSES

Todos los endpoints pueden devolver estos errores estándar:

### 400 Bad Request
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2025-01-15T10:00:00",
  "path": "/api/invoices",
  "errors": [
    {
      "field": "clientId",
      "message": "Client ID is required"
    }
  ]
}
```

### 401 Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Token expired or invalid",
  "timestamp": "2025-01-15T10:00:00",
  "path": "/api/invoices"
}
```

### 403 Forbidden
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. User does not have access to this company.",
  "timestamp": "2025-01-15T10:00:00",
  "path": "/api/companies/5"
}
```

### 404 Not Found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Invoice not found with id: 999",
  "timestamp": "2025-01-15T10:00:00",
  "path": "/api/invoices/999"
}
```

### 409 Conflict
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Invoice number already exists: 001/2025",
  "timestamp": "2025-01-15T10:00:00",
  "path": "/api/invoices"
}
```

### 500 Internal Server Error
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "timestamp": "2025-01-15T10:00:00",
  "path": "/api/invoices"
}
```

---

## 📝 10. IMPORTANT NOTES

### 10.1 Authentication Flow
1. Client calls `POST /auth/login` with credentials
2. Backend returns JWT token in response
3. Client stores token (localStorage/sessionStorage)
4. Client includes token in all subsequent requests:
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
   ```
5. Token expires after configured time (default: 24 hours)

### 10.2 Company Context
- JWT token contains `companyId` claim indicating current active company
- Backend automatically filters data by `companyId` from token
- Frontend should NOT send `companyId` in request bodies (except for super-admin operations)
- To switch company, call `POST /companies/switch/{companyId}`

### 10.3 Invoice Number Auto-Generation
- If `invoiceNumber` is `null` or empty string, backend auto-generates sequential number
- Format: `001/2025`, `002/2025`, etc.
- Generation is scoped by company and year
- Frontend should send empty string or omit field to use auto-generation

### 10.4 Verifactu Status Flow
```
PENDING → SENDING → ACCEPTED/REJECTED
```
- `PENDING`: Invoice created, waiting for verification
- `SENDING`: Verification request sent to AEAT
- `ACCEPTED`: Invoice accepted by AEAT (QR available)
- `REJECTED`: Invoice rejected by AEAT (error details in `verifactuError`)

### 10.5 PDF Versions
- **Draft PDF**: Always available, no QR code
- **Final PDF**: Only available after `verifactuStatus = ACCEPTED`, includes QR code

### 10.6 Data Sanitization
- All string inputs are automatically sanitized (XSS protection)
- Applies to: Client names, addresses, invoice notes, etc.
- Sanitization removes/escapes: `<script>`, `<iframe>`, etc.

### 10.7 Rate Limiting
- General endpoints: 100 requests/minute per IP
- Auth endpoints: 10 requests/minute per IP
- Exceeded limits return `429 Too Many Requests`

### 10.8 File Upload Limits
- Max file size: 10MB (configurable)
- Allowed types: `application/pdf` only
- Files stored in MinIO/S3 with unique keys

### 10.9 Pagination
- Default page size: 20
- Max page size: 100
- Total count returned in `X-Total-Count` header
- Page numbers are 0-indexed

### 10.10 Date Formats
- All dates use ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`
- Timezone: UTC (backend converts to user timezone if needed)
- Example: `2025-01-15T10:30:00`

---

## 🔗 11. ENDPOINT SUMMARY

| Module | Endpoints Count | Base Path |
|--------|----------------|-----------|
| Auth & Users | 2 | `/auth`, `/users` |
| Invoices | 8 | `/invoices` |
| Clients | 5 | `/clients` |
| Companies | 7 | `/companies` |
| Documents | 5 | `/documents` |
| Verifactu | 3 | `/verifactu` |
| Analytics | 2 | `/dashboard`, `/companies/{id}/metrics` |
| Search | 1 | `/search` |
| **TOTAL** | **33** | `/api/*` |

---

## ✨ 12. CHANGELOG

### Version 1.0.0 (2025-01-15)
- Initial API contracts documentation
- Documented all core endpoints
- Added validation rules and business logic notes
- Included error response formats
- Documented authentication flow and company context

---

**Fecha de generación**: 2025-12-03  
**Mantenedor**: Backend Team  
**Contacto**: Para preguntas sobre estos contratos, consultar con el equipo de backend

