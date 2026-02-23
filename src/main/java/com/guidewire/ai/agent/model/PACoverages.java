package com.guidewire.ai.agent.model;

public class PACoverages {
    private String biLimit;
    private String pdLimit;
    private Integer collisionDeductible;
    private Integer compDeductible;

    public String getBiLimit() { return biLimit; }
    public void setBiLimit(String v) { this.biLimit = v; }
    public String getPdLimit() { return pdLimit; }
    public void setPdLimit(String v) { this.pdLimit = v; }
    public Integer getCollisionDeductible() { return collisionDeductible; }
    public void setCollisionDeductible(Integer v) { this.collisionDeductible = v; }
    public Integer getCompDeductible() { return compDeductible; }
    public void setCompDeductible(Integer v) { this.compDeductible = v; }
}
