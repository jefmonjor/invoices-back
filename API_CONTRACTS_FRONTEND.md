# Contratos de API - Invoices Backend para Frontend

## Formato General

### Fechas
- **Formato:** ISO-8601 string `"yyyy-MM-dd'T'HH:mm:ss"`
- **Ejemplo:** `"2025-11-20T16:34:19"`
- **IMPORTANTE:** Ya NO se envían como arrays `[2025, 11, 20, ...]`

### Números Decimales
- **Formato:** Number o String
- **Ejemplo:** `21.00` o `"21.00"`
- **Escala:** 2 decimales

---

## 1. POST /api/invoices - Crear Factura

### Request Body (CreateInvoiceRequest)
```json
{
  "companyId": 1,                    // REQUERIDO - Long
  "clientId": 1,                     // REQUERIDO - Long
  "invoiceNumber": "047/2025",       // REQUERIDO - String (max 50 chars)
                                     // Acepta: letras, números, guiones, puntos, barras
                                     // Ejemplos válidos: "047/2025", "A057/2025", "INV-2025-001", "4592JBZ-SEP-25"

  "settlementNumber": "LIQ-001",     // OPCIONAL - String (max 50 chars)

  "irpfPercentage": 15.0,            // OPCIONAL - Decimal (0-100)
  "rePercentage": 5.2,               // OPCIONAL - Decimal (0-100)

  "notes": "Notas adicionales",      // OPCIONAL - String (max 5000 chars)

  "items": [                         // REQUERIDO - Array (mínimo 1 item)
    {
      "description": "Servicio de transporte",  // REQUERIDO - String (max 255 chars)
      "units": 3,                                // REQUERIDO - Integer (min 1)
      "price": 15.00,                            // REQUERIDO - Decimal (min 0)
      "vatPercentage": 21.00,                    // REQUERIDO - Decimal (0-100)
      "discountPercentage": 0.00,                // OPCIONAL - Decimal (0-100), default: 0

      // Campos extendidos (TODOS OPCIONALES)
      "itemDate": "2025-11-20",                  // LocalDate - Fecha específica del item
      "vehiclePlate": "1234ABC",                 // String (max 50) - Matrícula
      "orderNumber": "PED-123",                  // String (max 50) - Número de pedido
      "zone": "ZONA-A",                          // String (max 100) - Zona de trabajo
      "gasPercentage": 5.5                       // Decimal (0-100) - Porcentaje de gas
    }
  ]
}
```

### Response 201 Created
```json
{
  "id": 1,
  "companyId": 1,
  "clientId": 1,
  "company": {
    "id": 1,
    "businessName": "TRANSOLIDO S.L.",
    "taxId": "B91923755",
    "address": "Castillo Lastrucci, 3, 3D",
    "city": "DOS HERMANAS",
    "postalCode": "41701",
    "province": "SEVILLA",
    "phone": "659889201",
    "email": "contacto@transolido.es",
    "iban": "ES60 0182 4840 0022 0165 7539"
  },
  "client": {
    "id": 1,
    "businessName": "SERSFRITRUCKS, S.A.",
    "taxId": "A50008588",
    "address": "JIMÉNEZ DE LA ESPADA, 57, BAJO",
    "city": "CARTAGENA",
    "postalCode": "30203",
    "province": "MURCIA",
    "phone": "968123456",
    "email": "info@sersfritrucks.com"
  },
  "invoiceNumber": "047/2025",
  "settlementNumber": "LIQ-001",
  "issueDate": "2025-11-20T16:34:19",      // GENERADO AUTOMÁTICAMENTE por backend
  "baseAmount": 45.00,                      // CALCULADO AUTOMÁTICAMENTE
  "irpfPercentage": 15.00,
  "irpfAmount": 6.75,                       // CALCULADO AUTOMÁTICAMENTE
  "rePercentage": 5.20,
  "reAmount": 2.34,                         // CALCULADO AUTOMÁTICAMENTE
  "totalAmount": 54.45,                     // CALCULADO AUTOMÁTICAMENTE
  "status": "DRAFT",                        // SIEMPRE "DRAFT" al crear
  "notes": "Notas adicionales",
  "createdAt": "2025-11-20T16:34:19",
  "updatedAt": "2025-11-20T16:34:19",
  "items": [
    {
      "id": 1,
      "invoiceId": 1,
      "description": "Servicio de transporte",
      "units": 3,
      "price": 15.00,
      "vatPercentage": 21.00,
      "discountPercentage": 0.00,
      "subtotal": 45.00,                    // CALCULADO AUTOMÁTICAMENTE
      "total": 54.45,                       // CALCULADO AUTOMÁTICAMENTE
      "itemDate": "2025-11-20",
      "vehiclePlate": "1234ABC",
      "orderNumber": "PED-123",
      "zone": "ZONA-A",
      "gasPercentage": 5.50,
      "createdAt": "2025-11-20T16:34:19",
      "updatedAt": "2025-11-20T16:34:19"
    }
  ]
}
```

---

## 2. GET /api/invoices - Listar Todas las Facturas

### Request
- No requiere parámetros

### Response 200 OK
```json
[
  {
    "id": 1,
    "companyId": 1,
    "clientId": 1,
    "company": { /* CompanyDTO */ },
    "client": { /* ClientDTO */ },
    "invoiceNumber": "047/2025",
    "settlementNumber": "LIQ-001",
    "issueDate": "2025-11-20T16:34:19",
    "baseAmount": 45.00,
    "irpfPercentage": 15.00,
    "irpfAmount": 6.75,
    "rePercentage": 5.20,
    "reAmount": 2.34,
    "totalAmount": 54.45,
    "status": "DRAFT",
    "notes": "Notas",
    "createdAt": "2025-11-20T16:34:19",
    "updatedAt": "2025-11-20T16:34:19",
    "items": [ /* InvoiceItemDTO[] */ ]
  }
]
```

---

## 3. GET /api/invoices/{id} - Obtener Factura por ID

### Request
- **Path Parameter:** `id` (Long) - ID de la factura

### Response 200 OK
```json
{
  "id": 1,
  "companyId": 1,
  "clientId": 1,
  "company": { /* CompanyDTO */ },
  "client": { /* ClientDTO */ },
  "invoiceNumber": "047/2025",
  "settlementNumber": "LIQ-001",
  "issueDate": "2025-11-20T16:34:19",
  "baseAmount": 45.00,
  "irpfPercentage": 15.00,
  "irpfAmount": 6.75,
  "rePercentage": 5.20,
  "reAmount": 2.34,
  "totalAmount": 54.45,
  "status": "DRAFT",
  "notes": "Notas",
  "createdAt": "2025-11-20T16:34:19",
  "updatedAt": "2025-11-20T16:34:19",
  "items": [ /* InvoiceItemDTO[] */ ]
}
```

### Response 404 Not Found
```json
{
  "message": "Invoice not found with id: 1",
  "timestamp": "2025-11-20T16:34:19"
}
```

---

## 4. PUT /api/invoices/{id} - Actualizar Factura

### Request Body (UpdateInvoiceRequest)
```json
{
  // ⚠️ CAMPOS INMUTABLES (puedes enviarlos pero NO se actualizarán)
  // Si intentas cambiarlos, recibirás un error 400
  "companyId": 1,            // INMUTABLE - Si difiere del actual: ERROR
  "clientId": 1,             // INMUTABLE - Si difiere del actual: ERROR
  "invoiceNumber": "047/2025", // INMUTABLE - Si difiere del actual: ERROR
  "irpfPercentage": 15.0,    // INMUTABLE - Si difiere del actual: ERROR
  "rePercentage": 5.2,       // INMUTABLE - Si difiere del actual: ERROR

  // ✅ CAMPOS ACTUALIZABLES
  "settlementNumber": "LIQ-002",  // OPCIONAL - String (max 50)
  "notes": "Notas actualizadas",  // OPCIONAL - String (max 5000)

  "items": [                      // OPCIONAL - Array (reemplaza TODOS los items)
    {
      "description": "Nuevo servicio",
      "units": 5,
      "price": 20.00,
      "vatPercentage": 21.00,
      "discountPercentage": 0.00,
      "itemDate": "2025-11-20",
      "vehiclePlate": "5678XYZ",
      "orderNumber": "PED-456",
      "zone": "ZONA-B",
      "gasPercentage": 10.0
    }
  ]
}
```

### ⚠️ IMPORTANTE - Campos Inmutables
Si envías un campo inmutable con un valor diferente al actual, recibirás:

**Response 400 Bad Request**
```json
{
  "message": "Cannot change company ID. Current: 1, Requested: 2",
  "timestamp": "2025-11-20T16:34:19"
}
```

### Recomendación Frontend
**Opción 1:** NO envíes los campos inmutables en el PUT
```json
{
  "settlementNumber": "LIQ-002",
  "notes": "Solo actualizar lo mutable",
  "items": [...]
}
```

**Opción 2:** Envía los campos inmutables CON EL MISMO VALOR que obtuviste en el GET
```json
{
  "companyId": 1,           // Mismo valor que GET
  "clientId": 1,            // Mismo valor que GET
  "invoiceNumber": "047/2025", // Mismo valor que GET
  "irpfPercentage": 15.0,   // Mismo valor que GET
  "rePercentage": 5.2,      // Mismo valor que GET
  "settlementNumber": "LIQ-002",
  "notes": "Actualizado",
  "items": [...]
}
```

### Response 200 OK
```json
{
  "id": 1,
  "companyId": 1,
  "clientId": 1,
  // ... mismo formato que GET /api/invoices/{id}
  "settlementNumber": "LIQ-002",
  "notes": "Notas actualizadas",
  "updatedAt": "2025-11-20T17:00:00",  // Timestamp actualizado
  "items": [ /* nuevos items */ ]
}
```

---

## 5. DELETE /api/invoices/{id} - Eliminar Factura

### Request
- **Path Parameter:** `id` (Long) - ID de la factura

### Response 204 No Content
- Sin cuerpo de respuesta

### Response 404 Not Found
```json
{
  "message": "Invoice not found with id: 1",
  "timestamp": "2025-11-20T16:34:19"
}
```

---

## 6. GET /api/invoices/{id}/pdf - Generar PDF de Factura

### Request
- **Path Parameter:** `id` (Long) - ID de la factura

### Response 200 OK
- **Content-Type:** `application/pdf`
- **Content-Disposition:** `inline; filename=invoice-{id}.pdf`
- **Body:** Bytes del PDF

### Ejemplo Frontend (JavaScript)
```javascript
// Descargar PDF
const response = await fetch(`/api/invoices/${id}/pdf`, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

if (response.ok) {
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `invoice-${id}.pdf`;
  a.click();
  window.URL.revokeObjectURL(url);
}
```

### Response 404 Not Found
```json
{
  "message": "Invoice not found with id: 1",
  "timestamp": "2025-11-20T16:34:19"
}
```

### Response 500 Internal Server Error
```json
{
  "message": "Error generating PDF for invoice 1",
  "timestamp": "2025-11-20T16:34:19"
}
```

---

## Estados de Factura (InvoiceStatus)

```typescript
type InvoiceStatus =
  | "DRAFT"      // Borrador - recién creada
  | "PENDING"    // Pendiente de pago
  | "FINALIZED"  // Finalizada
  | "PAID"       // Pagada
  | "CANCELLED"  // Cancelada
```

---

## Validaciones de Frontend a Implementar

### CreateInvoiceRequest
1. ✅ `companyId` - Requerido, número positivo
2. ✅ `clientId` - Requerido, número positivo
3. ✅ `invoiceNumber` - Requerido, máx 50 chars, pattern: `^[A-Za-z0-9./-]+$`
4. ✅ `settlementNumber` - Opcional, máx 50 chars
5. ✅ `irpfPercentage` - Opcional, 0-100
6. ✅ `rePercentage` - Opcional, 0-100
7. ✅ `notes` - Opcional, máx 5000 chars
8. ✅ `items` - Requerido, mínimo 1 item

### CreateInvoiceItemRequest
1. ✅ `description` - Requerido, máx 255 chars
2. ✅ `units` - Requerido, mínimo 1
3. ✅ `price` - Requerido, mínimo 0
4. ✅ `vatPercentage` - Requerido, 0-100
5. ✅ `discountPercentage` - Opcional, 0-100
6. ✅ `itemDate` - Opcional, formato ISO date
7. ✅ `vehiclePlate` - Opcional, máx 50 chars
8. ✅ `orderNumber` - Opcional, máx 50 chars
9. ✅ `zone` - Opcional, máx 100 chars
10. ✅ `gasPercentage` - Opcional, 0-100

### UpdateInvoiceRequest
- Mismas validaciones que CreateInvoiceRequest
- ⚠️ Campos inmutables deben coincidir con valores actuales o NO enviarse

---

## Cambios Importantes para Frontend

### 1. ✅ Fechas como Strings (NO arrays)
**Antes (INCORRECTO):**
```json
{
  "issueDate": [2025, 11, 20, 16, 34, 19, 43429836]
}
```

**Ahora (CORRECTO):**
```json
{
  "issueDate": "2025-11-20T16:34:19"
}
```

### 2. ✅ Campo `date` en CreateInvoiceRequest ELIMINADO
**NO enviar:**
```json
{
  "date": "2025-11-20T16:34:19"  // ❌ Este campo NO existe
}
```

El backend genera `issueDate` automáticamente con `LocalDateTime.now()`

### 3. ✅ UpdateInvoiceRequest ahora acepta todos los campos
**Antes:**
```json
{
  "notes": "...",
  "items": [...]
}
```

**Ahora (Opción 1 - Recomendada):**
```json
{
  "notes": "...",
  "settlementNumber": "...",
  "items": [...]
}
```

**Ahora (Opción 2):**
```json
{
  "companyId": 1,        // Mismo valor que GET
  "clientId": 1,         // Mismo valor que GET
  "invoiceNumber": "...", // Mismo valor que GET
  "irpfPercentage": 15,  // Mismo valor que GET
  "rePercentage": 5.2,   // Mismo valor que GET
  "settlementNumber": "...",
  "notes": "...",
  "items": [...]
}
```

### 4. ✅ Nuevos campos extendidos en items
```json
{
  "items": [
    {
      // Campos básicos
      "description": "...",
      "units": 1,
      "price": 10.00,
      "vatPercentage": 21.00,

      // Campos extendidos (OPCIONALES)
      "itemDate": "2025-11-20",
      "vehiclePlate": "1234ABC",
      "orderNumber": "PED-123",
      "zone": "ZONA-A",
      "gasPercentage": 5.5
    }
  ]
}
```

---

## Testing de Integración

### Flujo Completo
```javascript
// 1. Crear factura
const createResponse = await fetch('/api/invoices', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    companyId: 1,
    clientId: 1,
    invoiceNumber: "TEST-001",
    settlementNumber: "LIQ-001",
    irpfPercentage: 15,
    rePercentage: 5.2,
    notes: "Test invoice",
    items: [{
      description: "Test item",
      units: 1,
      price: 100,
      vatPercentage: 21,
      itemDate: "2025-11-20",
      vehiclePlate: "1234ABC",
      zone: "ZONA-A"
    }]
  })
});

const invoice = await createResponse.json();
console.log('Created invoice:', invoice);
// issueDate será string: "2025-11-20T16:34:19"

// 2. Actualizar factura
const updateResponse = await fetch(`/api/invoices/${invoice.id}`, {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    // Solo campos mutables
    settlementNumber: "LIQ-002",
    notes: "Updated notes",
    items: invoice.items // Mantener items o actualizarlos
  })
});

const updatedInvoice = await updateResponse.json();
console.log('Updated invoice:', updatedInvoice);

// 3. Generar PDF
const pdfResponse = await fetch(`/api/invoices/${invoice.id}/pdf`, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const pdfBlob = await pdfResponse.blob();
// Descargar o mostrar PDF
```

---

## Errores Comunes y Soluciones

### Error 1: Fechas como arrays
**Síntoma:** `Invalid date value: [2025, 11, 20, ...]`
**Solución:** ✅ YA ARREGLADO - Backend ahora envía strings ISO-8601

### Error 2: Campo "date" no reconocido
**Síntoma:** `Unrecognized field "date"`
**Solución:** ✅ NO enviar campo `date` en CreateInvoiceRequest

### Error 3: Campo "companyId" no reconocido en PUT
**Síntoma:** `Unrecognized field "companyId" (class UpdateInvoiceRequest)`
**Solución:** ✅ YA ARREGLADO - UpdateInvoiceRequest ahora acepta todos los campos

### Error 4: PDF endpoint 500
**Síntoma:** `GET /api/invoices/2/pdf → 500`
**Solución:** ✅ YA ARREGLADO - Endpoint implementado correctamente

### Error 5: Intentar cambiar campo inmutable
**Síntoma:** `Cannot change company ID. Current: 1, Requested: 2`
**Solución:** En PUT, enviar el mismo valor que GET o no enviar el campo

---

## Resumen de Cambios Realizados

1. ✅ **UpdateInvoiceRequest extendido** - Acepta todos los campos sin error de deserialización
2. ✅ **Validación de campos inmutables** - Backend valida que no cambien
3. ✅ **Serialización de fechas corregida** - Fechas como strings ISO-8601
4. ✅ **Endpoint PDF implementado** - GET /api/invoices/{id}/pdf funcional
5. ✅ **Campo `date` eliminado** - Backend genera `issueDate` automáticamente
6. ✅ **Campos extendidos soportados** - itemDate, vehiclePlate, orderNumber, zone, gasPercentage

---

## Contacto para Dudas
Si el frontend encuentra algún error o necesita algún ajuste adicional, repórtalo con:
- Endpoint afectado
- Request enviado (JSON)
- Response recibido (JSON + status code)
- Error del browser console
