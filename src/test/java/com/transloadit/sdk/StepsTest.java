package com.transloadit.sdk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;


/**
 * Unit test for {@link Steps} class.
 */
public class StepsTest {
    /**
     * Variable that holds an {@link Steps} instance to perform the tests on.
     */
    private Steps steps;

    /**
     * Assings a new {@link Steps} instance to Steps variable before each individual test.
     */
    @BeforeEach
    public void setUp() {
        steps = new Steps();
    }

    /**
     * Checks the functionality of the {@link Steps#addStep(String, String, Map)} and {@link Steps#getStep(String)}
     * methods by first creating a Step  with defined parameters and then querying it using these parameters.
     */
    @Test
    public void addStepGetStep() {
        steps.addStep("encode", "/video/encode", new HashMap<String, Object>());
        Assertions.assertEquals(steps.getStep("encode").robot, "/video/encode");

        HashMap<String, Object> stepOptions = new HashMap<String, Object>();
        stepOptions.put("width", 1920);
        stepOptions.put("height", 720);

        steps.addStep("video", stepOptions);
        Assertions.assertEquals(steps.getStep("video").options, stepOptions);
    }

    /**
     * Checks the functionality of the {@link Steps#removeStep(String)} method by first adding a specific Step to the
     * Steps instance, verifying its existence and deleting it afterwards. Test passes if existence of Step can't
     * be verified after deleting it.
     */
    @Test
    public void removeStep() {
        steps.addStep("encode", "/video/encode", new HashMap<String, Object>());

        Assertions.assertTrue(steps.all.containsKey("encode"));
        steps.removeStep("encode");
        Assertions.assertFalse(steps.all.containsKey("encode"));
    }

    /**
     * Compares the result of {@link Steps#toMap()} against a predefined HashMap. Test passe if they are identical.
     */
    @Test
    public void toMap() {
        steps.addStep("encode", "/video/encode", new HashMap<String, Object>());
        steps.addStep("thumbs", new HashMap<String, Object>());

        Map<String, Map> controlMap = new HashMap<String, Map>();

        Map<String, String> encodeStep = new HashMap<String, String>();
        encodeStep.put("robot", "/video/encode");

        Map<String, String> thumbStep = new HashMap<String, String>();

        controlMap.put("encode", encodeStep);
        controlMap.put("thumbs", thumbStep);

        Assertions.assertEquals(controlMap, steps.toMap());
    }

}
