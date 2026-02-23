package com.guidewire.ai.agent.model;

public class PAVehicle {
    private String vin;
    private Integer year;
    private String make;
    private String model;
    private String usage;
    private String licenseState;
    private String licensePlate;
    private Double vehicleCost;
    private Double statedValue;
    private Boolean leasedOrRented;
    private String leaseLength;
    private Integer annualMileage;
    private Integer commutingMilesOneWay;
    private Boolean absPresent;
    private Boolean passiveRestraintSys;
    private Boolean antiTheft;
    private String primaryDriverLicense;

    public String getVin() { return vin; }
    public void setVin(String v) { this.vin = v; }
    public Integer getYear() { return year; }
    public void setYear(Integer v) { this.year = v; }
    public String getMake() { return make; }
    public void setMake(String v) { this.make = v; }
    public String getModel() { return model; }
    public void setModel(String v) { this.model = v; }
    public String getUsage() { return usage; }
    public void setUsage(String v) { this.usage = v; }
    public String getLicenseState() { return licenseState; }
    public void setLicenseState(String v) { this.licenseState = v; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String v) { this.licensePlate = v; }
    public Double getVehicleCost() { return vehicleCost != null ? vehicleCost : 0.0; }
    public void setVehicleCost(Double v) { this.vehicleCost = v; }
    public Double getStatedValue() { return statedValue; }
    public void setStatedValue(Double v) { this.statedValue = v; }
    public Boolean getLeasedOrRented() { return leasedOrRented; }
    public void setLeasedOrRented(Boolean v) { this.leasedOrRented = v; }
    public String getLeaseLength() { return leaseLength; }
    public void setLeaseLength(String v) { this.leaseLength = v; }
    public Integer getAnnualMileage() { return annualMileage; }
    public void setAnnualMileage(Integer v) { this.annualMileage = v; }
    public Integer getCommutingMilesOneWay() { return commutingMilesOneWay; }
    public void setCommutingMilesOneWay(Integer v) { this.commutingMilesOneWay = v; }
    public Boolean getAbsPresent() { return absPresent; }
    public void setAbsPresent(Boolean v) { this.absPresent = v; }
    public Boolean getPassiveRestraintSys() { return passiveRestraintSys; }
    public void setPassiveRestraintSys(Boolean v) { this.passiveRestraintSys = v; }
    public Boolean getAntiTheft() { return antiTheft; }
    public void setAntiTheft(Boolean v) { this.antiTheft = v; }
    public String getPrimaryDriverLicense() { return primaryDriverLicense; }
    public void setPrimaryDriverLicense(String v) { this.primaryDriverLicense = v; }
}
