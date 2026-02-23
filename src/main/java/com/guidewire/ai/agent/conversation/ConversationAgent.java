package com.guidewire.ai.agent.conversation;

import com.guidewire.ai.agent.engine.CoveragePermutationEngine;
import com.guidewire.ai.agent.model.*;
import com.guidewire.ai.agent.nl.ExtractedPolicyRequest;
import com.guidewire.ai.agent.nl.NaturalLanguageParser;
import com.guidewire.ai.agent.service.AIQuoteAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Stateless agent that drives a natural-language policy-buying conversation.
 *
 * <p>Each call to {@link #processMessage} takes the current {@link ConversationState}
 * (which may be {@code null} for a brand-new session), applies the user's message,
 * advances the state machine, and returns a {@link ConversationResponse} containing
 * the agent's reply and the updated state.
 *
 * <p>State machine:
 * <pre>
 *   COLLECTING_INFO  →  (transient) READY_TO_QUOTE  →  AWAITING_PLAN_SELECTION
 *                                                              ↓
 *                                                    (transient) BINDING  →  COMPLETE
 *                                                                              or ERROR
 * </pre>
 */
public class ConversationAgent {

    private static final Logger logger = LoggerFactory.getLogger(ConversationAgent.class);

    private static final int MAX_PLANS_TO_SHOW = 3;

    private final NaturalLanguageParser nlParser;
    private final AIQuoteAgentService   agentService;

    public ConversationAgent() {
        this.nlParser     = new NaturalLanguageParser();
        this.agentService = new AIQuoteAgentService();
    }

    /** Package-private constructor for testing with injected dependencies. */
    ConversationAgent(NaturalLanguageParser nlParser, AIQuoteAgentService agentService) {
        this.nlParser     = nlParser;
        this.agentService = agentService;
    }

    // ════════════════════════════════════════════════════════════════
    // Public entry point
    // ════════════════════════════════════════════════════════════════

    /**
     * Process one user message in the context of the current session state.
     *
     * @param userMessage raw text entered by the user
     * @param state       current session state, or {@code null} to start a new conversation
     * @return agent reply + updated state
     */
    public ConversationResponse processMessage(String userMessage, ConversationState state) {
        try {
            // ── Brand-new conversation ──────────────────────────────
            if (state == null || state.getStage() == null) {
                state = new ConversationState();
                state.setStage(ConversationStage.COLLECTING_INFO);
                state.setCollectedData(new ExtractedPolicyRequest());

                logger.info("New conversation — parsing initial message with NL parser.");
                ExtractedPolicyRequest parsed = nlParser.parse(userMessage);
                state.setCollectedData(parsed);

                // Fall through to the COLLECTING_INFO handler below
            }

            // ── Route by current stage ──────────────────────────────
            switch (state.getStage()) {

                case COLLECTING_INFO:
                    return handleCollecting(userMessage, state);

                case AWAITING_PLAN_SELECTION:
                    return handlePlanSelection(userMessage, state);

                case COMPLETE:
                case ERROR:
                    return new ConversationResponse(
                            "This conversation is already complete. Please start a new session.",
                            true, state);

                default:
                    return new ConversationResponse("Unexpected state — please start a new session.",
                            true, state);
            }

        } catch (Exception e) {
            logger.error("Unexpected error in processMessage", e);
            if (state == null) state = new ConversationState();
            state.setStage(ConversationStage.ERROR);
            return new ConversationResponse(
                    "An unexpected error occurred: " + e.getMessage() +
                    "\nPlease try again or contact support.", true, state);
        }
    }

    /** Shut down underlying services cleanly. */
    public void shutdown() {
        agentService.shutdown();
        try { nlParser.close(); } catch (IOException e) {
            logger.warn("Error closing NL parser", e);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Stage handlers
    // ════════════════════════════════════════════════════════════════

    private ConversationResponse handleCollecting(String userMessage, ConversationState state) {
        // Apply the user's answer to the pending field (if any)
        if (state.getPendingFieldName() != null) {
            boolean valid = applyAnswer(state.getCollectedData(),
                    state.getPendingFieldName(), userMessage.trim());
            if (!valid) {
                String hint = validationHint(state.getPendingFieldName());
                return new ConversationResponse(
                        "I didn't understand that. " + hint + "\n" +
                        questionFor(state.getPendingFieldName()),
                        false, state);
            }
        }

        // Find the next field we still need
        String nextField = findNextMissingField(state.getCollectedData());
        if (nextField != null) {
            state.setPendingFieldName(nextField);
            return new ConversationResponse(questionFor(nextField), false, state);
        }

        // All required fields collected — run the quote
        state.setPendingFieldName(null);
        return runQuoteAndTransition(state);
    }

    private ConversationResponse runQuoteAndTransition(ConversationState state) {
        state.setStage(ConversationStage.READY_TO_QUOTE);
        try {
            logger.info("All fields collected — generating quotes...");
            CustomerInfo    customerInfo = buildCustomerInfo(state.getCollectedData());
            UserPreferences prefs        = buildUserPreferences();

            QuoteAnalysisResult result =
                    agentService.generateQuotesWithRecommendations(customerInfo, prefs);

            state.setQuoteResult(result);
            state.setStage(ConversationStage.AWAITING_PLAN_SELECTION);

            if (result.getRecommendations() == null || result.getRecommendations().isEmpty()) {
                state.setStage(ConversationStage.ERROR);
                return new ConversationResponse(
                        "No quotes could be generated at this time. " +
                        "Please verify PolicyCenter is running and try again.",
                        true, state);
            }

            return new ConversationResponse(formatRecommendations(result), false, state);

        } catch (Exception e) {
            logger.error("Quote generation failed", e);
            state.setStage(ConversationStage.ERROR);
            return new ConversationResponse(
                    "Failed to generate quotes: " + e.getMessage() +
                    "\nPlease verify PolicyCenter is accessible and try again.",
                    true, state);
        }
    }

    private ConversationResponse handlePlanSelection(String userMessage, ConversationState state) {
        int index = parsePlanSelection(userMessage.trim(), state.getQuoteResult());
        if (index < 0) {
            int maxPlans = Math.min(MAX_PLANS_TO_SHOW,
                    state.getQuoteResult().getRecommendations().size());
            return new ConversationResponse(
                    "Please enter a number between 1 and " + maxPlans +
                    ", or type the plan name (e.g. \"Standard\").",
                    false, state);
        }

        QuoteRecommendation selected =
                state.getQuoteResult().getRecommendations().get(index);
        state.setStage(ConversationStage.BINDING);

        try {
            String jobNumber   = state.getQuoteResult().getJobNumber();
            String policyNumber = agentService.bindPolicy(jobNumber);
            state.setStage(ConversationStage.COMPLETE);

            String msg = String.format(
                    "Your policy has been issued successfully!\n\n" +
                    "  Plan:          %s\n" +
                    "  Annual Premium: $%.2f\n" +
                    "  Policy Number:  %s\n\n" +
                    "Policy documents will be sent to your registered email. " +
                    "Welcome aboard!",
                    selected.getPlanName(),
                    selected.getPremium() != null ? selected.getPremium() : 0.0,
                    policyNumber);

            return new ConversationResponse(msg, true, state);

        } catch (IOException e) {
            logger.error("Policy binding failed", e);
            state.setStage(ConversationStage.ERROR);
            return new ConversationResponse(
                    "Policy binding failed: " + e.getMessage() +
                    "\nPlease contact support with your quote reference.",
                    true, state);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Field collection helpers
    // ════════════════════════════════════════════════════════════════

    /** Returns the name of the first required field that is still null, or null if complete. */
    private String findNextMissingField(ExtractedPolicyRequest d) {
        if (d.getAddressLine1() == null) return "addressLine1";
        if (d.getCity()         == null) return "city";
        if (d.getState()        == null) return "state";
        if (d.getPostalCode()   == null) return "postalCode";
        if (d.getVehicleYear()  == null) return "vehicleYear";
        if (d.getVehicleMake()  == null) return "vehicleMake";
        if (d.getVehicleModel() == null) return "vehicleModel";
        if (d.getFirstName()    == null) return "firstName";
        if (d.getLastName()     == null) return "lastName";
        if (d.getDateOfBirth()  == null) return "dateOfBirth";
        if (d.getLicenseNumber()== null) return "licenseNumber";
        // email is optional
        return null;
    }

    /**
     * Applies the user's raw answer to the named field.
     * Returns {@code false} if the value fails format validation.
     */
    private boolean applyAnswer(ExtractedPolicyRequest d, String field, String raw) {
        switch (field) {
            case "addressLine1":
                d.setAddressLine1(raw);
                return true;

            case "city":
                d.setCity(raw);
                return true;

            case "state":
                if (raw.length() == 2 && raw.matches("[A-Za-z]{2}")) {
                    d.setState(raw.toUpperCase());
                    return true;
                }
                // Accept full state names mapped to abbreviations
                String abbr = stateAbbreviation(raw);
                if (abbr != null) { d.setState(abbr); return true; }
                return false;

            case "postalCode":
                if (raw.matches("\\d{5}")) { d.setPostalCode(raw); return true; }
                return false;

            case "vehicleYear":
                try {
                    int yr = Integer.parseInt(raw.trim());
                    if (yr >= 1900 && yr <= LocalDate.now().getYear() + 1) {
                        d.setVehicleYear(yr);
                        return true;
                    }
                } catch (NumberFormatException ignored) {}
                return false;

            case "vehicleMake":
                d.setVehicleMake(raw);
                return true;

            case "vehicleModel":
                d.setVehicleModel(raw);
                return true;

            case "firstName":
                d.setFirstName(raw);
                return true;

            case "lastName":
                d.setLastName(raw);
                return true;

            case "dateOfBirth":
                if (raw.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    d.setDateOfBirth(raw);
                    return true;
                }
                return false;

            case "licenseNumber":
                if (!raw.isEmpty()) { d.setLicenseNumber(raw); return true; }
                return false;

            default:
                return true;
        }
    }

    private String questionFor(String field) {
        switch (field) {
            case "addressLine1":  return "What is your street address? (e.g. 265 Slater Street Apt 101)";
            case "city":          return "What city do you live in?";
            case "state":         return "What state do you live in? (Please provide the 2-letter code, e.g. CT)";
            case "postalCode":    return "What is your ZIP code? (5 digits)";
            case "vehicleYear":   return "What year is your vehicle? (e.g. 2023)";
            case "vehicleMake":   return "What is the make (brand) of your vehicle? (e.g. Lincoln)";
            case "vehicleModel":  return "What is the model of your vehicle? (e.g. MKC)";
            case "firstName":     return "What is your first name?";
            case "lastName":      return "What is your last name?";
            case "dateOfBirth":   return "What is your date of birth? (MM/DD/YYYY, e.g. 01/15/1985)";
            case "licenseNumber": return "What is your driver's license number?";
            default:              return "Please provide your " + field + ":";
        }
    }

    private String validationHint(String field) {
        switch (field) {
            case "state":       return "Please enter a 2-letter US state code (e.g. CT, NY, CA).";
            case "postalCode":  return "Please enter a 5-digit ZIP code (e.g. 06045).";
            case "vehicleYear": return "Please enter a 4-digit year (e.g. 2023).";
            case "dateOfBirth": return "Please use MM/DD/YYYY format (e.g. 01/15/1985).";
            default:            return "Please try again.";
        }
    }

    /** Simple common-name-to-abbreviation lookup (subset of US states). */
    private String stateAbbreviation(String name) {
        switch (name.trim().toLowerCase()) {
            case "connecticut":    return "CT";
            case "new york":       return "NY";
            case "new jersey":     return "NJ";
            case "massachusetts":  return "MA";
            case "california":     return "CA";
            case "florida":        return "FL";
            case "texas":          return "TX";
            case "illinois":       return "IL";
            case "pennsylvania":   return "PA";
            case "ohio":           return "OH";
            case "georgia":        return "GA";
            case "michigan":       return "MI";
            case "virginia":       return "VA";
            case "washington":     return "WA";
            case "arizona":        return "AZ";
            case "colorado":       return "CO";
            case "nevada":         return "NV";
            default:               return null;
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Object builders
    // ════════════════════════════════════════════════════════════════

    private CustomerInfo buildCustomerInfo(ExtractedPolicyRequest d) {
        CustomerInfo c = new CustomerInfo();
        c.setFirstName(d.getFirstName());
        c.setLastName(d.getLastName());
        c.setDateOfBirth(toIsoDate(d.getDateOfBirth()));
        c.setEmail(d.getEmail() != null && !d.getEmail().isEmpty()
                ? d.getEmail() : "noreply@placeholder.com");
        c.setLineOfBusiness("PersonalAutoLine");

        // Effective date = tomorrow
        c.setDesiredEffectiveDate(LocalDate.now().plusDays(1).toString());

        // Address
        Address addr = new Address();
        addr.setAddressline1(d.getAddressLine1());
        addr.setCity(d.getCity());
        addr.setState(d.getState());
        addr.setPostalcode(d.getPostalCode());
        c.setAddress(addr);

        // Derived vehicle values
        int currentYear = LocalDate.now().getYear();
        int vehicleYear = d.getVehicleYear() != null ? d.getVehicleYear() : currentYear;
        double vehicleCost = Math.max(8_000.0, 30_000.0 - (currentYear - vehicleYear) * 3_000.0);
        String plate       = d.getLicensePlate() != null ? d.getLicensePlate() : "UNKNOWN";
        String vin         = "UNKNOWN-" + plate;
        String licState    = d.getLicenseState() != null ? d.getLicenseState() : d.getState();

        PAVehicle vehicle = new PAVehicle();
        vehicle.setVin(vin);
        vehicle.setYear(vehicleYear);
        vehicle.setMake(d.getVehicleMake());
        vehicle.setModel(d.getVehicleModel());
        vehicle.setLicensePlate(plate);
        vehicle.setLicenseState(licState);
        vehicle.setAnnualMileage(12_000);
        vehicle.setVehicleCost(vehicleCost);
        vehicle.setUsage("Pleasure");
        vehicle.setPrimaryDriverLicense(d.getLicenseNumber());

        PADriver driver = new PADriver();
        driver.setFirstName(d.getFirstName());
        driver.setLastName(d.getLastName());
        driver.setDateOfBirth(toIsoDate(d.getDateOfBirth()));
        driver.setLicenseNumber(d.getLicenseNumber());
        driver.setLicenseState(licState);
        driver.setNumberOfViolations(0);
        driver.setNumberOfAccidents(0);
        driver.setGoodDriverDiscount(true);
        driver.setYearLicensed(String.valueOf(Math.max(1990, vehicleYear - 10)));

        c.setVehicles(Collections.singletonList(vehicle));
        c.setDrivers(Collections.singletonList(driver));

        return c;
    }

    private UserPreferences buildUserPreferences() {
        UserPreferences prefs = new UserPreferences();
        prefs.setPreferredTier(CoveragePermutationEngine.PlanTier.STANDARD);
        prefs.setPriceSensitivity(3);
        prefs.setPrioritizeCoverage(false);
        prefs.setPrioritizePrice(false);
        return prefs;
    }

    // ════════════════════════════════════════════════════════════════
    // Quote display helpers
    // ════════════════════════════════════════════════════════════════

    private String formatRecommendations(QuoteAnalysisResult result) {
        List<QuoteRecommendation> recs = result.getRecommendations();
        int count = Math.min(MAX_PLANS_TO_SHOW, recs.size());

        StringBuilder sb = new StringBuilder();
        sb.append("Great news! Here are your top ").append(count).append(" quote(s):\n\n");

        for (int i = 0; i < count; i++) {
            QuoteRecommendation rec = recs.get(i);
            sb.append(String.format("%d. %s  —  $%.2f / year%n",
                    i + 1, rec.getPlanName(),
                    rec.getPremium() != null ? rec.getPremium() : 0.0));

            CoveragePlan plan = rec.getCoveragePlan();
            if (plan != null) {
                sb.append(String.format("   Coverage:  BI %s,  PD %s%n",
                        plan.getBiLimit(), plan.getPdLimit()));
                if (plan.getCollisionDeductible() != null) {
                    sb.append(String.format("   Deductibles: Collision $%d,  Comp $%d%n",
                            plan.getCollisionDeductible(), plan.getCompDeductible()));
                }
            }

            if (rec.getRecommendationLabel() != null) {
                sb.append("   ").append(rec.getRecommendationLabel()).append("\n");
            }
            if (rec.getReasoningPoints() != null && !rec.getReasoningPoints().isEmpty()) {
                sb.append("   ").append(rec.getReasoningPoints().get(0)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Please enter 1, 2, or 3 to select a plan (or type the plan name):");
        return sb.toString();
    }

    /**
     * Parses the user's plan-selection input.
     *
     * @return 0-based index into {@code result.getRecommendations()}, or -1 on invalid input.
     */
    private int parsePlanSelection(String input, QuoteAnalysisResult result) {
        List<QuoteRecommendation> recs = result.getRecommendations();
        int maxPlans = Math.min(MAX_PLANS_TO_SHOW, recs.size());

        // Numeric selection
        try {
            int num = Integer.parseInt(input.trim());
            if (num >= 1 && num <= maxPlans) return num - 1;
        } catch (NumberFormatException ignored) {}

        // Name match (case-insensitive, partial)
        String lower = input.toLowerCase();
        for (int i = 0; i < maxPlans; i++) {
            String name = recs.get(i).getPlanName();
            if (name != null && (name.equalsIgnoreCase(input) ||
                    name.toLowerCase().contains(lower))) {
                return i;
            }
        }
        return -1;
    }

    // ════════════════════════════════════════════════════════════════
    // Date conversion utility
    // ════════════════════════════════════════════════════════════════

    /** Converts MM/DD/YYYY → yyyy-MM-dd.  Returns the original string on failure. */
    private String toIsoDate(String mmddyyyy) {
        if (mmddyyyy == null || mmddyyyy.isEmpty()) return null;
        try {
            String[] p = mmddyyyy.split("/");
            if (p.length == 3 && p[2].length() == 4) {
                return p[2] + "-" + p[0] + "-" + p[1];
            }
        } catch (Exception ignored) {}
        return mmddyyyy; // already in another format — pass through
    }
}
