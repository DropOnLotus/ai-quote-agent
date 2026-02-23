package com.guidewire.ai.agent.model;

public class PolicyIssuanceRequest {
    private String jobNumber;
    private String billingMethod;
    private String paymentPlanId;

    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String v) { this.jobNumber = v; }
    public String getBillingMethod() { return billingMethod; }
    public void setBillingMethod(String v) { this.billingMethod = v; }
    public String getPaymentPlanId() { return paymentPlanId; }
    public void setPaymentPlanId(String v) { this.paymentPlanId = v; }
}
