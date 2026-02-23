package com.guidewire.ai.agent.model;

import com.guidewire.ai.agent.engine.CoveragePermutationEngine;

public class UserPreferences {
    private CoveragePermutationEngine.PlanTier preferredTier;
    private Integer priceSensitivity;
    private Boolean prioritizeCoverage;
    private Boolean prioritizePrice;

    public CoveragePermutationEngine.PlanTier getPreferredTier() { return preferredTier; }
    public void setPreferredTier(CoveragePermutationEngine.PlanTier v) { this.preferredTier = v; }
    public Integer getPriceSensitivity() { return priceSensitivity != null ? priceSensitivity : 3; }
    public void setPriceSensitivity(Integer v) { this.priceSensitivity = v; }
    public Boolean getPrioritizeCoverage() { return prioritizeCoverage; }
    public void setPrioritizeCoverage(Boolean v) { this.prioritizeCoverage = v; }
    public Boolean getPrioritizePrice() { return prioritizePrice; }
    public void setPrioritizePrice(Boolean v) { this.prioritizePrice = v; }
}
