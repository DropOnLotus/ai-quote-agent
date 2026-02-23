package com.guidewire.ai.agent.cli;

import com.guidewire.ai.agent.conversation.ConversationAgent;
import com.guidewire.ai.agent.conversation.ConversationResponse;
import com.guidewire.ai.agent.conversation.ConversationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Interactive command-line front-end for the AI Quote Agent.
 *
 * <pre>
 *   java -jar target/ai-quote-agent-cli.jar
 * </pre>
 *
 * Type your insurance request, answer follow-up questions, pick a plan, and
 * the agent will bind the policy for you.  Type {@code quit} or {@code exit}
 * at any prompt to abort.
 */
public class QuoteAgentCLI {

    private static final Logger logger = LoggerFactory.getLogger(QuoteAgentCLI.class);

    public static void main(String[] args) {
        printBanner();
        ConversationAgent agent = new ConversationAgent();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            ConversationState state = null;

            System.out.println("How can I help you today?  " +
                    "(describe the policy you'd like to buy, or type 'quit' to exit)\n");
            System.out.print("> ");
            System.out.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    System.out.print("> ");
                    System.out.flush();
                    continue;
                }

                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    System.out.println("Goodbye!");
                    break;
                }

                ConversationResponse response = agent.processMessage(line, state);
                state = response.getUpdatedState();

                System.out.println();
                System.out.println(response.getMessage());
                System.out.println();

                if (response.isComplete()) {
                    break;
                }

                System.out.print("> ");
                System.out.flush();
            }

        } catch (Exception e) {
            logger.error("Fatal CLI error", e);
            System.err.println("Fatal error: " + e.getMessage());
        } finally {
            agent.shutdown();
        }
    }

    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         AI Quote Agent — Guidewire PolicyCenter          ║");
        System.out.println("║    Powered by Claude AI  ·  Natural-Language Interface   ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
