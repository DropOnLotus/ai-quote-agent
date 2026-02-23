package com.guidewire.ai.agent.conversation;

/**
 * Lifecycle stages of a single policy-buying conversation.
 *
 * READY_TO_QUOTE and BINDING are transient — they are resolved within the
 * same {@code processMessage()} call and never persisted in session state.
 */
public enum ConversationStage {
    /** Gathering required fields via NL parse + follow-up questions. */
    COLLECTING_INFO,

    /** (Transient) All fields collected; about to call AIQuoteAgentService. */
    READY_TO_QUOTE,

    /** Quotes displayed; waiting for the user to pick a plan (1/2/3 or name). */
    AWAITING_PLAN_SELECTION,

    /** (Transient) Plan selected; about to call bindPolicy(). */
    BINDING,

    /** Policy successfully issued. Conversation is over. */
    COMPLETE,

    /** An unrecoverable error occurred. Conversation is over. */
    ERROR
}
