package com.guidewire.ai.agent.model;

import java.util.List;

public class QuoteAnalysisResult {
    private String accountNumber;
    private String jobNumber;
    private List<QuoteRecommendation> recommendations;
    private QuoteRecommendation topRecommendation;
    private Integer numberOfQuotesAnalyzed;
    private Long executionTimeMs;

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String v) { this.accountNumber = v; }
    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String v) { this.jobNumber = v; }
    public List<QuoteRecommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<QuoteRecommendation> v) { this.recommendations = v; }
    public QuoteRecommendation getTopRecommendation() { return topRecommendation; }
    public void setTopRecommendation(QuoteRecommendation v) { this.topRecommendation = v; }
    public Integer getNumberOfQuotesAnalyzed() { return numberOfQuotesAnalyzed; }
    public void setNumberOfQuotesAnalyzed(Integer v) { this.numberOfQuotesAnalyzed = v; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long v) { this.executionTimeMs = v; }
}
