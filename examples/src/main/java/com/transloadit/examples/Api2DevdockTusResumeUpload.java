package com.transloadit.examples;

import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs API2's contract-owned TUS resume scenario against devdock.
 *
 * <p>This example is intentionally checked into the SDK repository: it reads the
 * API/TUS facts from API2's injected scenario JSON, interrupts an upload like an
 * unlucky user would, and resumes it through the public SDK method.</p>
 */
public final class Api2DevdockTusResumeUpload {
    /**
     * Runs the TUS resume scenario.
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

        JSONObject createResponse = scenario.getJSONObject("prepared").getJSONObject("createResponse");
        JSONObject upload = scenario.getJSONObject("upload");
        JSONObject resume = upload.getJSONObject("resume");

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("createResponse", createResponse);
        context.put("scenario", scenario);

        byte[] content = scenarioBytes(upload);
        String tusUrl = String.valueOf(resolveValue(upload.get("tusUrl"), context, "upload.tusUrl"));
        Map<String, String> metadata = uploadMetadata(upload, context);

        String firstUploadUrl = createInterruptedUpload(
                tusUrl,
                content,
                metadata,
                resume.getInt("stopAfterAcceptedBytes"));

        // Remember the interrupted upload by fingerprint, like a TUS client URL storage would.
        Map<String, String> storedUploads = new HashMap<String, String>();
        storedUploads.put(resume.getString("fingerprint"), firstUploadUrl);
        int previousUploadCount = storedUploads.size();

        AssemblyResponse completedAssembly = transloadit.resumeTusUpload(
                storedUploads.get(resume.getString("fingerprint")),
                content,
                createResponse.getString("assembly_ssl_url"));
        JSONObject status = completedAssembly.json();
        if (status.has("error") && !status.isNull("error")) {
            throw new IllegalStateException("resumeTusUpload returned " + status.getString("error")
                    + ": " + status.optString("message"));
        }

        if (resume.getBoolean("removeFingerprintOnSuccess")) {
            storedUploads.remove(resume.getString("fingerprint"));
        }
        int remainingPreviousUploadCount = storedUploads.size();

        JSONObject result = new JSONObject();
        result.put("firstUploadUrl", firstUploadUrl);
        result.put("previousUploadCount", previousUploadCount);
        result.put("remainingPreviousUploadCount", remainingPreviousUploadCount);
        result.put("uploadUrl", firstUploadUrl);
        writeResult(result);

        System.out.println("Java SDK devdock scenario "
                + scenario.getJSONObject("exampleInput").getString("scenarioId")
                + " resumed " + firstUploadUrl);
    }

    private static JSONObject loadScenario() throws Exception {
        String scenarioPath = System.getenv("API2_SDK_EXAMPLE_SCENARIO");
        if (scenarioPath == null || scenarioPath.isEmpty()) {
            scenarioPath = "examples/api2-devdock-tus-resume-upload/api2-scenario.json";
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

    private static Object resolveValue(Object valueSpec, Map<String, Object> context, String label) {
        if (!(valueSpec instanceof JSONObject)) {
            throw new IllegalStateException(label + " value spec must be an object");
        }

        JSONObject spec = (JSONObject) valueSpec;
        if (spec.has("value")) {
            return spec.get("value");
        }

        JSONObject source = spec.getJSONObject("source");
        Object current = context.get(source.getString("root"));
        if (current == null) {
            throw new IllegalStateException(label + " value source root is unavailable");
        }
        JSONArray pathParts = source.getJSONArray("path");
        for (int index = 0; index < pathParts.length(); index += 1) {
            String part = pathParts.getString(index);
            if (!(current instanceof JSONObject) || !((JSONObject) current).has(part)) {
                throw new IllegalStateException(label + " value source cannot read " + part);
            }
            current = ((JSONObject) current).get(part);
        }

        return current;
    }

    private static byte[] scenarioBytes(JSONObject upload) {
        JSONObject source = upload.getJSONObject("source");
        if (!"bytes".equals(source.getString("kind"))) {
            throw new IllegalStateException("upload.source.kind must be bytes");
        }
        if (!"utf8".equals(source.getString("encoding"))) {
            throw new IllegalStateException("upload.source.encoding must be utf8");
        }

        return source.getString("value").getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, String> uploadMetadata(JSONObject upload, Map<String, Object> context) {
        Map<String, String> metadata = new LinkedHashMap<String, String>();
        JSONArray fields = upload.getJSONArray("metadata");
        for (int index = 0; index < fields.length(); index += 1) {
            JSONObject field = fields.getJSONObject(index);
            String name = field.getString("name");
            metadata.put(name, String.valueOf(resolveValue(field.get("value"), context, name)));
        }

        return metadata;
    }

    /**
     * Creates a TUS upload and only sends the first chunk, leaving the upload
     * interrupted the way a dropped connection would.
     */
    private static String createInterruptedUpload(
            String tusUrl,
            byte[] content,
            Map<String, String> metadata,
            int stopAfterAcceptedBytes) throws Exception {
        List<String> metadataParts = new ArrayList<String>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            metadataParts.add(entry.getKey() + " "
                    + Base64.getEncoder().encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8)));
        }

        URL createUrl = new URL(tusUrl);
        HttpURLConnection createConnection = (HttpURLConnection) createUrl.openConnection();
        createConnection.setRequestMethod("POST");
        createConnection.setRequestProperty("Tus-Resumable", "1.0.0");
        createConnection.setRequestProperty("Upload-Length", String.valueOf(content.length));
        createConnection.setRequestProperty("Upload-Metadata", String.join(",", metadataParts));
        createConnection.setDoOutput(true);
        createConnection.getOutputStream().close();
        if (createConnection.getResponseCode() != 201) {
            throw new IllegalStateException("TUS create returned HTTP "
                    + createConnection.getResponseCode() + ", expected 201");
        }
        String location = createConnection.getHeaderField("Location");
        createConnection.disconnect();
        if (location == null || location.isEmpty()) {
            throw new IllegalStateException("TUS create did not return a Location header");
        }
        String uploadUrl = new URL(createUrl, location).toString();

        HttpURLConnection patchConnection = (HttpURLConnection) new URL(uploadUrl).openConnection();
        // HttpURLConnection rejects PATCH, so use the same POST + override header TUS supports.
        patchConnection.setRequestMethod("POST");
        patchConnection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        patchConnection.setRequestProperty("Tus-Resumable", "1.0.0");
        patchConnection.setRequestProperty("Upload-Offset", "0");
        patchConnection.setRequestProperty("Content-Type", "application/offset+octet-stream");
        patchConnection.setDoOutput(true);
        patchConnection.getOutputStream().write(content, 0, stopAfterAcceptedBytes);
        patchConnection.getOutputStream().close();
        if (patchConnection.getResponseCode() != 204) {
            throw new IllegalStateException("TUS first chunk returned HTTP "
                    + patchConnection.getResponseCode() + ", expected 204");
        }
        String acceptedBytesHeader = patchConnection.getHeaderField("Upload-Offset");
        patchConnection.disconnect();
        int acceptedBytes = acceptedBytesHeader == null ? -1 : Integer.parseInt(acceptedBytesHeader);
        if (acceptedBytes != stopAfterAcceptedBytes) {
            throw new IllegalStateException("TUS first chunk accepted " + acceptedBytes
                    + " bytes, expected " + stopAfterAcceptedBytes);
        }

        return uploadUrl;
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

    private Api2DevdockTusResumeUpload() {
        throw new IllegalStateException("Utility class");
    }
}
