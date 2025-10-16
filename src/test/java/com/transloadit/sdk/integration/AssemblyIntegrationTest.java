package com.transloadit.sdk.integration;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AssemblyIntegrationTest {

    @Test
    void createAssemblyAndWaitForCompletion() throws Exception {
        String key = System.getenv("TRANSLOADIT_KEY");
        String secret = System.getenv("TRANSLOADIT_SECRET");
        Assumptions.assumeTrue(key != null && !key.isBlank(), "TRANSLOADIT_KEY env var required");
        Assumptions.assumeTrue(secret != null && !secret.isBlank(), "TRANSLOADIT_SECRET env var required");

        Transloadit client = new Transloadit(key, secret);
        Assembly assembly = client.newAssembly();

        Map<String, Object> importStep = new HashMap<>();
        importStep.put("url", "https://demos.transloadit.com/inputs/chameleon.jpg");
        assembly.addStep("import", "/http/import", importStep);

        Map<String, Object> resizeStep = new HashMap<>();
        resizeStep.put("use", "import");
        resizeStep.put("width", 32);
        resizeStep.put("height", 32);
        assembly.addStep("resize", "/image/resize", resizeStep);

        AssemblyResponse response = assembly.save(false);
        String assemblyId = response.getId();

        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        while (!response.isFinished() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5000);
            response = client.getAssembly(assemblyId);
        }

        Assertions.assertTrue(response.isFinished(), "Assembly did not finish in time");
        Assertions.assertEquals("ASSEMBLY_COMPLETED", response.json().optString("ok"));

        JSONArray stepResult = response.getStepResult("resize");
        Assertions.assertNotNull(stepResult, "resize step result missing");
        Assertions.assertTrue(stepResult.length() > 0, "resize step result empty");
    }
}
