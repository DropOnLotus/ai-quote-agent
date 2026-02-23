package com.guidewire.ai.agent.model;

import com.guidewire.ai.agent.engine.CoveragePermutationEngine;
import java.util.List;

// ─────────────────────────────────────────────────────────────
// INPUT MODELS
// ─────────────────────────────────────────────────────────────

public class CustomerInfo {
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private Address address;
    private String email;
    private String phoneNumber;
    private String lineOfBusiness;
    private String desiredEffectiveDate;
    private List<PADriver> drivers;
    private List<PAVehicle> vehicles;

    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String v) { this.dateOfBirth = v; }
    public Address getAddress() { return address; }
    public void setAddress(Address v) { this.address = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String v) { this.phoneNumber = v; }
    public String getLineOfBusiness() { return lineOfBusiness; }
    public void setLineOfBusiness(String v) { this.lineOfBusiness = v; }
    public String getDesiredEffectiveDate() { return desiredEffectiveDate; }
    public void setDesiredEffectiveDate(String v) { this.desiredEffectiveDate = v; }
    public List<PADriver> getDrivers() { return drivers; }
    public void setDrivers(List<PADriver> v) { this.drivers = v; }
    public List<PAVehicle> getVehicles() { return vehicles; }
    public void setVehicles(List<PAVehicle> v) { this.vehicles = v; }
}
