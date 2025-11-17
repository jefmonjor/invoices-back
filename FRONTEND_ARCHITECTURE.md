# 🎨 ARQUITECTURA DEL FRONTEND - Sistema de Gestión de Facturas

**Fecha:** 17 Noviembre 2025
**Backend:** http://localhost:8080 (API Gateway)
**Stack:** React 18 + TypeScript + Vite + Zustand + MUI

---

## 📦 STACK TECNOLÓGICO

### Core
- **React 18.2+** - UI Library
- **TypeScript 5+** - Type safety
- **Vite 5+** - Build tool (super rápido)

### Estado y Routing
- **Zustand 4+** - Estado global (más simple que Redux)
- **React Router v6** - Routing
- **React Query (TanStack Query)** - Server state management

### UI y Estilos
- **Material-UI (MUI) 5+** o **Ant Design 5+** - Component library
- **Tailwind CSS 3+** - Utility-first CSS
- **Emotion** - CSS-in-JS (viene con MUI)

### Formularios y Validación
- **React Hook Form 7+** - Form management
- **Zod 3+** - Schema validation

### HTTP y APIs
- **Axios 1.6+** - HTTP client
- **JWT Decode** - JWT token parsing

### Testing
- **Vitest** - Unit testing
- **React Testing Library** - Component testing
- **MSW (Mock Service Worker)** - API mocking

### Utils
- **date-fns** - Date manipulation
- **react-pdf** - PDF viewer
- **recharts** - Charts y gráficas
- **react-toastify** - Notifications
- **decimal.js** o **currency.js** - Manejo preciso de montos (BigDecimal en Java)

---

## 🏗️ ARQUITECTURA DE CARPETAS

```
invoices-frontend/
├── src/
│   ├── api/                      # API Clients
│   ├── components/               # Componentes reutilizables
│   ├── features/                 # Features por dominio
│   ├── hooks/                    # Custom hooks
│   ├── routes/                   # Routing
│   ├── store/                    # Estado global
│   ├── types/                    # TypeScript types
│   ├── utils/                    # Utilidades
│   ├── styles/                   # Estilos globales
│   ├── App.tsx
│   └── main.tsx
```

---

## 🔌 API CLIENT - CONFIGURACIÓN CRÍTICA

### `/src/api/client.ts`

```typescript
import axios, { AxiosError, AxiosRequestConfig } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Cliente Axios configurado
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor: Agregar JWT token automáticamente
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor: Manejar errores globales
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Token expirado o inválido
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Helper para manejar errores
export const handleApiError = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message;
  }
  return 'Error desconocido';
};
```

### `/src/api/auth.api.ts`

```typescript
import { apiClient } from './client';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  expiresIn: number;
  user: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    roles: string[];
  };
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export const authApi = {
  // Login
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', data);
    return response.data;
  },

  // Register
  register: async (data: RegisterRequest): Promise<void> => {
    await apiClient.post('/api/auth/register', data);
  },

  // Logout (local)
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },
};
```

### `/src/api/invoices.api.ts`

```typescript
import { apiClient } from './client';

/**
 * IMPORTANTE - Manejo de BigDecimal de Java:
 *
 * Java usa BigDecimal para montos (subtotal, taxAmount, totalAmount).
 * Spring Boot serializa BigDecimal como number en JSON.
 *
 * ⚠️ PRECAUCIÓN: JavaScript tiene problemas de precisión con decimales
 * Ejemplo: 0.1 + 0.2 = 0.30000000000000004
 *
 * SOLUCIONES:
 * 1. Para VISUALIZACIÓN: usar number y formatear con Intl.NumberFormat
 * 2. Para CÁLCULOS: NO hacer cálculos complejos en frontend, confiar en backend
 * 3. Si necesitas cálculos: usar librería como decimal.js o currency.js
 *
 * En este proyecto: Los cálculos se hacen en el BACKEND (Java BigDecimal),
 * el frontend solo visualiza y envía valores al backend.
 */

export interface InvoiceItem {
  id?: number; // Opcional para crear nuevos items
  description: string;
  quantity: number;
  unitPrice: number; // BigDecimal en Java → number en TS (solo para visualizar)
  taxRate: number;   // Porcentaje (ej: 21.0 para 21%)
  total?: number;    // Calculado por el backend
}

export interface CreateInvoiceRequest {
  invoiceNumber: string;
  companyId: number;
  clientId: number;
  issueDate: string;  // ISO-8601: "2025-11-17" (LocalDate en Java)
  dueDate: string;    // ISO-8601: "2025-12-17"
  items: InvoiceItem[];
}

export interface Invoice {
  id: number;  // Long en Java - Compatible si < 9,007,199,254,740,991
  invoiceNumber: string;
  companyId: number;
  clientId: number;
  issueDate: string;      // ISO-8601: "2025-11-17T10:30:00"
  dueDate: string;        // ISO-8601: "2025-12-17T23:59:59"
  status: 'DRAFT' | 'PENDING' | 'PAID' | 'CANCELLED'; // Debe coincidir con Java Enum
  subtotal: number;       // BigDecimal en Java → number en TS
  taxAmount: number;      // BigDecimal en Java → number en TS
  totalAmount: number;
  items: InvoiceItem[];
  createdAt: string;
  updatedAt: string;
}

export interface InvoiceListParams {
  page?: number;         // Número de página (0-based en Spring)
  size?: number;         // Tamaño de página (default: 20)
  sortBy?: string;       // Campo para ordenar (ej: "invoiceNumber")
  sortDir?: 'asc' | 'desc';  // Dirección de ordenamiento
  status?: string;       // Filtro por estado
  clientId?: number;     // Filtro por cliente
}

/**
 * Interfaz exacta de Spring Boot 3 - Page<T>
 *
 * Spring Data devuelve esta estructura al usar Pageable.
 * Jackson serializa automáticamente con estos nombres de campos.
 */
export interface PagedResponse<T> {
  content: T[];              // Array de elementos de la página actual
  pageable: {
    pageNumber: number;      // Número de página actual (0-based)
    pageSize: number;        // Tamaño de página
    offset: number;          // Offset total
    paged: boolean;
    unpaged: boolean;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
  };
  totalPages: number;        // Total de páginas
  totalElements: number;     // Total de elementos en todas las páginas
  last: boolean;             // ¿Es la última página?
  first: boolean;            // ¿Es la primera página?
  size: number;              // Tamaño de página
  number: number;            // Número de página actual (0-based)
  numberOfElements: number;  // Número de elementos en esta página
  empty: boolean;            // ¿Está vacía la página?
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
}

/**
 * Interfaz simplificada para uso en componentes
 * (extrae solo lo necesario de PagedResponse)
 */
export interface SimplePage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;  // number (0-based de Spring)
  size: number;
  isLast: boolean;
  isFirst: boolean;
}

/**
 * Helper para convertir PagedResponse a SimplePage
 */
export const toSimplePage = <T>(page: PagedResponse<T>): SimplePage<T> => ({
  content: page.content,
  totalElements: page.totalElements,
  totalPages: page.totalPages,
  currentPage: page.number,
  size: page.size,
  isLast: page.last,
  isFirst: page.first,
});

export const invoicesApi = {
  // Listar facturas
  list: async (params?: InvoiceListParams): Promise<PagedResponse<Invoice>> => {
    const response = await apiClient.get<PagedResponse<Invoice>>('/api/invoices', {
      params,
    });
    return response.data;
  },

  // Obtener factura por ID
  getById: async (id: number): Promise<Invoice> => {
    const response = await apiClient.get<Invoice>(`/api/invoices/${id}`);
    return response.data;
  },

  // Crear factura
  create: async (data: CreateInvoiceRequest): Promise<Invoice> => {
    const response = await apiClient.post<Invoice>('/api/invoices', data);
    return response.data;
  },

  // Actualizar factura
  update: async (id: number, data: Partial<CreateInvoiceRequest>): Promise<Invoice> => {
    const response = await apiClient.put<Invoice>(`/api/invoices/${id}`, data);
    return response.data;
  },

  // Eliminar factura
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/invoices/${id}`);
  },

  // Generar PDF
  generatePDF: async (id: number): Promise<Blob> => {
    const response = await apiClient.post(
      `/api/invoices/${id}/generate-pdf`,
      {},
      {
        responseType: 'blob',
      }
    );
    return response.data;
  },

  // Descargar PDF
  downloadPDF: async (id: number, invoiceNumber: string): Promise<void> => {
    const blob = await invoicesApi.generatePDF(id);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `invoice-${invoiceNumber}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};
```

---

## 🛠️ UTILITIES - Spring Boot 3 Compatible

### `/src/utils/spring-errors.ts`

```typescript
import { UseFormSetError } from 'react-hook-form';
import { AxiosError } from 'axios';

/**
 * Estructura de error de Spring Boot 3 con Bean Validation
 *
 * Cuando falla @Valid en el controller, Spring devuelve esta estructura
 */
export interface SpringValidationError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  errors?: Array<{
    field: string;           // Nombre del campo (ej: "items[0].quantity")
    defaultMessage: string;  // Mensaje de error
    objectName: string;      // Nombre del objeto (ej: "createInvoiceRequest")
    code: string;            // Código de error (ej: "NotNull", "Min")
    rejectedValue?: any;     // Valor rechazado
  }>;
}

/**
 * Alternativa: Algunos backends devuelven errores como mapa
 */
export interface SpringFieldErrors {
  [fieldName: string]: string;
}

/**
 * Helper para setear errores de Spring en React Hook Form
 *
 * @param error Error de Axios con respuesta de Spring
 * @param setError Función setError de React Hook Form
 */
export const setSpringErrors = (
  error: unknown,
  setError: UseFormSetError<any>
) => {
  if (!(error instanceof AxiosError)) return;

  const data = error.response?.data as SpringValidationError;

  // Caso 1: Spring Boot con lista de errores (Bean Validation)
  if (data?.errors && Array.isArray(data.errors)) {
    data.errors.forEach((err) => {
      setError(err.field, {
        type: 'server',
        message: err.defaultMessage,
      });
    });
    return;
  }

  // Caso 2: Mapa de errores { "fieldName": "message" }
  if (data && typeof data === 'object') {
    Object.entries(data).forEach(([field, message]) => {
      if (typeof message === 'string') {
        setError(field, {
          type: 'server',
          message,
        });
      }
    });
  }
};

/**
 * Helper para extraer mensaje de error general de Spring
 */
export const getSpringErrorMessage = (error: unknown): string => {
  if (!(error instanceof AxiosError)) {
    return 'Error desconocido';
  }

  const data = error.response?.data as SpringValidationError;

  // Mensaje principal del error
  if (data?.message) {
    return data.message;
  }

  // Si hay errores de validación, mostrar el primero
  if (data?.errors && data.errors.length > 0) {
    return data.errors[0].defaultMessage;
  }

  return error.message || 'Error en la petición';
};
```

### `/src/utils/formatters.ts`

```typescript
/**
 * Formateadores para datos de Spring Boot
 * Compatible con BigDecimal, LocalDateTime, LocalDate
 */

/**
 * Formatea montos (BigDecimal de Java → number en TS)
 *
 * IMPORTANTE: Este formateador es SOLO para visualización.
 * NO uses los valores formateados para cálculos.
 *
 * @param amount Monto como number
 * @param currency Código de moneda (ISO 4217)
 * @param locale Locale para formateo
 */
export const formatCurrency = (
  amount: number,
  currency: string = 'USD',
  locale: string = 'es-ES'
): string => {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
};

/**
 * Formatea número con separadores de miles
 *
 * @param value Número a formatear
 * @param decimals Cantidad de decimales (default: 2)
 */
export const formatNumber = (value: number, decimals: number = 2): string => {
  return new Intl.NumberFormat('es-ES', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
};

/**
 * Formatea fecha ISO-8601 de Spring Boot a formato legible
 *
 * Spring serializa LocalDateTime como: "2025-11-17T10:30:00"
 * Spring serializa LocalDate como: "2025-11-17"
 *
 * @param isoDate String ISO-8601 de Spring
 * @param includeTime Incluir hora en el formato
 */
export const formatDate = (
  isoDate: string,
  includeTime: boolean = false
): string => {
  const date = new Date(isoDate);

  if (includeTime) {
    return new Intl.DateTimeFormat('es-ES', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date);
  }

  return new Intl.DateTimeFormat('es-ES', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
};

/**
 * Convierte Date a formato ISO-8601 para enviar a Spring Boot
 *
 * Para LocalDateTime en Java: "2025-11-17T10:30:00"
 * Para LocalDate en Java: "2025-11-17"
 *
 * @param date Objeto Date de JavaScript
 * @param dateOnly Solo fecha (sin hora)
 */
export const toISODate = (date: Date, dateOnly: boolean = false): string => {
  if (dateOnly) {
    // LocalDate en Java: "2025-11-17"
    return date.toISOString().split('T')[0];
  }

  // LocalDateTime en Java: "2025-11-17T10:30:00"
  // Nota: Remover milisegundos y zona horaria
  return date.toISOString().split('.')[0];
};

/**
 * Formatea el estado de la factura (Enum de Java)
 */
export const formatInvoiceStatus = (status: string): string => {
  const statusMap: Record<string, string> = {
    DRAFT: 'Borrador',
    PENDING: 'Pendiente',
    PAID: 'Pagada',
    CANCELLED: 'Cancelada',
  };

  return statusMap[status] || status;
};

/**
 * Obtiene color para el estado de factura (útil para badges)
 */
export const getStatusColor = (
  status: string
): 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' => {
  const colorMap: Record<string, any> = {
    DRAFT: 'default',
    PENDING: 'warning',
    PAID: 'success',
    CANCELLED: 'error',
  };

  return colorMap[status] || 'default';
};
```

### `/src/utils/validators.ts`

```typescript
import { z } from 'zod';

/**
 * Validaciones que coinciden con Bean Validation de Spring Boot
 *
 * Esto asegura que las validaciones del frontend sean consistentes
 * con las del backend (@NotNull, @Size, @Min, @Max, etc.)
 */

/**
 * Schema de validación para Invoice Item
 * Coincide con: @Valid InvoiceItem en Java
 */
export const invoiceItemSchema = z.object({
  description: z
    .string()
    .min(1, 'La descripción es requerida')
    .max(500, 'La descripción no puede exceder 500 caracteres'),
  quantity: z
    .number()
    .min(1, 'La cantidad debe ser al menos 1')
    .max(9999, 'La cantidad no puede exceder 9999'),
  unitPrice: z
    .number()
    .min(0.01, 'El precio debe ser mayor a 0')
    .max(999999.99, 'El precio es demasiado alto'),
  taxRate: z
    .number()
    .min(0, 'La tasa de impuesto no puede ser negativa')
    .max(100, 'La tasa de impuesto no puede exceder 100%'),
});

/**
 * Schema de validación para crear Invoice
 * Coincide con: CreateInvoiceRequest en Java
 */
export const createInvoiceSchema = z.object({
  invoiceNumber: z
    .string()
    .min(1, 'El número de factura es requerido')
    .max(50, 'El número de factura no puede exceder 50 caracteres')
    .regex(/^[A-Z0-9-]+$/, 'Formato inválido (solo mayúsculas, números y guiones)'),
  companyId: z.number().min(1, 'Debe seleccionar una empresa'),
  clientId: z.number().min(1, 'Debe seleccionar un cliente'),
  issueDate: z.string().min(1, 'La fecha de emisión es requerida'),
  dueDate: z.string().min(1, 'La fecha de vencimiento es requerida'),
  items: z
    .array(invoiceItemSchema)
    .min(1, 'Debe agregar al menos un ítem')
    .max(100, 'No puede agregar más de 100 ítems'),
});

/**
 * Type inference de Zod schemas
 */
export type InvoiceItemFormData = z.infer<typeof invoiceItemSchema>;
export type CreateInvoiceFormData = z.infer<typeof createInvoiceSchema>;

/**
 * Validador personalizado para fechas
 * Asegura que dueDate >= issueDate
 */
export const validateDates = (issueDate: string, dueDate: string): boolean => {
  const issue = new Date(issueDate);
  const due = new Date(dueDate);
  return due >= issue;
};
```

### `/src/utils/constants.ts`

```typescript
/**
 * Constantes que coinciden con el backend Java
 */

/**
 * Estados de factura (debe coincidir con InvoiceStatus enum en Java)
 */
export const INVOICE_STATUS = {
  DRAFT: 'DRAFT',
  PENDING: 'PENDING',
  PAID: 'PAID',
  CANCELLED: 'CANCELLED',
} as const;

export type InvoiceStatus = (typeof INVOICE_STATUS)[keyof typeof INVOICE_STATUS];

/**
 * Roles de usuario (debe coincidir con UserRole enum en Java)
 */
export const USER_ROLES = {
  ADMIN: 'ROLE_ADMIN',
  USER: 'ROLE_USER',
  CLIENT: 'ROLE_CLIENT',
} as const;

/**
 * Configuración de paginación (debe coincidir con defaults de Spring Boot)
 */
export const PAGINATION = {
  DEFAULT_PAGE: 0,        // Spring usa 0-based
  DEFAULT_SIZE: 20,       // Default en Spring Boot
  MAX_SIZE: 100,          // Máximo permitido
} as const;

/**
 * Configuración de moneda
 */
export const CURRENCY = {
  CODE: 'USD',
  SYMBOL: '$',
  LOCALE: 'es-ES',
} as const;
```

---

## 🗃️ ESTADO GLOBAL CON ZUSTAND

### `/src/store/authStore.ts`

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: User) => void;
  clearAuth: () => void;
  hasRole: (role: string) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      isAuthenticated: false,

      setAuth: (token, user) => {
        set({ token, user, isAuthenticated: true });
      },

      clearAuth: () => {
        set({ token: null, user: null, isAuthenticated: false });
      },

      hasRole: (role) => {
        const { user } = get();
        return user?.roles.includes(role) ?? false;
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);
```

---

## 🎣 CUSTOM HOOKS CON REACT QUERY

### `/src/features/invoices/hooks/useInvoices.ts`

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { invoicesApi, InvoiceListParams, CreateInvoiceRequest } from '@/api/invoices.api';
import { toast } from 'react-toastify';

// Listar facturas
export const useInvoices = (params?: InvoiceListParams) => {
  return useQuery({
    queryKey: ['invoices', params],
    queryFn: () => invoicesApi.list(params),
  });
};

// Obtener factura por ID
export const useInvoice = (id: number) => {
  return useQuery({
    queryKey: ['invoice', id],
    queryFn: () => invoicesApi.getById(id),
    enabled: !!id,
  });
};

// Crear factura
export const useCreateInvoice = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateInvoiceRequest) => invoicesApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      toast.success('Factura creada exitosamente');
    },
    onError: (error) => {
      toast.error(`Error al crear factura: ${error.message}`);
    },
  });
};

// Eliminar factura
export const useDeleteInvoice = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => invoicesApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      toast.success('Factura eliminada');
    },
    onError: (error) => {
      toast.error(`Error al eliminar factura: ${error.message}`);
    },
  });
};

// Generar PDF
export const useGeneratePDF = () => {
  return useMutation({
    mutationFn: ({ id, invoiceNumber }: { id: number; invoiceNumber: string }) =>
      invoicesApi.downloadPDF(id, invoiceNumber),
    onSuccess: () => {
      toast.success('PDF descargado');
    },
    onError: (error) => {
      toast.error(`Error al generar PDF: ${error.message}`);
    },
  });
};
```

---

## 🛣️ ROUTING CON PROTECCIÓN

### `/src/routes/PrivateRoute.tsx`

```typescript
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

interface PrivateRouteProps {
  requiredRole?: string;
}

export const PrivateRoute: React.FC<PrivateRouteProps> = ({ requiredRole }) => {
  const { isAuthenticated, hasRole } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && !hasRole(requiredRole)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
};
```

### `/src/routes/AppRoutes.tsx`

```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { PrivateRoute } from './PrivateRoute';

// Auth
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';

// Dashboard
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage';

// Invoices
import { InvoiceListPage } from '@/features/invoices/pages/InvoiceListPage';
import { InvoiceDetailPage } from '@/features/invoices/pages/InvoiceDetailPage';
import { InvoiceCreatePage } from '@/features/invoices/pages/InvoiceCreatePage';

// Users (Admin)
import { UserListPage } from '@/features/users/pages/UserListPage';

// Layout
import { MainLayout } from '@/components/layout/MainLayout';

export const AppRoutes = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Private routes */}
        <Route element={<PrivateRoute />}>
          <Route element={<MainLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />

            {/* Invoices */}
            <Route path="/invoices" element={<InvoiceListPage />} />
            <Route path="/invoices/create" element={<InvoiceCreatePage />} />
            <Route path="/invoices/:id" element={<InvoiceDetailPage />} />

            {/* Users (Admin only) */}
            <Route element={<PrivateRoute requiredRole="ROLE_ADMIN" />}>
              <Route path="/users" element={<UserListPage />} />
            </Route>
          </Route>
        </Route>

        {/* Default redirect */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
};
```

---

## 📄 EJEMPLO DE PÁGINA COMPLETA

### `/src/features/invoices/pages/InvoiceListPage.tsx`

```typescript
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  TextField,
  MenuItem,
} from '@mui/material';
import {
  Add as AddIcon,
  Visibility as ViewIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
} from '@mui/icons-material';
import { useInvoices, useDeleteInvoice, useGeneratePDF } from '../hooks/useInvoices';

const statusColors = {
  DRAFT: 'default',
  PENDING: 'warning',
  PAID: 'success',
  CANCELLED: 'error',
} as const;

export const InvoiceListPage: React.FC = () => {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [statusFilter, setStatusFilter] = useState<string>('');

  const { data, isLoading, error } = useInvoices({ page, size, status: statusFilter || undefined });
  const deleteMutation = useDeleteInvoice();
  const generatePDFMutation = useGeneratePDF();

  const handleDelete = async (id: number) => {
    if (window.confirm('¿Estás seguro de eliminar esta factura?')) {
      await deleteMutation.mutateAsync(id);
    }
  };

  const handleDownloadPDF = (id: number, invoiceNumber: string) => {
    generatePDFMutation.mutate({ id, invoiceNumber });
  };

  if (isLoading) return <div>Cargando...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Facturas</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/invoices/create')}
        >
          Nueva Factura
        </Button>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            select
            label="Estado"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            sx={{ minWidth: 200 }}
          >
            <MenuItem value="">Todos</MenuItem>
            <MenuItem value="DRAFT">Borrador</MenuItem>
            <MenuItem value="PENDING">Pendiente</MenuItem>
            <MenuItem value="PAID">Pagada</MenuItem>
            <MenuItem value="CANCELLED">Cancelada</MenuItem>
          </TextField>
        </CardContent>
      </Card>

      <TableContainer component={Card}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Número</TableCell>
              <TableCell>Cliente</TableCell>
              <TableCell>Fecha</TableCell>
              <TableCell align="right">Total</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell align="right">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.content.map((invoice) => (
              <TableRow key={invoice.id}>
                <TableCell>{invoice.invoiceNumber}</TableCell>
                <TableCell>Cliente #{invoice.clientId}</TableCell>
                <TableCell>{new Date(invoice.issueDate).toLocaleDateString()}</TableCell>
                <TableCell align="right">
                  ${invoice.totalAmount.toFixed(2)}
                </TableCell>
                <TableCell>
                  <Chip
                    label={invoice.status}
                    color={statusColors[invoice.status]}
                    size="small"
                  />
                </TableCell>
                <TableCell align="right">
                  <IconButton onClick={() => navigate(`/invoices/${invoice.id}`)}>
                    <ViewIcon />
                  </IconButton>
                  <IconButton onClick={() => handleDownloadPDF(invoice.id, invoice.invoiceNumber)}>
                    <DownloadIcon />
                  </IconButton>
                  <IconButton onClick={() => handleDelete(invoice.id)}>
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};
```

---

## 📦 PACKAGE.JSON COMPLETO

```json
{
  "name": "invoices-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "test": "vitest",
    "test:ui": "vitest --ui",
    "test:coverage": "vitest --coverage"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "axios": "^1.6.2",
    "zustand": "^4.4.7",
    "@tanstack/react-query": "^5.12.2",
    "@mui/material": "^5.15.0",
    "@mui/icons-material": "^5.15.0",
    "@emotion/react": "^11.11.1",
    "@emotion/styled": "^11.11.0",
    "react-hook-form": "^7.48.2",
    "zod": "^3.22.4",
    "@hookform/resolvers": "^3.3.2",
    "date-fns": "^2.30.0",
    "react-toastify": "^9.1.3",
    "recharts": "^2.10.3",
    "jwt-decode": "^4.0.0",
    "currency.js": "^2.0.4"
  },
  "devDependencies": {
    "@types/react": "^18.2.43",
    "@types/react-dom": "^18.2.17",
    "@typescript-eslint/eslint-plugin": "^6.14.0",
    "@typescript-eslint/parser": "^6.14.0",
    "@vitejs/plugin-react": "^4.2.1",
    "typescript": "^5.2.2",
    "vite": "^5.0.8",
    "vitest": "^1.0.4",
    "@testing-library/react": "^14.1.2",
    "@testing-library/jest-dom": "^6.1.5",
    "eslint": "^8.55.0",
    "eslint-plugin-react-hooks": "^4.6.0",
    "eslint-plugin-react-refresh": "^0.4.5"
  }
}
```

---

## ⚙️ CONFIGURACIÓN DE VITE

### `vite.config.ts`

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### `tsconfig.json`

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

---

## 🔒 VARIABLES DE ENTORNO

### `.env.development`

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=Invoices App
```

### `.env.production`

```bash
VITE_API_BASE_URL=https://api.tudominio.com
VITE_APP_NAME=Invoices App
```

---

## 🚀 COMANDOS PARA EMPEZAR

```bash
# 1. Crear proyecto con Vite
npm create vite@latest invoices-frontend -- --template react-ts

# 2. Entrar al proyecto
cd invoices-frontend

# 3. Instalar dependencias
npm install

# 4. Instalar dependencias adicionales
npm install axios zustand @tanstack/react-query @mui/material @mui/icons-material @emotion/react @emotion/styled react-router-dom react-hook-form zod @hookform/resolvers date-fns react-toastify recharts jwt-decode

# 5. Configurar alias @ en tsconfig
# (Ya incluido arriba)

# 6. Iniciar servidor de desarrollo
npm run dev

# Abrir en navegador: http://localhost:3000
```

---

## 📱 PANTALLAS A IMPLEMENTAR (PRIORIDAD)

### Sprint 1 (MVP - 2 semanas)
1. ✅ **LoginPage** - Autenticación
2. ✅ **DashboardPage** - Home con estadísticas
3. ✅ **InvoiceListPage** - Lista de facturas
4. ✅ **InvoiceDetailPage** - Ver detalle y descargar PDF

### Sprint 2 (Creación - 1 semana)
5. ✅ **InvoiceCreateWizard** - Wizard de creación de facturas (5 pasos)

### Sprint 3 (Admin - 1 semana)
6. ✅ **UserListPage** - Gestión de usuarios (admin)
7. ✅ **ProfilePage** - Ver/editar perfil

---

## 🎨 COMPONENTES REUTILIZABLES CLAVE

1. **InvoiceStatusBadge** - Badge de estado de factura
2. **InvoiceCard** - Card para listar facturas
3. **DataTable** - Tabla con paginación y ordenamiento
4. **WizardStepper** - Stepper para wizard de creación
5. **ConfirmDialog** - Modal de confirmación
6. **PDFViewer** - Visor de PDFs
7. **LoadingSpinner** - Spinner de carga
8. **ErrorBoundary** - Manejo de errores global

---

## ✅ CHECKLIST DE DESARROLLO

### Configuración Inicial
- [ ] Crear proyecto con Vite
- [ ] Instalar dependencias
- [ ] Configurar Axios + interceptores
- [ ] Configurar Zustand store
- [ ] Configurar React Query
- [ ] Configurar routing

### Autenticación
- [ ] LoginPage
- [ ] RegisterPage
- [ ] PrivateRoute
- [ ] Auth store
- [ ] JWT handling

### Dashboard
- [ ] Layout principal
- [ ] Sidebar navigation
- [ ] Header con user menu
- [ ] Stats cards
- [ ] Recent invoices list

### Facturas
- [ ] Invoice list con filtros
- [ ] Invoice detail
- [ ] Invoice wizard (5 pasos)
- [ ] PDF generation
- [ ] Status badges

### Testing
- [ ] Tests unitarios (Vitest)
- [ ] Tests de integración
- [ ] E2E tests (opcional)

---

## 🔗 RECURSOS ÚTILES

- **Backend Docs:** Ver `GUIA_UX_UI_FRONTEND.md` en el repo del backend
- **API Gateway:** http://localhost:8080
- **Swagger UI:** http://localhost:8081/swagger-ui.html (Invoice Service)
- **Material-UI Docs:** https://mui.com/
- **React Query Docs:** https://tanstack.com/query/latest
- **Zustand Docs:** https://docs.pmnd.rs/zustand

---

## ⚠️ NOTAS IMPORTANTES - SPRING BOOT 3 + JAVA 21

### 1. BigDecimal vs Number (CRÍTICO)

**Problema:**
- Java usa `BigDecimal` para montos (precisión exacta)
- JavaScript usa `number` (IEEE 754 - punto flotante)
- Ejemplo: `0.1 + 0.2 = 0.30000000000000004` ❌

**Solución implementada:**
```typescript
// ✅ CORRECTO - Solo visualización en frontend
import currency from 'currency.js';

const total = currency(invoice.subtotal)
  .add(invoice.taxAmount)
  .format(); // "$1,500.00"

// ✅ CORRECTO - Confiar en cálculos del backend
const createInvoice = (data: CreateInvoiceRequest) => {
  // Backend calcula subtotal, taxAmount, totalAmount
  return invoicesApi.create(data);
};

// ❌ INCORRECTO - NO hacer cálculos complejos en frontend
const calculateTotal = () => {
  return items.reduce((sum, item) =>
    sum + (item.unitPrice * item.quantity * (1 + item.taxRate/100)), 0
  ); // Puede tener errores de precisión
};
```

**Regla de oro:**
- **Visualización:** Usar `number` con `Intl.NumberFormat` o `currency.js`
- **Cálculos:** Siempre en el **backend (Java BigDecimal)**

---

### 2. Paginación de Spring Data (CRÍTICO)

**Spring Boot 3 devuelve:**
```json
{
  "content": [...],
  "pageable": { "pageNumber": 0, "pageSize": 20, ... },
  "totalPages": 5,
  "totalElements": 100,
  "number": 0,  // ← Página actual (0-based)
  "size": 20,
  "last": false,
  "first": true
}
```

**Implementación correcta:**
```typescript
// ✅ Usar interfaz PagedResponse exacta (ya implementada arriba)
const { data, isLoading } = useInvoices({
  page: 0,        // Spring usa 0-based
  size: 20,
  sortBy: 'invoiceNumber',
  sortDir: 'desc'
});

// Acceder a datos
console.log(data.content);           // Array de facturas
console.log(data.number);            // Página actual (0-based)
console.log(data.totalElements);     // Total de elementos
```

---

### 3. Fechas ISO-8601 (IMPORTANTE)

**Spring Boot serializa:**
- `LocalDate` → `"2025-11-17"`
- `LocalDateTime` → `"2025-11-17T10:30:00"`
- `Instant` → `"2025-11-17T10:30:00Z"`

**Enviar al backend:**
```typescript
import { toISODate } from '@/utils/formatters';

const formData = {
  issueDate: toISODate(new Date(), true),  // "2025-11-17"
  dueDate: toISODate(new Date(), true),    // "2025-11-17"
};
```

**Visualizar en frontend:**
```typescript
import { formatDate } from '@/utils/formatters';

const displayDate = formatDate(invoice.issueDate);        // "17/11/2025"
const displayDateTime = formatDate(invoice.createdAt, true); // "17/11/2025 10:30:00"
```

---

### 4. Manejo de Errores de Bean Validation

**Spring Boot lanza `MethodArgumentNotValidException`:**
```json
{
  "timestamp": "2025-11-17T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "items[0].quantity",
      "defaultMessage": "must be greater than or equal to 1",
      "code": "Min"
    }
  ]
}
```

**Implementación en formularios:**
```typescript
import { setSpringErrors } from '@/utils/spring-errors';
import { useForm } from 'react-hook-form';

const { setError } = useForm();

try {
  await createInvoice(data);
} catch (error) {
  // Mapear errores de Spring a React Hook Form
  setSpringErrors(error, setError);

  // Los errores aparecen automáticamente en los campos
  // field: "items[0].quantity" → se marca el input correspondiente
}
```

---

### 5. IDs Long de Java vs Number de TS

**Java Long:**
- Rango: `-9,223,372,036,854,775,808` a `9,223,372,036,854,775,807`

**JavaScript number:**
- Precisión: `±9,007,199,254,740,991` (2^53 - 1)

**Solución:**
```typescript
// ✅ SAFE - IDs típicos de base de datos (<9 trillones)
interface Invoice {
  id: number;  // Compatible con Java Long
}

// ❌ Si usas IDs muy grandes o UUIDs, usa string:
interface Invoice {
  id: string;  // UUID: "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 6. Enums de Java

**Backend Java:**
```java
public enum InvoiceStatus {
    DRAFT, PENDING, PAID, CANCELLED
}
```

**Frontend TypeScript:**
```typescript
// ✅ CORRECTO - String literal types
type InvoiceStatus = 'DRAFT' | 'PENDING' | 'PAID' | 'CANCELLED';

// ✅ O usar const object
export const INVOICE_STATUS = {
  DRAFT: 'DRAFT',
  PENDING: 'PENDING',
  PAID: 'PAID',
  CANCELLED: 'CANCELLED',
} as const;

type InvoiceStatus = (typeof INVOICE_STATUS)[keyof typeof INVOICE_STATUS];
```

---

### 7. Validaciones Consistentes

**Backend (Java):**
```java
@NotBlank(message = "Invoice number is required")
@Size(max = 50, message = "Invoice number cannot exceed 50 characters")
private String invoiceNumber;

@Min(value = 1, message = "Quantity must be at least 1")
private Integer quantity;
```

**Frontend (TypeScript - Zod):**
```typescript
const schema = z.object({
  invoiceNumber: z
    .string()
    .min(1, 'Invoice number is required')
    .max(50, 'Invoice number cannot exceed 50 characters'),
  quantity: z
    .number()
    .min(1, 'Quantity must be at least 1'),
});
```

**Ventaja:** Si el usuario pasa validaciones del frontend, también pasará las del backend.

---

### 8. Ejemplo Completo de Integración

```typescript
// Crear factura con manejo completo de Spring Boot 3
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { createInvoiceSchema } from '@/utils/validators';
import { setSpringErrors, getSpringErrorMessage } from '@/utils/spring-errors';
import { formatCurrency, toISODate } from '@/utils/formatters';
import { toast } from 'react-toastify';

const CreateInvoiceForm = () => {
  const { register, handleSubmit, setError, formState: { errors } } = useForm({
    resolver: zodResolver(createInvoiceSchema),
  });

  const createMutation = useCreateInvoice();

  const onSubmit = async (data: any) => {
    try {
      // Convertir fechas a formato ISO-8601
      const payload = {
        ...data,
        issueDate: toISODate(new Date(data.issueDate), true),
        dueDate: toISODate(new Date(data.dueDate), true),
      };

      const invoice = await createMutation.mutateAsync(payload);

      // Mostrar total formateado (BigDecimal → number → formatted string)
      toast.success(`Factura creada: ${formatCurrency(invoice.totalAmount)}`);

    } catch (error) {
      // Mapear errores de Spring Boot a campos del formulario
      setSpringErrors(error, setError);

      // Mostrar mensaje de error general
      toast.error(getSpringErrorMessage(error));
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {/* Inputs con errores mapeados automáticamente */}
    </form>
  );
};
```

---

## 🎯 CONCLUSIÓN

Esta arquitectura te proporciona:

✅ **Type-safe** con TypeScript
✅ **Escalable** con features modulares
✅ **Performante** con React Query caching
✅ **Testeable** con Vitest
✅ **Mantenible** con Clean Code
✅ **Enterprise-ready** con MUI
✅ **100% compatible con Spring Boot 3 + Java 21**

**¡Listo para empezar a construir! 🚀**
