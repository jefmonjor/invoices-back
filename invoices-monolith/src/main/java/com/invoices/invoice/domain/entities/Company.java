package com.invoices.invoice.domain.entities;

import com.invoices.shared.domain.validation.ValidNif;
import com.invoices.shared.domain.validation.ValidIban;

/**
 * Company domain entity - represents the issuer (emisor) of invoices.
 * Pure domain object with NO framework dependencies.
 */
public class Company {
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

    @ValidIban
    private final String iban; // IBAN para pagos

    private final String lastHash;
    private final String certRef;
    private final String certPassword;

    private final java.time.LocalDateTime createdAt;
    private final java.time.LocalDateTime updatedAt;

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
            String iban) {
        this(id, businessName, taxId, address, city, postalCode, province, "España", phone, email, iban, null, null);
    }

    public Company(
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
            String iban,
            String lastHash,
            String certRef,
            String certPassword,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        validateMandatoryFields(businessName, taxId);

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
        this.iban = iban;
        this.lastHash = lastHash;
        this.certRef = certRef;
        this.certPassword = certPassword;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Constructor override for backward compatibility
    public Company(
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
            String iban,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        this(id, businessName, taxId, address, city, postalCode, province, country, phone, email, iban, null, null,
                null, createdAt, updatedAt);
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

    public String getIban() {
        return iban;
    }

    public String getLastHash() {
        return lastHash;
    }

    public String getCertRef() {
        return certRef;
    }

    public String getCertPassword() {
        return certPassword;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Company withDetails(
            String businessName,
            String address,
            String city,
            String postalCode,
            String province,
            String phone,
            String email) {
        return new Company(
                this.id,
                businessName,
                this.taxId, // Tax ID is immutable
                address,
                city,
                postalCode,
                province,
                this.country,
                phone,
                email,
                this.iban,
                this.lastHash,
                this.certRef,
                this.certPassword,
                this.createdAt,
                this.updatedAt);
    }

    public void setLastHash(String lastHash) {
        // This method is needed for the service to update the hash
        // Ideally Company should be immutable, but for now we follow the pattern
        // Or we should return a new Company instance
        // Since fields are final, we can't set it.
        // We need to change fields to non-final or provide a withLastHash method.
        // Given the existing code structure, let's add a withLastHash method.
        throw new UnsupportedOperationException("Company is immutable, use withLastHash");
    }

    public Company withLastHash(String lastHash) {
        return new Company(
                this.id,
                this.businessName,
                this.taxId,
                this.address,
                this.city,
                this.postalCode,
                this.province,
                this.country,
                this.phone,
                this.email,
                this.iban,
                lastHash,
                this.certRef,
                this.certPassword,
                this.createdAt,
                java.time.LocalDateTime.now());
    }

    public Company withCertificateData(String certRef, String certPassword) {
        return new Company(
                this.id,
                this.businessName,
                this.taxId,
                this.address,
                this.city,
                this.postalCode,
                this.province,
                this.country,
                this.phone,
                this.email,
                this.iban,
                this.lastHash,
                certRef,
                certPassword,
                this.createdAt,
                java.time.LocalDateTime.now());
    }
}
