package com.guidewire.ai.agent.model;

import java.util.List;

public class QuoteDetails {
    private Double totalPremium;
    private Double totalCost;
    private String currency;
    private List<CostItem> costs;

    public Double getTotalPremium() { return totalPremium; }
    public void setTotalPremium(Double v) { this.totalPremium = v; }
    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double v) { this.totalCost = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public List<CostItem> getCosts() { return costs; }
    public void setCosts(List<CostItem> v) { this.costs = v; }
}
