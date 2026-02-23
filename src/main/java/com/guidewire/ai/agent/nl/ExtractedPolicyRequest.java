package com.guidewire.ai.agent.nl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * POJO holding up to 14 fields extracted from a natural-language insurance request.
 * All fields are nullable; missing fields drive the follow-up question sequence.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedPolicyRequest {

    // ── Address ──────────────────────────────────────────────────────
    private String addressLine1;
    private String city;
    private String state;        // 2-letter code, e.g. "CT"
    private String postalCode;   // 5-digit ZIP

    // ── Vehicle ──────────────────────────────────────────────────────
    private Integer vehicleYear;
    private String vehicleMake;
    private String vehicleModel;
    private String licensePlate;
    private String licenseState; // plate state; defaults to state if absent

    // ── Driver / Customer ────────────────────────────────────────────
    private String firstName;
    private String lastName;
    private String dateOfBirth;  // expected in MM/DD/YYYY from NL parser
    private String licenseNumber;
    private String email;        // optional; defaults to "noreply@placeholder.com"

    // ── Getters / Setters ────────────────────────────────────────────

    public String getAddressLine1()              { return addressLine1; }
    public void   setAddressLine1(String v)      { this.addressLine1 = v; }

    public String getCity()                      { return city; }
    public void   setCity(String v)              { this.city = v; }

    public String getState()                     { return state; }
    public void   setState(String v)             { this.state = v; }

    public String getPostalCode()                { return postalCode; }
    public void   setPostalCode(String v)        { this.postalCode = v; }

    public Integer getVehicleYear()              { return vehicleYear; }
    public void    setVehicleYear(Integer v)     { this.vehicleYear = v; }

    public String getVehicleMake()               { return vehicleMake; }
    public void   setVehicleMake(String v)       { this.vehicleMake = v; }

    public String getVehicleModel()              { return vehicleModel; }
    public void   setVehicleModel(String v)      { this.vehicleModel = v; }

    public String getLicensePlate()              { return licensePlate; }
    public void   setLicensePlate(String v)      { this.licensePlate = v; }

    public String getLicenseState()              { return licenseState; }
    public void   setLicenseState(String v)      { this.licenseState = v; }

    public String getFirstName()                 { return firstName; }
    public void   setFirstName(String v)         { this.firstName = v; }

    public String getLastName()                  { return lastName; }
    public void   setLastName(String v)          { this.lastName = v; }

    public String getDateOfBirth()               { return dateOfBirth; }
    public void   setDateOfBirth(String v)       { this.dateOfBirth = v; }

    public String getLicenseNumber()             { return licenseNumber; }
    public void   setLicenseNumber(String v)     { this.licenseNumber = v; }

    public String getEmail()                     { return email; }
    public void   setEmail(String v)             { this.email = v; }
}
