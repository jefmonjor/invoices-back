package com.invoices.invoice.domain.entities;

/**
 * Company domain entity - represents the issuer (emisor) of invoices.
 * Pure domain object with NO framework dependencies.
 */
public class Company {
    private final Long id;
    private final String businessName;  // Razón Social
    private final String taxId;         // CIF/NIF
    private final String address;       // Dirección completa
    private final String city;
    private final String postalCode;
    private final String province;
    private final String phone;
    private final String email;
    private final String iban;          // IBAN para pagos

    public Company(
        Long id,
        String businessName,
        String taxId,
        String address,
        String city,
        String postalCode,
        String province,
        String phone,
        String email,
        String iban
    ) {
        validateMandatoryFields(businessName, taxId);

        this.id = id;
        this.businessName = businessName;
        this.taxId = taxId;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
        this.province = province;
        this.phone = phone;
        this.email = email;
        this.iban = iban;
    }

    private void validateMandatoryFields(String businessName, String taxId) {
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new IllegalArgumentException("Business name cannot be null or empty");
        }
        if (taxId == null || taxId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tax ID cannot be null or empty");
        }
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null) sb.append(address);
        if (postalCode != null || city != null) {
            if (sb.length() > 0) sb.append(", ");
            if (postalCode != null) sb.append(postalCode);
            if (city != null) sb.append(" ").append(city);
        }
        if (province != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(province);
        }
        return sb.toString();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getProvince() {
        return province;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getIban() {
        return iban;
    }
}
