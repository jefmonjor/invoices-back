package com.invoices.verifactu.infrastructure.controllers;

import com.invoices.verifactu.domain.ports.VerifactuPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceVerifactuController.class)
class InvoiceVerifactuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VerifactuPort verifactuPort;

    // Mock other dependencies required by SecurityConfig if any,
    // but usually @WebMvcTest loads only the controller.
    // However, SecurityConfig might require UserDetailsService and
    // JwtAuthenticationFilter.
    @MockBean
    private com.invoices.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockBean
    private com.invoices.security.JwtUtil jwtUtil;

    @MockBean(name = "companySecurity")
    private com.invoices.security.CompanySecurity companySecurity;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sendInvoice_ShouldReturnOk_WhenUserIsAdmin() throws Exception {
        Long invoiceId = 1L;
        Long companyId = 100L;

        // Mock security check if needed, but WithMockUser(roles="ADMIN") might bypass
        // the @companySecurity check
        // if the expression is "hasRole('ADMIN') or ...".
        // Since we are ADMIN, the first part is true.

        mockMvc.perform(post("/api/verifactu/invoices/{invoiceId}/send", invoiceId)
                .param("companyId", String.valueOf(companyId))
                .with(csrf()))
                .andExpect(status().isOk());

        verify(verifactuPort).sendInvoice(companyId, invoiceId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void sendInvoice_ShouldReturnOk_WhenUserHasCompanyAccess() throws Exception {
        Long invoiceId = 1L;
        Long companyId = 100L;

        // Mock company security check
        org.mockito.Mockito.when(companySecurity.hasCompanyAccess(companyId, "ADMIN")).thenReturn(true);

        mockMvc.perform(post("/api/verifactu/invoices/{invoiceId}/send", invoiceId)
                .param("companyId", String.valueOf(companyId))
                .with(csrf()))
                .andExpect(status().isOk());

        verify(verifactuPort).sendInvoice(companyId, invoiceId);
    }
}
