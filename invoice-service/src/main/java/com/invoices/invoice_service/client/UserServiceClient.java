package com.invoices.invoice_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Obtiene un usuario por su ID desde el user-service
     * @param id ID del usuario
     * @return UserDTO con la información del usuario
     */
    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
