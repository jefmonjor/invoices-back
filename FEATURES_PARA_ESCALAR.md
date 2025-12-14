# Features Desactivadas para MVP

> Este documento describe las features avanzadas que fueron simplificadas para MVP.
> Úsalo como guía para re-implementar cuando la app necesite escalar.

---

## 1. WebSocket Real-Time (VeriFactu)

### Qué hacía:
- Notificaciones en tiempo real cuando VeriFactu responde
- STOMP sobre WebSocket con SockJS fallback

### Para re-implementar:
```java
// Backend: WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }
}
```

```typescript
// Frontend: websocket.service.ts
import { Client } from '@stomp/stompjs';
const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    onConnect: () => { /*...*/ }
});
```

### Dependencias:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

---

## 2. Redis Streams (Event Queue)

### Qué hacía:
- Cola de eventos para procesar VeriFactu async
- Garantizaba orden y no pérdida de mensajes

### Para re-implementar:
```java
redisTemplate.opsForStream().add("verifactu-queue", message);
```

---

## 3. GlobalSearch API

### Qué hacía:
- Búsqueda unificada Cmd+K
- Endpoint: GET /api/search?query=X&companyId=Y

### Para re-implementar:
```java
@GetMapping("/api/search")
public List<SearchResult> search(@RequestParam String query, @RequestParam Long companyId) {
    // Search in invoices, clients
}
```

---

## 4. Circuit Breaker (Resilience4j)

### Para re-implementar:
```java
@CircuitBreaker(name = "minio", fallbackMethod = "fallback")
public void storeFile(...) { }
```

---

## Cuándo Escalar

| Señal | Feature |
|-------|---------|
| >100 usuarios | WebSocket |
| >10 facturas/seg | Redis Streams |
| >5000 registros | GlobalSearch |
| Integraciones failing | CircuitBreaker |
