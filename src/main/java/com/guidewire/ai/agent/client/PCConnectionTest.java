package com.guidewire.ai.agent.client;

import com.guidewire.ai.agent.config.PCConfig;

/**
 * Quick connectivity + authentication smoke test.
 *
 * Run this class FIRST in IntelliJ to verify that:
 *   1. PolicyCenter is reachable on the configured URL
 *   2. The Basic Auth credentials are accepted
 *
 * Right-click → Run 'PCConnectionTest.main()'
 *
 * If you see HTTP 200 or 404  → server is up, credentials valid
 * If you see HTTP 401          → wrong username / password
 * If you see HTTP 403          → user lacks REST permission in PC
 * If you see ConnectException  → PC is not running or wrong URL/port
 */
public class PCConnectionTest {

    public static void main(String[] args) {

        PCConfig config = PCConfig.getInstance();

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   PolicyCenter Connection Test               ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println("  URL      : " + config.getBaseUrl());
        System.out.println("  Username : " + config.getUsername());
        System.out.println("  Password : " + "*".repeat(config.getPassword().length()));
        System.out.println();

        PolicyCenterRestClient client = new PolicyCenterRestClient();

        try {
            boolean ok = client.testConnection();
            if (ok) {
                System.out.println("✅  SUCCESS – PolicyCenter is reachable and auth is accepted.");
                System.out.println("    You are ready to run AIQuoteAgentExample.");
            } else {
                System.out.println("❌  FAILED  – Could not reach PolicyCenter.");
                System.out.println("    • Confirm PC is running.");
                System.out.println("    • Check pc.base.url in application.properties.");
            }
        } catch (Exception e) {
            System.out.println("❌  ERROR – " + e.getMessage());
            printHelp(e.getMessage());
        } finally {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private static void printHelp(String message) {
        System.out.println();
        System.out.println("─── Troubleshooting ───────────────────────────");
        if (message != null && message.contains("401")) {
            System.out.println("  HTTP 401 Unauthorized:");
            System.out.println("  → Edit  src/main/resources/application.properties");
            System.out.println("  → Set   pc.auth.username and pc.auth.password");
            System.out.println("  → Default dev credentials are  su / gw");
        } else if (message != null && message.contains("403")) {
            System.out.println("  HTTP 403 Forbidden:");
            System.out.println("  → The user exists but lacks REST API permission.");
            System.out.println("  → In PC Admin: System > User Roles > assign REST role");
            System.out.println("    or use the 'su' superuser account.");
        } else if (message != null && (message.contains("Connection refused")
                                    || message.contains("ConnectException"))) {
            System.out.println("  Connection refused:");
            System.out.println("  → Is PolicyCenter running?  (check server console)");
            System.out.println("  → Is the port correct?  default is 8180");
            System.out.println("  → Edit pc.base.url in application.properties");
        } else {
            System.out.println("  • Check that PolicyCenter is fully started");
            System.out.println("  • Verify application.properties values");
            System.out.println("  • Check PC server logs for more detail");
        }
        System.out.println("────────────────────────────────────────────────");
    }
}
