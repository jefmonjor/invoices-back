package com.invoices.security.infrastructure.interceptor;

import com.invoices.security.JwtUtil;
import com.invoices.security.context.CompanyContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanySecurityInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Long companyId = jwtUtil.extractCompanyId(token);
                if (companyId != null) {
                    CompanyContext.setCompanyId(companyId);
                    log.debug("Set company context to: {}", companyId);
                }
            } catch (Exception e) {
                log.warn("Failed to extract company ID from token: {}", e.getMessage());
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        CompanyContext.clear();
    }
}
