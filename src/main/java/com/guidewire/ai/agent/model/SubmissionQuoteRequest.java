package com.guidewire.ai.agent.model;

public class SubmissionQuoteRequest {
    private String jobNumber;
    private PolicyDetails policyDetails;
    private PersonalAutoDetails paDetails;

    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String v) { this.jobNumber = v; }
    public PolicyDetails getPolicyDetails() { return policyDetails; }
    public void setPolicyDetails(PolicyDetails v) { this.policyDetails = v; }
    public PersonalAutoDetails getPaDetails() { return paDetails; }
    public void setPaDetails(PersonalAutoDetails v) { this.paDetails = v; }
}
