package com.guidewire.ai.agent.model;

public class PolicyIssuanceResponse {
    private String policyNumber;
    private String status;
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String v) { this.policyNumber = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
