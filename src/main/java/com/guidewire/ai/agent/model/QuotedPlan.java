package com.guidewire.ai.agent.model;

import java.util.List;

public class QuotedPlan {
    private CoveragePlan plan;
    private Double totalPremium;
    private Double totalCost;
    private String jobNumber;
    private List<CostItem> costs;

    public CoveragePlan getPlan() { return plan; }
    public void setPlan(CoveragePlan v) { this.plan = v; }
    public Double getTotalPremium() { return totalPremium; }
    public void setTotalPremium(Double v) { this.totalPremium = v; }
    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double v) { this.totalCost = v; }
    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String v) { this.jobNumber = v; }
    public List<CostItem> getCosts() { return costs; }
    public void setCosts(List<CostItem> v) { this.costs = v; }
}
