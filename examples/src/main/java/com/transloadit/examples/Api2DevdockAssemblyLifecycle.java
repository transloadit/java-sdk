package com.transloadit.examples;

import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs API2's contract-owned Assembly lifecycle scenario against devdock.
 */
public final class Api2DevdockAssemblyLifecycle {
    /**
     * Runs the Assembly lifecycle scenario.
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

        AssemblyResponse created = transloadit.createTusAssembly(
                scenario.getJSONObject("assembly").getInt("fileCount"));
        String createdAssemblySslUrl = created.getSslUrl();
        boolean cancelAssembly = true;

        try {
            AssemblyResponse fetched = transloadit.getAssemblyByUrl(createdAssemblySslUrl);

            Map<String, Object> listOptions = new HashMap<String, Object>();
            listOptions.put("assembly_id", created.getId());
            listOptions.put("pagesize", scenario.getJSONObject("list").getInt("pageSize"));
            ListResponse listed = transloadit.listAssemblies(listOptions);

            AssemblyResponse cancelled = transloadit.cancelAssembly(createdAssemblySslUrl);
            cancelAssembly = false;

            JSONObject result = new JSONObject();
            result.put("cancelled", assemblyResult(cancelled));
            result.put("created", assemblyResult(created));
            result.put("fetched", assemblyResult(fetched));
            result.put("listContainsCreated", listContainsAssembly(listed.getItems(), created.getId()));
            result.put("listCount", listed.size());
            writeResult(result);
        } finally {
            if (cancelAssembly) {
                transloadit.cancelAssembly(createdAssemblySslUrl);
            }
        }

        System.out.println("Java SDK devdock scenario " + scenario.getString("scenarioId")
                + " canceled Assembly " + created.getId());
    }

    private static JSONObject assemblyResult(AssemblyResponse response) {
        JSONObject result = new JSONObject();
        result.put("assemblyId", response.getId());
        result.put("assemblySslUrl", response.getSslUrl());
        result.put("assemblyUrl", response.getUrl());
        result.put("ok", response.json().optString("ok", ""));
        return result;
    }

    private static boolean listContainsAssembly(JSONArray items, String assemblyId) {
        for (int index = 0; index < items.length(); index += 1) {
            if (assemblyId.equals(items.getJSONObject(index).optString("id"))) {
                return true;
            }
        }

        return false;
    }

    private static JSONObject loadScenario() throws Exception {
        String scenarioPath = System.getenv("API2_SDK_EXAMPLE_SCENARIO");
        if (scenarioPath == null || scenarioPath.isEmpty()) {
            scenarioPath = "examples/api2-devdock-assembly-lifecycle/api2-scenario.json";
        }

        byte[] contents = Files.readAllBytes(Paths.get(scenarioPath));
        return new JSONObject(new String(contents, StandardCharsets.UTF_8));
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(name + " must be set");
        }

        return value;
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

    private Api2DevdockAssemblyLifecycle() {
        throw new IllegalStateException("Utility class");
    }
}
