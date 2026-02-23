package com.guidewire.ai.agent.model;

public class Address {
    private String addressline1;
    private String city;
    private String state;
    private String postalcode;
    private String addressType = "home";

    public String getAddressline1() { return addressline1; }
    public void setAddressline1(String v) { this.addressline1 = v; }
    public String getCity() { return city; }
    public void setCity(String v) { this.city = v; }
    public String getState() { return state; }
    public void setState(String v) { this.state = v; }
    public String getPostalcode() { return postalcode; }
    public void setPostalcode(String v) { this.postalcode = v; }
    public String getAddressType() { return addressType; }
    public void setAddressType(String v) { this.addressType = v; }
}
