package com.invoices.invoice.domain.entities;

import com.invoices.shared.domain.validation.ValidNif;

/**
 * Client domain entity - represents the customer/client.
 * Pure domain object with NO framework dependencies.
 */
public class Client {
    private final Long id;
    private final String businessName; // Razón Social

    @ValidNif
    private final String taxId; // CIF/NIF
    private final String address; // Dirección completa
    private final String city;
    private final String postalCode;
    private final String province;
    private final String country; // País
    private final String phone;
    private final String email;
    private final Long companyId;

    public Client(
            Long id,
            String businessName,
            String taxId,
            String address,
            String city,
            String postalCode,
            String province,
            String country,
            String phone,
            String email,
            Long companyId) {
        validateMandatoryFields(businessName, taxId, address);

        this.id = id;
        this.businessName = businessName;
        this.taxId = taxId;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
        this.province = province;
        this.country = country != null ? country : "España";
        this.phone = phone;
        this.email = email;
        this.companyId = companyId;
    }

    // Overloaded constructor for backward compatibility or simpler instantiation
    public Client(
            Long id,
            String businessName,
            String taxId,
            String address,
            String city,
            String postalCode,
            String province,
            String phone,
            String email,
            Long companyId) {
        this(id, businessName, taxId, address, city, postalCode, province, "España", phone, email, companyId);
    }

    // Note: Removed short constructor - address is now mandatory

    private void validateMandatoryFields(String businessName, String taxId, String address) {
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new IllegalArgumentException("Business name cannot be null or empty");
        }
        if (taxId == null || taxId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tax ID cannot be null or empty");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null)
            sb.append(address);
        if (postalCode != null || city != null) {
            if (sb.length() > 0)
                sb.append(", ");
            if (postalCode != null)
                sb.append(postalCode);
            if (city != null)
                sb.append(" ").append(city);
        }
        if (province != null) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(province);
        }
        if (country != null) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(country);
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

    public String getCountry() {
        return country;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Client withBusinessName(String businessName) {
        return new Client(this.id, businessName, this.taxId, this.address, this.city, this.postalCode, this.province,
                this.country, this.phone, this.email, this.companyId);
    }

    public Client withEmail(String email) {
        return new Client(this.id, this.businessName, this.taxId, this.address, this.city, this.postalCode,
                this.province, this.country, this.phone, email, this.companyId);
    }

    public Client withTaxId(String taxId) {
        return new Client(this.id, this.businessName, taxId, this.address, this.city, this.postalCode,
                this.province, this.country, this.phone, this.email, this.companyId);
    }
}
