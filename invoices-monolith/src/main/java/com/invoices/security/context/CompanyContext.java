package com.invoices.security.context;

public class CompanyContext {
    private static final ThreadLocal<Long> currentCompanyId = new ThreadLocal<>();

    public static void setCompanyId(Long companyId) {
        currentCompanyId.set(companyId);
    }

    public static Long getCompanyId() {
        return currentCompanyId.get();
    }

    public static void clear() {
        currentCompanyId.remove();
    }
}
