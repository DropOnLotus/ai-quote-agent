package com.guidewire.ai.agent.engine;

import com.guidewire.ai.agent.model.CoveragePlan;
import com.guidewire.ai.agent.model.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CoveragePermutationEngine {

    private static final Logger logger = LoggerFactory.getLogger(CoveragePermutationEngine.class);

    private static final String[] BI_LIMITS         = { "25/50", "50/100", "100/300", "250/500", "500/1000" };
    private static final String[] PD_LIMITS         = { "25", "50", "100", "250", "500" };
    private static final Integer[] COLLISION_DEDUCTIBLES = { 250, 500, 1000, 2500 };
    private static final Integer[] COMP_DEDUCTIBLES      = { 250, 500, 1000, 2500 };

    public enum PlanTier { MINIMUM, BASIC, STANDARD, PREMIUM, PLATINUM, CUSTOM }

    // ── Public API ────────────────────────────────────────────────

    public List<CoveragePlan> generateCoveragePlans() {
        List<CoveragePlan> plans = new ArrayList<>();
        plans.add(createPlan("Minimum",  "State minimum liability coverage – lowest cost",
                BI_LIMITS[0], PD_LIMITS[0], null, null, PlanTier.MINIMUM));
        plans.add(createPlan("Basic",    "Essential protection with higher deductibles",
                BI_LIMITS[1], PD_LIMITS[1], COLLISION_DEDUCTIBLES[2], COMP_DEDUCTIBLES[2], PlanTier.BASIC));
        plans.add(createPlan("Standard", "Balanced coverage – recommended for most drivers",
                BI_LIMITS[2], PD_LIMITS[2], COLLISION_DEDUCTIBLES[1], COMP_DEDUCTIBLES[1], PlanTier.STANDARD));
        plans.add(createPlan("Premium",  "Enhanced protection with low deductibles",
                BI_LIMITS[3], PD_LIMITS[3], COLLISION_DEDUCTIBLES[0], COMP_DEDUCTIBLES[0], PlanTier.PREMIUM));
        plans.add(createPlan("Platinum", "Maximum protection – best for high-value assets",
                BI_LIMITS[4], PD_LIMITS[4], COLLISION_DEDUCTIBLES[0], COMP_DEDUCTIBLES[0], PlanTier.PLATINUM));
        logger.info("Generated {} standard coverage plans", plans.size());
        return plans;
    }

    public List<CoveragePlan> generateCustomPlans(UserPreferences prefs) {
        List<CoveragePlan> plans = new ArrayList<>();
        PlanTier preferred = prefs.getPreferredTier();
        plans.add(generatePlanForTier(preferred));
        if (preferred != PlanTier.PLATINUM) plans.add(generatePlanForTier(getNextTier(preferred)));
        if (preferred != PlanTier.MINIMUM)  plans.add(generatePlanForTier(getPreviousTier(preferred)));
        if (preferred == PlanTier.STANDARD || preferred == PlanTier.PREMIUM) {
            plans.add(createDeductibleVariation(preferred, true));
            plans.add(createDeductibleVariation(preferred, false));
        }
        logger.info("Generated {} custom plans for tier {}", plans.size(), preferred);
        return plans;
    }

    public List<CoveragePlan> generateOptimizationVariations(String baseBI, String basePD,
                                                              Integer baseCollDed, Integer baseCompDed) {
        List<CoveragePlan> v = new ArrayList<>();
        v.add(createPlan("Current Selection", "Your selected coverage", baseBI, basePD, baseCollDed, baseCompDed, PlanTier.CUSTOM));
        v.add(createPlan("Lower Premium Option", "Same limits, higher deductibles",
                baseBI, basePD, getNextHigherDeductible(baseCollDed), getNextHigherDeductible(baseCompDed), PlanTier.CUSTOM));
        v.add(createPlan("Better Protection Option", "Higher limits, same deductibles",
                getNextHigherLimit(baseBI, BI_LIMITS), getNextHigherLimit(basePD, PD_LIMITS),
                baseCollDed, baseCompDed, PlanTier.CUSTOM));
        return v;
    }

    public boolean validateCombination(CoveragePlan plan, List<String> errors) {
        boolean valid = true;
        if (plan.getBiLimit() == null || plan.getBiLimit().isEmpty()) {
            errors.add("Bodily Injury liability is required"); valid = false;
        }
        if (plan.getPdLimit() == null || plan.getPdLimit().isEmpty()) {
            errors.add("Property Damage liability is required"); valid = false;
        }
        if (plan.getCollisionDeductible() != null && plan.getCompDeductible() == null) {
            errors.add("Comprehensive coverage required when Collision is selected"); valid = false;
        }
        return valid;
    }

    // ── Helpers ───────────────────────────────────────────────────

    private CoveragePlan createPlan(String name, String desc, String bi, String pd,
                                    Integer collDed, Integer compDed, PlanTier tier) {
        CoveragePlan p = new CoveragePlan();
        p.setPlanName(name); p.setDescription(desc);
        p.setBiLimit(bi);    p.setPdLimit(pd);
        p.setCollisionDeductible(collDed); p.setCompDeductible(compDed);
        p.setTier(tier);
        return p;
    }

    private CoveragePlan generatePlanForTier(PlanTier tier) {
        switch (tier) {
            case MINIMUM:  return createPlan("Minimum",  "State minimum", BI_LIMITS[0], PD_LIMITS[0], null, null, tier);
            case BASIC:    return createPlan("Basic",    "Essential",     BI_LIMITS[1], PD_LIMITS[1], 1000, 1000, tier);
            case STANDARD: return createPlan("Standard", "Balanced",      BI_LIMITS[2], PD_LIMITS[2], 500,  500,  tier);
            case PREMIUM:  return createPlan("Premium",  "Enhanced",      BI_LIMITS[3], PD_LIMITS[3], 250,  250,  tier);
            case PLATINUM: return createPlan("Platinum", "Maximum",       BI_LIMITS[4], PD_LIMITS[4], 250,  250,  tier);
            default:       return createPlan("Standard", "Default",       BI_LIMITS[2], PD_LIMITS[2], 500,  500,  tier);
        }
    }

    private CoveragePlan createDeductibleVariation(PlanTier tier, boolean higher) {
        int idx  = tier.ordinal();
        int dIdx = higher ? Math.min(3, idx + 1) : Math.max(0, idx - 1);
        return createPlan(
                tier.name() + (higher ? " – Higher Deductible" : " – Lower Deductible"),
                higher ? "Save on premium" : "Lower out-of-pocket costs",
                BI_LIMITS[Math.min(idx, BI_LIMITS.length - 1)],
                PD_LIMITS[Math.min(idx, PD_LIMITS.length - 1)],
                COLLISION_DEDUCTIBLES[dIdx], COMP_DEDUCTIBLES[dIdx], tier);
    }

    private PlanTier getNextTier(PlanTier t) {
        return PlanTier.values()[Math.min(t.ordinal() + 1, PlanTier.values().length - 2)];
    }

    private PlanTier getPreviousTier(PlanTier t) {
        return PlanTier.values()[Math.max(t.ordinal() - 1, 0)];
    }

    private Integer getNextHigherDeductible(Integer current) {
        if (current == null) return COLLISION_DEDUCTIBLES[0];
        for (int i = 0; i < COLLISION_DEDUCTIBLES.length - 1; i++)
            if (COLLISION_DEDUCTIBLES[i].equals(current)) return COLLISION_DEDUCTIBLES[i + 1];
        return current;
    }

    private String getNextHigherLimit(String current, String[] limits) {
        for (int i = 0; i < limits.length - 1; i++)
            if (limits[i].equals(current)) return limits[i + 1];
        return current;
    }
}
