package com.transloadit.sdk;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.transloadit.sdk.response.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class AssemblyTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9040);

    public Assembly assembly;

    @Before
    public void setUp() throws Exception {
        assembly = new Assembly(new Transloadit("KEY", "SECRET", 3600, "http://localhost:9040"));
    }

    @Test
    public void addFile() throws Exception {
        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        assertEquals(file, assembly.files.get("file_name"));
    }

    @Test
    public void save() throws Exception {
        stubFor(post(urlPathEqualTo("/assemblies"))
                .willReturn(aResponse().withBodyFile("assembly.json")));

        Response savedAssembly = assembly.save();
        assertEquals(savedAssembly.json().get("ok"), "ASSEMBLY_COMPLETED");
    }

}