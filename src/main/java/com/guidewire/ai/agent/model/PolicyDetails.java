package com.guidewire.ai.agent.model;

import java.util.List;

public class PolicyDetails {
    private String termType;
    private String effectiveDate;
    private String expirationDate;
    private String writtenDate;
    private String rateAsOfDate;

    public String getTermType() { return termType; }
    public void setTermType(String v) { this.termType = v; }
    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String v) { this.effectiveDate = v; }
    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String v) { this.expirationDate = v; }
    public String getWrittenDate() { return writtenDate; }
    public void setWrittenDate(String v) { this.writtenDate = v; }
    public String getRateAsOfDate() { return rateAsOfDate; }
    public void setRateAsOfDate(String v) { this.rateAsOfDate = v; }
}
