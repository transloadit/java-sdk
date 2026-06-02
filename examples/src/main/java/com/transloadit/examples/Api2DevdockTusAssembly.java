package com.transloadit.examples;

import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.UploadTusAssemblyResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Runs API2's contract-owned TUS Assembly scenario against devdock.
 */
public final class Api2DevdockTusAssembly {
    /**
     * Runs the TUS Assembly scenario.
     *
     * @param args ignored
     * @throws Exception if the scenario cannot be completed
     */
    public static void main(String[] args) throws Exception {
        JSONObject scenario = loadScenario();
        Transloadit transloadit = new Transloadit(
                requiredEnv("TRANSLOADIT_KEY"),
                requiredEnv("TRANSLOADIT_SECRET"),
                requiredEnv("TRANSLOADIT_ENDPOINT"));

        int fileCount = featureStep(
                scenario,
                "preparations",
                "createTusAssembly",
                "feature-call")
                .getJSONObject("input")
                .getInt("file_count");

        JSONObject uploadConfig = scenario.getJSONObject("upload");
        JSONObject source = uploadConfig.getJSONObject("source");
        byte[] bytes = source.getString("value").getBytes(StandardCharsets.UTF_8);
        UploadTusAssemblyResult uploadResult = transloadit.uploadTusAssembly(
                fileCount,
                bytes,
                uploadConfig.getString("fieldName"),
                uploadConfig.getString("fileName"),
                uploadUserMeta(uploadConfig));
        JSONObject status = uploadResult.getAssembly().json();

        JSONObject result = new JSONObject();
        result.put("createResponse", status);
        result.put("status", status);
        result.put("uploadUrl", uploadResult.getUploadUrl());
        writeResult(result);

        System.out.println("Java SDK devdock scenario " + scenario.getString("scenarioId")
                + " uploaded to " + uploadResult.getUploadUrl()
                + " and finished with " + status.getString("ok"));
    }

    private static JSONObject loadScenario() throws Exception {
        String scenarioPath = System.getenv("API2_SDK_EXAMPLE_SCENARIO");
        if (scenarioPath == null || scenarioPath.isEmpty()) {
            scenarioPath = "examples/api2-devdock-tus-assembly/api2-scenario.json";
        }

        byte[] contents = Files.readAllBytes(Paths.get(scenarioPath));
        return new JSONObject(new String(contents, StandardCharsets.UTF_8));
    }

    private static JSONObject featureStep(
            JSONObject scenario,
            String collectionName,
            String featureId,
            String kind) {
        JSONArray steps = scenario.getJSONArray(collectionName);
        for (int index = 0; index < steps.length(); index += 1) {
            JSONObject step = steps.getJSONObject(index);
            if (!featureId.equals(step.getString("featureId"))) {
                continue;
            }
            if (!kind.equals(step.getString("kind"))) {
                throw new IllegalStateException(collectionName + "[" + index
                        + "] must have kind " + kind);
            }

            return step;
        }

        throw new IllegalStateException("Scenario has no " + collectionName
                + " step for feature " + featureId);
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(name + " must be set");
        }

        return value;
    }

    private static Map<String, String> uploadUserMeta(JSONObject uploadConfig) {
        Map<String, String> metadata = new HashMap<String, String>();
        if (!uploadConfig.has("userMeta")) {
            return metadata;
        }

        JSONObject userMeta = uploadConfig.getJSONObject("userMeta");
        for (Iterator<String> keys = userMeta.keys(); keys.hasNext();) {
            String key = keys.next();
            metadata.put(key, String.valueOf(userMeta.get(key)));
        }

        return metadata;
    }

    private static void writeResult(JSONObject result) throws Exception {
        String resultPath = System.getenv("API2_SDK_EXAMPLE_RESULT");
        if (resultPath == null || resultPath.isEmpty()) {
            return;
        }

        Files.write(
                Paths.get(resultPath),
                (result.toString(2) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private Api2DevdockTusAssembly() {
        throw new IllegalStateException("Utility class");
    }
}
