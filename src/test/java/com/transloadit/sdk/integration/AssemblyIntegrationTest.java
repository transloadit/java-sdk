package com.transloadit.sdk.integration;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.response.AssemblyResponse;

import java.io.File;
import java.io.FileOutputStream;
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
        Assumptions.assumeTrue(key != null && !key.trim().isEmpty(), "TRANSLOADIT_KEY env var required");
        Assumptions.assumeTrue(secret != null && !secret.trim().isEmpty(), "TRANSLOADIT_SECRET env var required");

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

    @Test
    void directUploadOriginalStepProducesResult() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("JAVA_SDK_E2E")), "set JAVA_SDK_E2E=1 to run");

        String key = System.getenv("TRANSLOADIT_KEY");
        String secret = System.getenv("TRANSLOADIT_SECRET");
        Assumptions.assumeTrue(key != null && !key.trim().isEmpty(), "TRANSLOADIT_KEY env var required");
        Assumptions.assumeTrue(secret != null && !secret.trim().isEmpty(), "TRANSLOADIT_SECRET env var required");

        File upload = File.createTempFile("java-sdk-smartcdn", ".bin");
        try (java.io.InputStream in = AssemblyIntegrationTest.class.getResourceAsStream("/chameleon.jpg");
             FileOutputStream fos = new FileOutputStream(upload)) {
            if (in == null) {
                Assertions.fail("Embedded chameleon.jpg fixture missing");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            long targetSize = 32L * 1024L * 1024L;
            long current = upload.length();
            if (current < targetSize) {
                byte[] padding = new byte[8192];
                while (current < targetSize) {
                    long remaining = targetSize - current;
                    int toWrite = (int) Math.min(padding.length, remaining);
                    fos.write(padding, 0, toWrite);
                    current += toWrite;
                }
            }
        }

        Transloadit client = new Transloadit(key, secret);
        Assembly assembly = client.newAssembly();
        try {
            assembly.addFile(upload, "image");

        Map<String, Object> resize = new HashMap<>();
        resize.put("use", ":original");
        resize.put("width", 32);
        resize.put("height", 32);
        resize.put("resize_strategy", "fit");
        resize.put("format", "jpg");
        resize.put("result", true);
        assembly.addStep("resize", "/image/resize", resize);

        AssemblyResponse response = assembly.save(true);
        String assemblyId = response.getId();

        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        while (!response.isFinished() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5000);
            response = client.getAssembly(assemblyId);
        }

        Assertions.assertTrue(response.isFinished(), "Assembly did not finish in time: " + assemblyId);
        String ok = response.json().optString("ok");
        if (!"ASSEMBLY_COMPLETED".equals(ok)) {
            Assertions.fail("Assembly " + assemblyId + " finished with status=" + ok + " payload=" + response.json());
        }

        JSONArray resizeResult = response.getStepResult("resize");
        if (resizeResult == null) {
            Assertions.fail("resize step result missing for assembly " + assemblyId + " payload=" + response.json());
        }
        Assertions.assertTrue(resizeResult.length() > 0, "resize step result empty for assembly " + assemblyId);
        } finally {
            upload.delete();
        }
    }

}
