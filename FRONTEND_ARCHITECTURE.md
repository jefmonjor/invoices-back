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

export interface InvoiceItem {
  description: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
}

export interface CreateInvoiceRequest {
  invoiceNumber: string;
  companyId: number;
  clientId: number;
  issueDate: string;
  dueDate: string;
  items: InvoiceItem[];
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  companyId: number;
  clientId: number;
  issueDate: string;
  dueDate: string;
  status: 'DRAFT' | 'PENDING' | 'PAID' | 'CANCELLED';
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  items: InvoiceItem[];
  createdAt: string;
  updatedAt: string;
}

export interface InvoiceListParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  status?: string;
  clientId?: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

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
    "jwt-decode": "^4.0.0"
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

## 🎯 CONCLUSIÓN

Esta arquitectura te proporciona:

✅ **Type-safe** con TypeScript
✅ **Escalable** con features modulares
✅ **Performante** con React Query caching
✅ **Testeable** con Vitest
✅ **Mantenible** con Clean Code
✅ **Enterprise-ready** con MUI

**¡Listo para empezar a construir! 🚀**
