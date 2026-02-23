package com.guidewire.ai.agent.model;

import java.util.List;

public class SubmissionQuoteResponse {
    private String jobId;
    private String jobNumber;
    private String policyNumber;
    private String status;
    private QuoteDetails quoteDetails;

    public String getJobId() { return jobId; }
    public void setJobId(String v) { this.jobId = v; }
    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String v) { this.jobNumber = v; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String v) { this.policyNumber = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public QuoteDetails getQuoteDetails() { return quoteDetails; }
    public void setQuoteDetails(QuoteDetails v) { this.quoteDetails = v; }
}
