package com.guidewire.ai.agent.model;

public class CostItem {
    private String description;
    private Double amount;
    private String costType;

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Double getAmount() { return amount; }
    public void setAmount(Double v) { this.amount = v; }
    public String getCostType() { return costType; }
    public void setCostType(String v) { this.costType = v; }
}
