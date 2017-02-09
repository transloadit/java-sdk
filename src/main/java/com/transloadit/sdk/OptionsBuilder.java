package com.transloadit.sdk;

import java.util.Map;

/**
 * Base class for objects that use steps and send options to Transloadit.
 */
public class OptionsBuilder {
    Transloadit transloadit;
    Steps steps;
    Map<String, Object> options;

    /**
     * Adds a step to the steps.
     *
     * @param name {@link String} name of the step
     * @param robot {@link String} name of the robot used by the step.
     * @param options {@link Map} extra options required for the step.
     */
    public void addStep(String name, String robot, Map<String, Object> options){
        steps.addStep(name, robot, options);
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
}
