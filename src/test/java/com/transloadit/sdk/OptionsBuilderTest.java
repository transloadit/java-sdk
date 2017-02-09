package com.transloadit.sdk;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class OptionsBuilderTest {
    public OptionsBuilder optionsBuilder;

    @Before
    public void setUp() throws Exception {
        optionsBuilder = new OptionsBuilder();
        optionsBuilder.steps = new Steps();
        optionsBuilder.options = new HashMap<>();
    }


    @Test
    public void testAddStep() throws Exception {
        optionsBuilder.addStep("encode", "/video/encode", new HashMap<>());

        assertEquals(optionsBuilder.steps.getStep("encode").robot , "/video/encode");
    }

    @Test
    public void testAddOptions() throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put("foo", "bar");
        options.put("red", "color");

        optionsBuilder.addOptions(options);
        assertEquals(options, optionsBuilder.options);
    }

    @Test
    public void testAddOption() throws Exception {
        optionsBuilder.addOption("foo", "bar");
        assertEquals(optionsBuilder.options.get("foo"), "bar");
    }

}