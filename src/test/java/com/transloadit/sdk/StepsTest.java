package com.transloadit.sdk;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * test Steps
 */
public class StepsTest {
    public Steps steps;

    @Before
    public void setUp() throws Exception {
        steps = new Steps();
    }

    @Test
    public void addStep() {
        steps.addStep("encode", "/video/encode", new HashMap<String, Object>());
        assertEquals(steps.getStep("encode").robot , "/video/encode");
    }

    @Test
    public void removeStep() throws Exception {
        steps.addStep("encode", "/video/encode", new HashMap<String, Object>());

        assertTrue(steps.all.containsKey("encode"));
        steps.removeStep("encode");
        assertFalse(steps.all.containsKey("encode"));
    }

    @Test
    public void getStep() throws Exception {
        steps.addStep("encode", "/video/encode", new HashMap<String, Object>());
        assertEquals(steps.getStep("encode").robot , "/video/encode");
    }

    @Test
    public void toMap() throws Exception {
        steps.addStep("encode", "/video/encode", new HashMap<String, Object>());
        steps.addStep("thumbs", "/video/thumbs", new HashMap<String, Object>());

        Map<String, Map> controlMap = new HashMap<String, Map>();

        Map<String, String> encodeStep = new HashMap<String, String>();
        encodeStep.put("robot", "/video/encode");

        Map<String, String> thumbStep = new HashMap<String, String>();
        thumbStep.put("robot", "/video/thumbs");

        controlMap.put("encode", encodeStep);
        controlMap.put("thumbs", thumbStep);

        assertEquals(controlMap, steps.toMap());
    }

}