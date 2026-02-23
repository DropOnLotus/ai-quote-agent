package com.guidewire.ai.agent.conversation;

/**
 * The agent's reply to a single user message.
 */
public class ConversationResponse {

    private final String message;
    private final boolean complete;
    private final ConversationState updatedState;

    public ConversationResponse(String message, boolean complete, ConversationState updatedState) {
        this.message      = message;
        this.complete     = complete;
        this.updatedState = updatedState;
    }

    public String getMessage()                { return message; }
    public boolean isComplete()               { return complete; }
    public ConversationState getUpdatedState(){ return updatedState; }
}
