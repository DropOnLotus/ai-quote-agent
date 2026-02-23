package com.guidewire.ai.agent.model;

import java.util.List;

public class PersonalAutoDetails {
    private List<PADriver> drivers;
    private List<PAVehicle> vehicles;
    private PACoverages coverages;

    public List<PADriver> getDrivers() { return drivers; }
    public void setDrivers(List<PADriver> v) { this.drivers = v; }
    public List<PAVehicle> getVehicles() { return vehicles; }
    public void setVehicles(List<PAVehicle> v) { this.vehicles = v; }
    public PACoverages getCoverages() { return coverages; }
    public void setCoverages(PACoverages v) { this.coverages = v; }
}
