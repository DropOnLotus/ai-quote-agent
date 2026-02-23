package com.guidewire.ai.agent.model;

public class UserProfile {
    private Integer age;
    private Integer numberOfViolations;
    private Integer numberOfAccidents;
    private Double vehicleValue;
    private Double estimatedNetWorth;
    private Double emergencyFundAmount;

    public Integer getAge() { return age; }
    public void setAge(Integer v) { this.age = v; }
    public Integer getNumberOfViolations() { return numberOfViolations != null ? numberOfViolations : 0; }
    public void setNumberOfViolations(Integer v) { this.numberOfViolations = v; }
    public Integer getNumberOfAccidents() { return numberOfAccidents != null ? numberOfAccidents : 0; }
    public void setNumberOfAccidents(Integer v) { this.numberOfAccidents = v; }
    public Double getVehicleValue() { return vehicleValue != null ? vehicleValue : 0.0; }
    public void setVehicleValue(Double v) { this.vehicleValue = v; }
    public Double getEstimatedNetWorth() { return estimatedNetWorth != null ? estimatedNetWorth : 0.0; }
    public void setEstimatedNetWorth(Double v) { this.estimatedNetWorth = v; }
    public Double getEmergencyFundAmount() { return emergencyFundAmount != null ? emergencyFundAmount : 5000.0; }
    public void setEmergencyFundAmount(Double v) { this.emergencyFundAmount = v; }
}
