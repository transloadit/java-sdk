package com.transloadit.sdk;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for objects that use steps and send options to Transloadit.
 */
public class OptionsBuilder {
    /**
     * The {@link Transloadit} client.
     */
    protected Transloadit transloadit;
    /**
     * Map of {@link Steps} to be performed during {@link Assembly} execution.
     */
    protected Steps steps;
    /**
     * Map of extra options to be sent along with the {@link Request}.
     */
    protected Map<String, Object> options;

    /**
     * Adds a step to the steps.
     *
     * @param name {@link String} name of the step
     * @param robot {@link String} name of the robot used by the step.
     * @param options {@link Map} extra options required for the step.
     */
    public void addStep(String name, String robot, Map<String, Object> options) {
        steps.addStep(name, robot, options);
    }

    /**
     * Removes step with the given name from the set of steps.
     *
     * @param name Name of the step to remove.
     */
    public void removeStep(String name) {
        steps.removeStep(name);
    }

    /**
     * Adds extra options(e.g "template_id") to be sent along with the request.
     *
     * @param options {@link Map} set of options to add
     */
    public void addOptions(Map<String, Object> options) {
        this.options.putAll(options);
    }

    /**
     * Adds a single option to be sent along with the request.
     *
     * @param key {@link String} name of the option
     * @param value {@link Object} value of the option.
     */
    public void addOption(String key, Object value) {
        this.options.put(key, value);
    }

    /**
     * Returns the Transloadit client instance attached to the options builder.
     *
     * @return Transloadit client instance attached to the options builder
     */
    public Transloadit getClient() {
        return transloadit;
    }

    /**
     * Adds a Key - Value Pair to the (form) fields section of an Assembly or Template.
     * Already existing Keys will be overwritten.
     * Also overwrites existing values stored under the key "fields" in options. This happens if the value is not a
     * instance of {@link JSONObject}
     * @param key {@link String}
     * @param value {@link Object}
     */
    public void addField(String key, Object value) {
        HashMap<String, Object> fields = new HashMap<String, Object>();
        fields.put(key, value);
        addFields(fields);
    }

    /**
     * Adds multiple Key-Value pairs to the (form) fields section of an Assembly or Template.
     * Already existing Keys will be overwritten.
     * Also overwrites existing values stored under the key "fields" in options. This happens if the value is not a
     * instance of {@link JSONObject}
     * @param fields
     */
    public void addFields(Map<String, Object> fields) {

        // Construct JSONObject with all field key-value pairs
        JSONObject fieldValues = new JSONObject();
        for (String key:fields.keySet()) {
            fieldValues.put(key, fields.get(key));
        }

        // Add JSONObject to key "fields"
        if (options.containsKey("fields") && (options.get("fields") instanceof JSONObject)) {
            JSONObject existingField = (JSONObject) options.get("fields");
            for (String key: fieldValues.keySet()) {
                existingField.put(key, fieldValues.get(key));
            }
            options.put("fields", existingField);

        } else {
            options.put("fields", fieldValues);  // This overwrites other "fields" entries
        }

    }
}
