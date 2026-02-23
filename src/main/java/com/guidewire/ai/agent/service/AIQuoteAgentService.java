package com.guidewire.ai.agent.service;

import com.guidewire.ai.agent.client.PolicyCenterRestClient;
import com.guidewire.ai.agent.engine.CoveragePermutationEngine;
import com.guidewire.ai.agent.engine.RecommendationEngine;
import com.guidewire.ai.agent.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

public class AIQuoteAgentService {

    private static final Logger logger = LoggerFactory.getLogger(AIQuoteAgentService.class);

    private final PolicyCenterRestClient  pcClient;
    private final CoveragePermutationEngine coverageEngine;
    private final RecommendationEngine    recommendationEngine;
    private final ExecutorService         executor;

    public AIQuoteAgentService() {
        this.pcClient            = new PolicyCenterRestClient();
        this.coverageEngine      = new CoveragePermutationEngine();
        this.recommendationEngine = new RecommendationEngine();
        this.executor            = Executors.newFixedThreadPool(5);
    }

    // ── Main entry point ─────────────────────────────────────────

    public QuoteAnalysisResult generateQuotesWithRecommendations(CustomerInfo customerInfo,
                                                                   UserPreferences prefs) throws Exception {
        logger.info("Starting AI Quote Agent for {} {}", customerInfo.getFirstName(), customerInfo.getLastName());
        long start = System.currentTimeMillis();

        // 1. Create Account
        logger.info("Step 1: Creating account...");
        String accountNumber = createAccount(customerInfo);

        // 2. Create Submission
        logger.info("Step 2: Creating submission...");
        String jobNumber = createSubmission(accountNumber, customerInfo.getLineOfBusiness());

        // 3. Generate coverage plan variations
        logger.info("Step 3: Generating coverage plans...");
        List<CoveragePlan> plans = generateCoveragePlans(prefs);

        // 4. Quote all plans in parallel
        logger.info("Step 4: Quoting {} plans in parallel...", plans.size());
        List<QuotedPlan> quotedPlans = quoteAllPlansParallel(jobNumber, plans, customerInfo);

        // 5. Build risk profile
        logger.info("Step 5: Building user risk profile...");
        UserProfile profile = buildUserProfile(customerInfo);

        // 6. AI recommendations
        logger.info("Step 6: Generating AI recommendations...");
        List<QuoteRecommendation> recommendations =
                recommendationEngine.analyzeAndRecommend(quotedPlans, profile, prefs);

        long elapsed = System.currentTimeMillis() - start;
        logger.info("Completed in {} ms", elapsed);

        return buildResult(accountNumber, jobNumber, recommendations, elapsed);
    }

    /** Bind the policy selected by the user */
    public String bindPolicy(String jobNumber) throws IOException {
        logger.info("Binding policy for JobNumber: {}", jobNumber);
        PolicyIssuanceRequest req = new PolicyIssuanceRequest();
        req.setJobNumber(jobNumber);
        PolicyIssuanceResponse resp = pcClient.issuePolicy(req);
        logger.info("Policy issued: {}", resp.getPolicyNumber());
        return resp.getPolicyNumber();
    }

    // ── Steps ────────────────────────────────────────────────────

    private String createAccount(CustomerInfo c) throws IOException {
        CreateAccountRequest req = new CreateAccountRequest();
        
        // Build nested AccountHolder
        CreateAccountRequest.AccountHolder holder = new CreateAccountRequest.AccountHolder();
        holder.setFirstname(c.getFirstName());
        holder.setLastname(c.getLastName());
        holder.setDateofbirth(c.getDateOfBirth());
        req.setAccountholder(holder);
        
        // Set account-level fields
        req.setAccountname(c.getFirstName() + " " + c.getLastName());
        // accountnumber is optional - PC will generate if not provided
        req.setEmailaddress(c.getEmail());
        req.setPhonenumber(c.getPhoneNumber());
        
        // Build nested PrimaryAddress from CustomerInfo Address
        if (c.getAddress() != null) {
            CreateAccountRequest.PrimaryAddress addr = new CreateAccountRequest.PrimaryAddress();
            addr.setAddressline1(c.getAddress().getAddressline1());
            addr.setCity(c.getAddress().getCity());
            addr.setState(c.getAddress().getState());
            addr.setPostalcode(c.getAddress().getPostalcode());
            addr.setCountry("US");  // default to US, can be made configurable
            req.setPrimaryaddress(addr);
        }
        
        CreateAccountResponse resp = pcClient.createAccount(req);
        logger.info("Account created: {}", resp.getAccountnumber());
        return resp.getAccountnumber();
    }

    private String createSubmission(String accountNumber, String lob) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SubmissionRequest req = new SubmissionRequest();
        req.setAccountNumber(accountNumber);
        req.setEffectiveDate(sdf.format(new Date()));
        req.setProducerCode("100-002541");
        req.setProductCode("PersonalAuto");
        SubmissionResponse resp = pcClient.createSubmission(req);
        logger.info("Submission created: JobNumber={}, PolicyNumber={}", resp.getJobNumber(), resp.getPolicyNumber());
        return resp.getJobNumber();
    }

    private List<CoveragePlan> generateCoveragePlans(UserPreferences prefs) {
        List<CoveragePlan> plans = prefs.getPreferredTier() != null
                ? coverageEngine.generateCustomPlans(prefs)
                : coverageEngine.generateCoveragePlans();

        List<CoveragePlan> valid = new ArrayList<>();
        for (CoveragePlan plan : plans) {
            List<String> errors = new ArrayList<>();
            if (coverageEngine.validateCombination(plan, errors)) valid.add(plan);
            else logger.warn("Invalid plan '{}': {}", plan.getPlanName(), errors);
        }
        logger.info("{} valid coverage plans ready", valid.size());
        return valid;
    }

    private List<QuotedPlan> quoteAllPlansParallel(String jobNumber,
                                                    List<CoveragePlan> plans,
                                                    CustomerInfo c) throws Exception {
        List<Future<QuotedPlan>> futures = new ArrayList<>();
        for (CoveragePlan plan : plans)
            futures.add(executor.submit(() -> quoteSinglePlan(jobNumber, plan, c)));

        List<QuotedPlan> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                results.add(futures.get(i).get(30, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                logger.error("Quote timeout for plan: {}", plans.get(i).getPlanName());
            } catch (Exception e) {
                logger.error("Quote failed for plan: {}", plans.get(i).getPlanName(), e);
            }
        }
        logger.info("Quoted {}/{} plans successfully", results.size(), plans.size());
        return results;
    }

    private QuotedPlan quoteSinglePlan(String jobNumber, CoveragePlan plan, CustomerInfo c) throws IOException {
        SubmissionQuoteRequest req = new SubmissionQuoteRequest();
        req.setJobNumber(jobNumber);

        PolicyDetails pd = new PolicyDetails();
        pd.setTermType("Annual");
        pd.setEffectiveDate(c.getDesiredEffectiveDate());
        req.setPolicyDetails(pd);

        PersonalAutoDetails paDetails = new PersonalAutoDetails();
        paDetails.setDrivers(c.getDrivers());
        paDetails.setVehicles(c.getVehicles());

        PACoverages cov = new PACoverages();
        cov.setBiLimit(plan.getBiLimit());
        cov.setPdLimit(plan.getPdLimit());
        cov.setCollisionDeductible(plan.getCollisionDeductible());
        cov.setCompDeductible(plan.getCompDeductible());
        paDetails.setCoverages(cov);
        req.setPaDetails(paDetails);

        SubmissionQuoteResponse resp = pcClient.quoteSubmission(req);

        QuotedPlan qp = new QuotedPlan();
        qp.setPlan(plan);
        qp.setJobNumber(resp.getJobNumber());
        qp.setTotalPremium(resp.getQuoteDetails().getTotalPremium());
        qp.setTotalCost(resp.getQuoteDetails().getTotalCost());
        qp.setCosts(resp.getQuoteDetails().getCosts());
        logger.debug("Plan '{}' quoted at ${}", plan.getPlanName(), qp.getTotalPremium());
        return qp;
    }

    private UserProfile buildUserProfile(CustomerInfo c) {
        UserProfile p = new UserProfile();
        if (c.getDateOfBirth() != null) {
            try {
                int year = Integer.parseInt(c.getDateOfBirth().split("-")[0]);
                p.setAge(java.time.Year.now().getValue() - year);
            } catch (Exception ignored) { p.setAge(30); }
        }
        if (c.getDrivers() != null && !c.getDrivers().isEmpty()) {
            PADriver d = c.getDrivers().get(0);
            p.setNumberOfViolations(d.getNumberOfViolations());
            p.setNumberOfAccidents(d.getNumberOfAccidents());
        }
        if (c.getVehicles() != null && !c.getVehicles().isEmpty())
            p.setVehicleValue(c.getVehicles().get(0).getVehicleCost());

        // Estimate net worth – extend with real data source as needed
        double nw = 100000;
        if (c.getVehicles() != null && !c.getVehicles().isEmpty()) {
            double val = c.getVehicles().get(0).getVehicleCost();
            if (val > 40000) nw += 200000;
            else if (val > 25000) nw += 100000;
        }
        p.setEstimatedNetWorth(nw);
        p.setEmergencyFundAmount(5000.0);
        return p;
    }

    private QuoteAnalysisResult buildResult(String accountNumber, String jobNumber,
                                             List<QuoteRecommendation> recs, long elapsed) {
        QuoteAnalysisResult r = new QuoteAnalysisResult();
        r.setAccountNumber(accountNumber);
        r.setJobNumber(jobNumber);
        r.setRecommendations(recs);
        r.setNumberOfQuotesAnalyzed(recs.size());
        r.setExecutionTimeMs(elapsed);
        if (!recs.isEmpty()) r.setTopRecommendation(recs.get(0));
        return r;
    }

    public void shutdown() {
        logger.info("Shutting down AI Quote Agent Service");
        executor.shutdown();
        try { pcClient.close(); } catch (IOException e) { logger.error("Error closing client", e); }
    }
}
