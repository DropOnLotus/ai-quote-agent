package com.guidewire.ai.agent.engine;

import com.guidewire.ai.agent.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RecommendationEngine {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationEngine.class);

    private static final double PRICE_WEIGHT            = 0.35;
    private static final double COVERAGE_ADEQUACY_WEIGHT = 0.30;
    private static final double VALUE_WEIGHT            = 0.20;
    private static final double RISK_MATCH_WEIGHT       = 0.15;

    // ── Main Entry Point ─────────────────────────────────────────

    public List<QuoteRecommendation> analyzeAndRecommend(List<QuotedPlan> quotes,
                                                          UserProfile profile,
                                                          UserPreferences prefs) {
        logger.info("Analyzing {} quotes", quotes.size());
        List<QuoteRecommendation> recs = new ArrayList<>();
        for (QuotedPlan q : quotes) recs.add(scoreQuote(q, profile, prefs, quotes));
        recs.sort(Comparator.comparingDouble(QuoteRecommendation::getTotalScore).reversed());

        for (int i = 0; i < recs.size(); i++) {
            recs.get(i).setRank(i + 1);
            if (i == 0)                                   recs.get(i).setRecommendationLabel("⭐ AI Recommended – Best Overall Value");
            else if (i == 1)                              recs.get(i).setRecommendationLabel("Strong Alternative");
            else if (Boolean.TRUE.equals(recs.get(i).getIsLowestPrice()))     recs.get(i).setRecommendationLabel("Lowest Price Option");
            else if (Boolean.TRUE.equals(recs.get(i).getIsHighestCoverage())) recs.get(i).setRecommendationLabel("Maximum Protection");
        }
        logger.info("Top recommendation: {}", recs.get(0).getPlanName());
        return recs;
    }

    // ── Scoring ──────────────────────────────────────────────────

    private QuoteRecommendation scoreQuote(QuotedPlan quote, UserProfile profile,
                                            UserPreferences prefs, List<QuotedPlan> all) {
        QuoteRecommendation rec = new QuoteRecommendation();
        rec.setPlanName(quote.getPlan().getPlanName());
        rec.setDescription(quote.getPlan().getDescription());
        rec.setPremium(quote.getTotalPremium());
        rec.setCoveragePlan(quote.getPlan());

        double ps  = calculatePriceScore(quote, all, prefs);
        double cs  = calculateCoverageAdequacyScore(quote, profile);
        double vs  = calculateValueScore(quote, all);
        double rs  = calculateRiskMatchScore(quote, profile);
        double tot = ps * PRICE_WEIGHT + cs * COVERAGE_ADEQUACY_WEIGHT + vs * VALUE_WEIGHT + rs * RISK_MATCH_WEIGHT;

        rec.setPriceScore(ps);
        rec.setCoverageAdequacyScore(cs);
        rec.setValueScore(vs);
        rec.setRiskMatchScore(rs);
        rec.setTotalScore(tot);
        rec.setReasoningPoints(generateReasoningPoints(quote, profile, prefs, all));
        rec.setIsLowestPrice(isLowestPrice(quote, all));
        rec.setIsHighestCoverage(isHighestCoverage(quote, all));
        rec.setIsBestValue(false); // updated after full ranking
        rec.setSavingsVsHighest(Math.max(0, findHighestPremium(all) - quote.getTotalPremium()));
        rec.setSavingsVsAverage(Math.max(0, calcAvgPremium(all) - quote.getTotalPremium()));
        return rec;
    }

    private double calculatePriceScore(QuotedPlan quote, List<QuotedPlan> all, UserPreferences prefs) {
        double min = findLowestPremium(all), max = findHighestPremium(all);
        double range = max - min;
        if (range == 0) return 100.0;
        double norm = (max - quote.getTotalPremium()) / range;
        double sens = prefs.getPriceSensitivity() / 5.0;
        return Math.min(100.0, norm * 100 * (0.5 + 0.5 * sens));
    }

    private double calculateCoverageAdequacyScore(QuotedPlan quote, UserProfile profile) {
        double s = 0;
        s += scoreLiability(quote.getPlan().getBiLimit(), quote.getPlan().getPdLimit(), profile);
        s += scorePhysicalDamage(quote.getPlan(), profile);
        s += scoreDeductible(quote.getPlan(), profile);
        return Math.min(100.0, s);
    }

    private double scoreLiability(String bi, String pd, UserProfile profile) {
        double s = 50;
        double nw = profile.getEstimatedNetWorth();
        if (nw > 500000) {
            if (bi != null && (bi.contains("250") || bi.contains("500"))) s += 20;
            if (pd != null && (pd.equals("250") || pd.equals("500"))) s += 10;
        } else if (nw > 100000) {
            if (bi != null && (bi.contains("100") || bi.contains("250"))) s += 15;
            if (pd != null && (pd.equals("100") || pd.equals("250"))) s += 8;
        } else {
            if (bi != null && (bi.contains("50") || bi.contains("100"))) s += 10;
            if (pd != null && (pd.equals("50") || pd.equals("100"))) s += 5;
        }
        return s;
    }

    private double scorePhysicalDamage(CoveragePlan plan, UserProfile profile) {
        boolean has = plan.getCollisionDeductible() != null;
        double val  = profile.getVehicleValue();
        if (val > 15000) return has ? 20 : 0;
        if (val > 5000)  return has ? 10 : 5;
        return has ? 5 : 10;
    }

    private double scoreDeductible(CoveragePlan plan, UserProfile profile) {
        double s = 10;
        if (plan.getCollisionDeductible() != null) {
            double d = plan.getCollisionDeductible(), ef = profile.getEmergencyFundAmount();
            if (d <= ef * 0.1)  s += 10;
            else if (d <= ef * 0.25) s += 5;
            else if (d > ef)    s -= 5;
        }
        return s;
    }

    private double calculateValueScore(QuotedPlan quote, List<QuotedPlan> all) {
        double pts  = coveragePoints(quote.getPlan());
        double eff  = pts / Math.max(quote.getTotalPremium(), 1);
        double maxE = all.stream()
                .mapToDouble(q -> coveragePoints(q.getPlan()) / Math.max(q.getTotalPremium(), 1))
                .max().orElse(1);
        return (eff / maxE) * 100;
    }

    private double coveragePoints(CoveragePlan p) {
        double pts = 0;
        if (p.getBiLimit() != null) {
            if (p.getBiLimit().contains("500"))      pts += 50;
            else if (p.getBiLimit().contains("250")) pts += 40;
            else if (p.getBiLimit().contains("100")) pts += 30;
            else if (p.getBiLimit().contains("50"))  pts += 20;
            else                                      pts += 10;
        }
        if (p.getPdLimit() != null) {
            try { pts += Integer.parseInt(p.getPdLimit().replaceAll("[^0-9]", "")) / 10.0; }
            catch (NumberFormatException ignored) {}
        }
        if (p.getCollisionDeductible() != null) pts += 20 - (p.getCollisionDeductible() / 100.0);
        if (p.getCompDeductible() != null)      pts += 20 - (p.getCompDeductible() / 100.0);
        return pts;
    }

    private double calculateRiskMatchScore(QuotedPlan quote, UserProfile profile) {
        double s = 50;
        int tier = quote.getPlan().getTier().ordinal();
        boolean highRisk = profile.getNumberOfViolations() > 1 || profile.getNumberOfAccidents() > 0;
        if (highRisk) {
            if (tier <= 1) s -= 20; else if (tier == 2) s += 10; else s += 20;
        } else {
            s += 15;
        }
        if (profile.getAge() != null && profile.getAge() < 25 && tier > 0) s += 10;
        if (profile.getAge() != null && profile.getAge() >= 55
                && profile.getNumberOfViolations() == 0 && tier <= 2) s += 15;
        return Math.min(100, s);
    }

    private List<String> generateReasoningPoints(QuotedPlan quote, UserProfile profile,
                                                   UserPreferences prefs, List<QuotedPlan> all) {
        List<String> pts = new ArrayList<>();
        if (isLowestPrice(quote, all))
            pts.add(String.format("Lowest price – saves $%.0f vs highest plan", findHighestPremium(all) - quote.getTotalPremium()));
        else if (quote.getTotalPremium() < calcAvgPremium(all))
            pts.add(String.format("Below average price – saves $%.0f vs average", calcAvgPremium(all) - quote.getTotalPremium()));

        if (isHighestCoverage(quote, all)) pts.add("Maximum protection – highest limits & lowest deductibles");

        double vs = calculateValueScore(quote, all);
        if (vs > 85) pts.add("Excellent value – strong coverage-to-cost ratio");
        else if (vs < 50) pts.add("Consider whether maximum protection is needed at this price");

        boolean highRisk = profile.getNumberOfAccidents() > 0 || profile.getNumberOfViolations() > 1;
        if (highRisk) {
            if (quote.getPlan().getTier().ordinal() >= CoveragePermutationEngine.PlanTier.STANDARD.ordinal())
                pts.add("Recommended for your driving history – provides important protection");
            else
                pts.add("Warning: Minimum coverage may be risky given recent violations/accidents");
        }
        if (profile.getEstimatedNetWorth() > 250000) {
            String bi = quote.getPlan().getBiLimit();
            if (bi != null && (bi.contains("250") || bi.contains("500")))
                pts.add("Liability limits appropriate for protecting your assets");
            else
                pts.add("Consider: your assets may exceed coverage limits in a serious accident");
        }
        boolean hasPhysDmg = quote.getPlan().getCollisionDeductible() != null;
        if (profile.getVehicleValue() > 15000 && hasPhysDmg)
            pts.add("Collision/Comprehensive included – important for your newer vehicle");
        else if (profile.getVehicleValue() < 5000 && !hasPhysDmg)
            pts.add("Liability-only – cost-effective choice for an older vehicle");
        return pts;
    }

    // ── Utilities ────────────────────────────────────────────────

    private boolean isLowestPrice(QuotedPlan q, List<QuotedPlan> all) {
        return q.getTotalPremium().equals(findLowestPremium(all));
    }

    private boolean isHighestCoverage(QuotedPlan q, List<QuotedPlan> all) {
        double max = all.stream().mapToDouble(p -> coveragePoints(p.getPlan())).max().orElse(0);
        return Math.abs(coveragePoints(q.getPlan()) - max) < 0.01;
    }

    private Double findLowestPremium(List<QuotedPlan> all) {
        return all.stream().mapToDouble(QuotedPlan::getTotalPremium).min().orElse(0);
    }

    private Double findHighestPremium(List<QuotedPlan> all) {
        return all.stream().mapToDouble(QuotedPlan::getTotalPremium).max().orElse(0);
    }

    private Double calcAvgPremium(List<QuotedPlan> all) {
        return all.stream().mapToDouble(QuotedPlan::getTotalPremium).average().orElse(0);
    }
}
