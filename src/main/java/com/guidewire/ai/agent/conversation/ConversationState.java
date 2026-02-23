package com.guidewire.ai.agent.conversation;

import com.guidewire.ai.agent.model.QuoteAnalysisResult;
import com.guidewire.ai.agent.nl.ExtractedPolicyRequest;

/**
 * Mutable session state for one policy-buying conversation.
 * Stored server-side (in-memory) or passed back-and-forth in the CLI loop.
 */
public class ConversationState {

    private ConversationStage stage;
    private ExtractedPolicyRequest collectedData;

    /** The field name currently being solicited from the user, or {@code null}. */
    private String pendingFieldName;

    /** Populated after the quoting step completes. */
    private QuoteAnalysisResult quoteResult;

    // ── Getters / Setters ────────────────────────────────────────────

    public ConversationStage getStage()             { return stage; }
    public void setStage(ConversationStage v)       { this.stage = v; }

    public ExtractedPolicyRequest getCollectedData() { return collectedData; }
    public void setCollectedData(ExtractedPolicyRequest v) { this.collectedData = v; }

    public String getPendingFieldName()             { return pendingFieldName; }
    public void setPendingFieldName(String v)       { this.pendingFieldName = v; }

    public QuoteAnalysisResult getQuoteResult()     { return quoteResult; }
    public void setQuoteResult(QuoteAnalysisResult v) { this.quoteResult = v; }
}
