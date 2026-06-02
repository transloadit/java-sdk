package com.transloadit.examples;

import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Runs API2's contract-owned Template lifecycle scenario against devdock.
 */
public final class Api2DevdockTemplateLifecycle {
    /**
     * Runs the Template lifecycle scenario.
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

        JSONObject templateConfig = scenario.getJSONObject("template");
        JSONObject updateConfig = scenario.getJSONObject("update");
        String templateName = templateConfig.getString("namePrefix") + "-" + System.currentTimeMillis();

        Response created = transloadit.createTemplate(templatePayload(templateName, templateConfig));
        String templateId = created.json().getString("id");
        boolean deleteTemplate = true;

        try {
            Response fetched = transloadit.getTemplate(templateId);

            Map<String, Object> listOptions = new HashMap<String, Object>();
            listOptions.put("pagesize", scenario.getJSONObject("list").getInt("pageSize"));
            ListResponse listed = transloadit.listTemplates(listOptions);

            String updatedTemplateName = templateName + updateConfig.getString("nameSuffix");
            transloadit.updateTemplate(templateId, templatePayload(updatedTemplateName, updateConfig));
            Response updated = transloadit.getTemplate(templateId);

            transloadit.deleteTemplate(templateId);
            deleteTemplate = false;

            JSONObject result = new JSONObject();
            JSONObject deletedGet = deletedGetResult(transloadit, templateId);
            for (Iterator<String> keys = deletedGet.keys(); keys.hasNext();) {
                String key = keys.next();
                result.put(key, deletedGet.get(key));
            }
            result.put("fetched", templateResult(fetched.json()));
            result.put("listCount", listed.size());
            result.put("templateId", templateId);
            result.put("templateName", templateName);
            result.put("updated", templateResult(updated.json()));
            result.put("updatedTemplateName", updatedTemplateName);
            writeResult(result);
        } finally {
            if (deleteTemplate) {
                transloadit.deleteTemplate(templateId);
            }
        }

        System.out.println("Java SDK devdock scenario " + scenario.getString("scenarioId")
                + " passed for " + templateId);
    }

    private static JSONObject deletedGetResult(Transloadit transloadit, String templateId) throws Exception {
        Response response = transloadit.getTemplate(templateId);
        JSONObject body = response.json();
        boolean succeeded = response.status() >= 200 && response.status() < 300 && !body.has("error");

        JSONObject result = new JSONObject();
        result.put("deletedGetSucceeded", succeeded);
        result.put("deletedErrorCode", body.optString("error", body.optString("ok", "")));
        return result;
    }

    private static JSONObject loadScenario() throws Exception {
        String scenarioPath = System.getenv("API2_SDK_EXAMPLE_SCENARIO");
        if (scenarioPath == null || scenarioPath.isEmpty()) {
            scenarioPath = "examples/api2-devdock-template-lifecycle/api2-scenario.json";
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

    private static Map<String, Object> templatePayload(String name, JSONObject config) {
        JSONObject content = config.getJSONObject("content");
        Map<String, Object> template = jsonObjectToMap(content.getJSONObject("additionalProperties"));
        template.put("steps", jsonObjectToMap(content.getJSONObject("steps")));

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("name", name);
        payload.put("require_signature_auth", config.getBoolean("requireSignatureAuth") ? 1 : 0);
        payload.put("template", template);
        return payload;
    }

    private static JSONObject templateResult(JSONObject template) {
        JSONObject result = new JSONObject();
        result.put("content", template.getJSONObject("content"));
        result.put("id", template.getString("id"));
        result.put("name", template.getString("name"));
        result.put("requireSignatureAuth", template.getInt("require_signature_auth") != 0);
        return result;
    }

    private static Map<String, Object> jsonObjectToMap(JSONObject object) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Iterator<String> keys = object.keys(); keys.hasNext();) {
            String key = keys.next();
            map.put(key, jsonValueToJava(object.get(key)));
        }

        return map;
    }

    private static Object jsonValueToJava(Object value) {
        if (value == JSONObject.NULL) {
            return null;
        }

        if (value instanceof JSONObject) {
            return jsonObjectToMap((JSONObject) value);
        }

        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            List<Object> list = new ArrayList<Object>();
            for (int index = 0; index < array.length(); index += 1) {
                list.add(jsonValueToJava(array.get(index)));
            }

            return list;
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

    private Api2DevdockTemplateLifecycle() {
        throw new IllegalStateException("Utility class");
    }
}
