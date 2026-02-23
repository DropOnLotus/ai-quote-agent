package com.guidewire.ai.agent;

import com.guidewire.ai.agent.engine.CoveragePermutationEngine;
import com.guidewire.ai.agent.model.*;
import com.guidewire.ai.agent.service.AIQuoteAgentService;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point – demonstrates the full AI Quote Agent workflow.
 * Run this class in IntelliJ or via: java -jar target/ai-quote-agent-1.0.0-SNAPSHOT.jar
 */
public class AIQuoteAgentExample {

    public static void main(String[] args) {
        AIQuoteAgentService agentService = new AIQuoteAgentService();

        try {
            CustomerInfo   customerInfo   = buildSampleCustomer();
            UserPreferences userPreferences = new UserPreferences();
            userPreferences.setPreferredTier(CoveragePermutationEngine.PlanTier.STANDARD);
            userPreferences.setPriceSensitivity(3);  // 1 = price-focused, 5 = coverage-focused
            userPreferences.setPrioritizeCoverage(false);
            userPreferences.setPrioritizePrice(false);

            System.out.println("═══════════════════════════════════════════");
            System.out.println("   AI Quote Agent  –  Starting Analysis    ");
            System.out.println("═══════════════════════════════════════════\n");

            QuoteAnalysisResult result = agentService.generateQuotesWithRecommendations(
                    customerInfo, userPreferences);

            displayResults(result);

            // Bind the top-recommended plan
            System.out.println("\n══════════════════════════════════════════");
            System.out.println("  Binding top-recommended plan...");
            System.out.println("══════════════════════════════════════════\n");
            String policyNumber = agentService.bindPolicy(result.getJobNumber());
            System.out.println("✓ Policy Issued Successfully!");
            System.out.println("  Policy Number : " + policyNumber);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            agentService.shutdown();
        }
    }

    // ── Sample Data ──────────────────────────────────────────────

    private static CustomerInfo buildSampleCustomer() {
        CustomerInfo c = new CustomerInfo();
        c.setFirstName("John");
        c.setLastName("Smith");
        c.setDateOfBirth("1985-06-15");
        c.setEmail("john.smith@email.com");
        c.setPhoneNumber("555-1234");
        c.setDesiredEffectiveDate("2026-03-01");
        c.setLineOfBusiness("PersonalAutoLine");

        Address addr = new Address();
        addr.setAddressline1("123 Main Street");
        addr.setCity("Springfield");
        addr.setState("IL");
        addr.setPostalcode("62701");
        addr.setAddressType("home");
        c.setAddress(addr);

        PADriver driver = new PADriver();
        driver.setFirstName("John");
        driver.setLastName("Smith");
        driver.setLicenseNumber("S123456789");
        driver.setLicenseState("IL");
        driver.setDateOfBirth("1985-06-15");
        driver.setNumberOfViolations(0);
        driver.setNumberOfAccidents(0);
        driver.setYearLicensed("2003-06-15");
        driver.setGoodDriverDiscount(true);
        driver.setAddress(addr);
        List<PADriver> drivers = new ArrayList<>();
        drivers.add(driver);
        c.setDrivers(drivers);

        PAVehicle vehicle = new PAVehicle();
        vehicle.setVin("1HGCM82633A123456");
        vehicle.setYear(2022);
        vehicle.setMake("Honda");
        vehicle.setModel("Accord");
        vehicle.setUsage("Commute");
        vehicle.setLicenseState("IL");
        vehicle.setVehicleCost(28000.0);
        vehicle.setStatedValue(28000.0);
        vehicle.setLeasedOrRented(false);
        vehicle.setAnnualMileage(12000);
        vehicle.setPrimaryDriverLicense("S123456789");
        List<PAVehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle);
        c.setVehicles(vehicles);

        return c;
    }

    // ── Output Formatting ────────────────────────────────────────

    private static void displayResults(QuoteAnalysisResult result) {
        System.out.println("Account Number       : " + result.getAccountNumber());
        System.out.println("Job Number           : " + result.getJobNumber());
        System.out.println("Plans Analyzed       : " + result.getNumberOfQuotesAnalyzed());
        System.out.println("Processing Time      : " + result.getExecutionTimeMs() + " ms");

        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("   AI RECOMMENDATIONS  (Ranked Best → Worst)");
        System.out.println("═══════════════════════════════════════════\n");

        int shown = 0;
        for (QuoteRecommendation rec : result.getRecommendations()) {
            if (shown++ >= 3) break;

            System.out.println("─────────────────────────────────────────");
            System.out.printf("  RANK #%d  –  %s%n", rec.getRank(), rec.getPlanName());
            if (rec.getRecommendationLabel() != null)
                System.out.println("  " + rec.getRecommendationLabel());
            System.out.println("─────────────────────────────────────────");

            System.out.printf("%n  💰 PREMIUM      : $%,.2f%n", rec.getPremium());
            if (rec.getSavingsVsHighest() != null && rec.getSavingsVsHighest() > 0)
                System.out.printf("     Saves         : $%,.2f vs highest plan%n", rec.getSavingsVsHighest());

            CoveragePlan p = rec.getCoveragePlan();
            System.out.println("\n  📋 COVERAGE:");
            System.out.println("     Bodily Injury  : " + p.getBiLimit());
            System.out.println("     Property Dmg   : $" + p.getPdLimit() + "K");
            System.out.println("     Collision Ded  : " + (p.getCollisionDeductible() != null ? "$" + p.getCollisionDeductible() : "Not included"));
            System.out.println("     Comp Ded       : " + (p.getCompDeductible() != null ? "$" + p.getCompDeductible() : "Not included"));

            System.out.println("\n  🤖 AI SCORES:");
            System.out.printf("     Price           : %5.1f / 100%n", rec.getPriceScore());
            System.out.printf("     Coverage Fit    : %5.1f / 100%n", rec.getCoverageAdequacyScore());
            System.out.printf("     Value           : %5.1f / 100%n", rec.getValueScore());
            System.out.printf("     Risk Match      : %5.1f / 100%n", rec.getRiskMatchScore());
            System.out.println("     ────────────────────────────────");
            System.out.printf("     TOTAL SCORE     : %5.1f / 100  ★%n", rec.getTotalScore());

            System.out.println("\n  💡 WHY THIS PLAN:");
            for (String reason : rec.getReasoningPoints())
                System.out.println("     • " + reason);

            System.out.println();
        }

        if (result.getRecommendations().size() > 3)
            System.out.println("  (...and " + (result.getRecommendations().size() - 3) + " more options available)\n");
    }
}
