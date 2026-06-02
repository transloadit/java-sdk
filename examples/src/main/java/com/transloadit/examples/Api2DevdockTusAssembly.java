package com.transloadit.examples;

import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.response.AssemblyResponse;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.net.URL;
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

        JSONObject createRequest = scenario.getJSONObject("createTusAssembly").getJSONObject("request");
        AssemblyResponse created = transloadit.createAssembly(
                jsonObjectToMap(createRequest.getJSONObject("normalizedParams")),
                jsonObjectToStringMap(createRequest.getJSONObject("formFields")));
        JSONObject createResponse = created.json();

        String uploadUrl = uploadScenarioBytes(scenario, createResponse);

        JSONObject result = new JSONObject();
        result.put("createResponse", createResponse);
        result.put("uploadUrl", uploadUrl);
        writeResult(result);

        System.out.println("Java SDK devdock scenario " + scenario.getString("scenarioId")
                + " uploaded to " + uploadUrl);
    }

    private static JSONObject loadScenario() throws Exception {
        String scenarioPath = System.getenv("API2_SDK_EXAMPLE_SCENARIO");
        if (scenarioPath == null || scenarioPath.isEmpty()) {
            scenarioPath = "examples/api2-devdock-tus-assembly/api2-scenario.json";
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

    private static String uploadScenarioBytes(JSONObject scenario, JSONObject createResponse) throws Exception {
        JSONObject uploadConfig = scenario.getJSONObject("upload");
        JSONObject source = uploadConfig.getJSONObject("source");
        byte[] bytes = source.getString("value").getBytes(StandardCharsets.UTF_8);

        TusClient tusClient = new TusClient();
        tusClient.setUploadCreationURL(new URL(createResponse.getString("tus_url")));

        TusUpload upload = new TusUpload();
        upload.setInputStream(new ByteArrayInputStream(bytes));
        upload.setSize(bytes.length);
        upload.setFingerprint("api2-devdock-java-sdk-" + createResponse.getString("assembly_id"));
        upload.setMetadata(uploadMetadata(scenario, createResponse));

        TusUploader uploader = tusClient.createUpload(upload);
        uploader.setChunkSize(bytes.length);
        while (uploader.uploadChunk() > -1) {
            // Upload the single scenario-owned source until tus-java-client reports completion.
        }
        uploader.finish(false);

        return uploader.getUploadURL().toString();
    }

    private static Map<String, String> uploadMetadata(
            JSONObject scenario,
            JSONObject createResponse) {
        Map<String, String> metadata = new HashMap<String, String>();
        JSONArray fields = scenario.getJSONObject("upload").getJSONArray("metadata");
        for (int index = 0; index < fields.length(); index += 1) {
            JSONObject field = fields.getJSONObject(index);
            metadata.put(field.getString("name"), String.valueOf(resolveScenarioValue(
                    field.getJSONObject("value"),
                    scenario,
                    createResponse)));
        }

        return metadata;
    }

    private static Object resolveScenarioValue(
            JSONObject value,
            JSONObject scenario,
            JSONObject createResponse) {
        if (value.has("value")) {
            return value.get("value");
        }

        JSONObject source = value.getJSONObject("source");
        Object current;
        if ("scenario".equals(source.getString("root"))) {
            current = scenario;
        } else if ("createResponse".equals(source.getString("root"))) {
            current = createResponse;
        } else {
            throw new IllegalStateException("Unsupported scenario value root: " + source.getString("root"));
        }

        JSONArray path = source.getJSONArray("path");
        for (int index = 0; index < path.length(); index += 1) {
            if (!(current instanceof JSONObject)) {
                throw new IllegalStateException("Cannot resolve scenario path through non-object value");
            }

            current = ((JSONObject) current).get(path.getString(index));
        }

        return current;
    }

    private static Map<String, Object> jsonObjectToMap(JSONObject object) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Iterator<String> keys = object.keys(); keys.hasNext();) {
            String key = keys.next();
            Object value = object.get(key);
            if (value instanceof JSONObject) {
                value = jsonObjectToMap((JSONObject) value);
            }
            map.put(key, value);
        }

        return map;
    }

    private static Map<String, String> jsonObjectToStringMap(JSONObject object) {
        Map<String, String> map = new HashMap<String, String>();
        for (Iterator<String> keys = object.keys(); keys.hasNext();) {
            String key = keys.next();
            map.put(key, String.valueOf(object.get(key)));
        }

        return map;
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
