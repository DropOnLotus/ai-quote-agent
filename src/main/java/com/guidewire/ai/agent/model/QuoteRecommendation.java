package com.guidewire.ai.agent.model;

import java.util.List;

public class QuoteRecommendation {
    private Integer rank;
    private String planName;
    private String description;
    private Double premium;
    private CoveragePlan coveragePlan;

    // AI Scores
    private Double priceScore;
    private Double coverageAdequacyScore;
    private Double valueScore;
    private Double riskMatchScore;
    private Double totalScore;

    // Labels & Reasoning
    private String recommendationLabel;
    private List<String> reasoningPoints;

    // Comparative flags
    private Boolean isLowestPrice;
    private Boolean isHighestCoverage;
    private Boolean isBestValue;
    private Double savingsVsHighest;
    private Double savingsVsAverage;

    public Integer getRank() { return rank; }
    public void setRank(Integer v) { this.rank = v; }
    public String getPlanName() { return planName; }
    public void setPlanName(String v) { this.planName = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Double getPremium() { return premium; }
    public void setPremium(Double v) { this.premium = v; }
    public CoveragePlan getCoveragePlan() { return coveragePlan; }
    public void setCoveragePlan(CoveragePlan v) { this.coveragePlan = v; }
    public Double getPriceScore() { return priceScore; }
    public void setPriceScore(Double v) { this.priceScore = v; }
    public Double getCoverageAdequacyScore() { return coverageAdequacyScore; }
    public void setCoverageAdequacyScore(Double v) { this.coverageAdequacyScore = v; }
    public Double getValueScore() { return valueScore; }
    public void setValueScore(Double v) { this.valueScore = v; }
    public Double getRiskMatchScore() { return riskMatchScore; }
    public void setRiskMatchScore(Double v) { this.riskMatchScore = v; }
    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double v) { this.totalScore = v; }
    public String getRecommendationLabel() { return recommendationLabel; }
    public void setRecommendationLabel(String v) { this.recommendationLabel = v; }
    public List<String> getReasoningPoints() { return reasoningPoints; }
    public void setReasoningPoints(List<String> v) { this.reasoningPoints = v; }
    public Boolean getIsLowestPrice() { return isLowestPrice; }
    public void setIsLowestPrice(Boolean v) { this.isLowestPrice = v; }
    public Boolean getIsHighestCoverage() { return isHighestCoverage; }
    public void setIsHighestCoverage(Boolean v) { this.isHighestCoverage = v; }
    public Boolean getIsBestValue() { return isBestValue; }
    public void setIsBestValue(Boolean v) { this.isBestValue = v; }
    public Double getSavingsVsHighest() { return savingsVsHighest; }
    public void setSavingsVsHighest(Double v) { this.savingsVsHighest = v; }
    public Double getSavingsVsAverage() { return savingsVsAverage; }
    public void setSavingsVsAverage(Double v) { this.savingsVsAverage = v; }
}
