package com.guidewire.ai.agent.model;

public class PADriver {
    private String firstName;
    private String lastName;
    private String licenseNumber;
    private String licenseState;
    private String dateOfBirth;
    private Integer numberOfViolations;
    private Integer numberOfAccidents;
    private String yearLicensed;
    private String dateCompletedTrainingClass;
    private String trainingClassType;
    private Boolean goodDriverDiscount;
    private String studentDriver;
    private Address address;

    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String v) { this.licenseNumber = v; }
    public String getLicenseState() { return licenseState; }
    public void setLicenseState(String v) { this.licenseState = v; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String v) { this.dateOfBirth = v; }
    public Integer getNumberOfViolations() { return numberOfViolations != null ? numberOfViolations : 0; }
    public void setNumberOfViolations(Integer v) { this.numberOfViolations = v; }
    public Integer getNumberOfAccidents() { return numberOfAccidents != null ? numberOfAccidents : 0; }
    public void setNumberOfAccidents(Integer v) { this.numberOfAccidents = v; }
    public String getYearLicensed() { return yearLicensed; }
    public void setYearLicensed(String v) { this.yearLicensed = v; }
    public String getDateCompletedTrainingClass() { return dateCompletedTrainingClass; }
    public void setDateCompletedTrainingClass(String v) { this.dateCompletedTrainingClass = v; }
    public String getTrainingClassType() { return trainingClassType; }
    public void setTrainingClassType(String v) { this.trainingClassType = v; }
    public Boolean getGoodDriverDiscount() { return goodDriverDiscount; }
    public void setGoodDriverDiscount(Boolean v) { this.goodDriverDiscount = v; }
    public String getStudentDriver() { return studentDriver; }
    public void setStudentDriver(String v) { this.studentDriver = v; }
    public Address getAddress() { return address; }
    public void setAddress(Address v) { this.address = v; }
}
