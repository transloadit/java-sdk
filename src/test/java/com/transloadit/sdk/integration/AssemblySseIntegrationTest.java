package com.transloadit.sdk.integration;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.AssemblyListener;
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AssemblySseIntegrationTest {

    @Test
    void sseStreamShouldCloseWithoutErrorsAfterAssemblyFinished() throws Exception {
        String key = System.getenv("TRANSLOADIT_KEY");
        String secret = System.getenv("TRANSLOADIT_SECRET");
        Assumptions.assumeTrue(key != null && !key.trim().isEmpty(), "TRANSLOADIT_KEY env var required");
        Assumptions.assumeTrue(secret != null && !secret.trim().isEmpty(), "TRANSLOADIT_SECRET env var required");

        Transloadit client = new Transloadit(key, secret);
        Assembly assembly = client.newAssembly();

        Path tempFile = createTempUpload();
        try {
            assembly.addFile(tempFile.toFile(), "file");

            Map<String, Object> resizeStep = new HashMap<>();
            resizeStep.put("use", ":original");
            resizeStep.put("width", 64);
            resizeStep.put("height", 64);
            resizeStep.put("resize_strategy", "fit");
            assembly.addStep("resize", "/image/resize", resizeStep);

            AtomicReference<AssemblyResponse> finishedResponse = new AtomicReference<>();
            CompletableFuture<Void> finishedFuture = new CompletableFuture<>();
            CompletableFuture<Exception> errorFuture = new CompletableFuture<>();

            AssemblyListener listener = new AssemblyListener() {
                @Override
                public void onAssemblyFinished(AssemblyResponse response) {
                    System.out.println("[AssemblySseIntegrationTest] assembly_finished event");
                    finishedResponse.set(response);
                    finishedFuture.complete(null);
                }

                @Override
                public void onError(Exception error) {
                    System.out.println("[AssemblySseIntegrationTest] SSE error: " + error);
                    errorFuture.complete(error);
                    finishedFuture.completeExceptionally(error);
                }

                @Override
                public void onMetadataExtracted() {
                }

                @Override
                public void onAssemblyUploadFinished() {
                }

                @Override
                public void onFileUploadFinished(JSONObject uploadInformation) {
                }

                @Override
                public void onFileUploadPaused(String name) {
                }

                @Override
                public void onFileUploadResumed(String name) {
                }

                @Override
                public void onFileUploadProgress(long uploadedBytes, long totalBytes) {
                }

                @Override
                public void onAssemblyProgress(JSONObject progress) {
                }

                @Override
                public void onAssemblyResultFinished(JSONArray result) {
                }
            };

            assembly.setAssemblyListener(listener);

            AssemblyResponse initialResponse = assembly.save(true);
            assertNotNull(initialResponse.getId(), "Assembly ID should be present");

            try {
                finishedFuture.get(5, TimeUnit.MINUTES);
            } catch (ExecutionException executionException) {
                Throwable cause = executionException.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                throw executionException;
            }

            AssemblyResponse completed = finishedResponse.get();
            assertNotNull(completed, "Assembly finished response missing");
            assertTrue(completed.isFinished(), "Assembly should be finished");
            assertEquals("ASSEMBLY_COMPLETED", completed.json().optString("ok"));

            try {
                Exception unexpected = errorFuture.get(30, TimeUnit.SECONDS);
                fail("Unexpected SSE error after completion: " + unexpected);
            } catch (TimeoutException ignore) {
                // expected: no error surfaced after assembly finished
            }
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignore) {
            }
        }
    }

    private static Path createTempUpload() throws IOException {
        Path file = Files.createTempFile("transloadit-sse-test", ".jpg");
        URL source = new URL("https://demos.transloadit.com/inputs/chameleon.jpg");
        try (InputStream input = source.openStream(); OutputStream output = Files.newOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        return file;
    }
}
