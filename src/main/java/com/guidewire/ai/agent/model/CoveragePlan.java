package com.guidewire.ai.agent.model;

import com.guidewire.ai.agent.engine.CoveragePermutationEngine;

public class CoveragePlan {
    private String planName;
    private String description;
    private String biLimit;
    private String pdLimit;
    private Integer collisionDeductible;
    private Integer compDeductible;
    private CoveragePermutationEngine.PlanTier tier;

    public String getPlanName() { return planName; }
    public void setPlanName(String v) { this.planName = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getBiLimit() { return biLimit; }
    public void setBiLimit(String v) { this.biLimit = v; }
    public String getPdLimit() { return pdLimit; }
    public void setPdLimit(String v) { this.pdLimit = v; }
    public Integer getCollisionDeductible() { return collisionDeductible; }
    public void setCollisionDeductible(Integer v) { this.collisionDeductible = v; }
    public Integer getCompDeductible() { return compDeductible; }
    public void setCompDeductible(Integer v) { this.compDeductible = v; }
    public CoveragePermutationEngine.PlanTier getTier() { return tier; }
    public void setTier(CoveragePermutationEngine.PlanTier v) { this.tier = v; }
}
