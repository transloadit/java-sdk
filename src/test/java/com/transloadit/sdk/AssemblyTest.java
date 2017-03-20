package com.transloadit.sdk;

import com.transloadit.sdk.response.AssemblyResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.ParameterBody.params;

public class AssemblyTest extends MockHttpService {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

    private MockServerClient mockServerClient;

    public Assembly assembly;

    @Before
    public void setUp() throws Exception {
        assembly = new Assembly(transloadit);
    }

    @Test
    public void addFile() throws Exception {
        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        assertEquals(file, assembly.files.get("file_name"));
    }

    @Test
    public void save() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        AssemblyResponse savedAssembly = assembly.save(false);
        assertEquals(savedAssembly.json().get("ok"), "ASSEMBLY_COMPLETED");

        mockServerClient.reset();

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(params(param("tus_num_expected_upload_files", "0"))))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        AssemblyResponse resumableAssembly = assembly.save(true);
        assertEquals(resumableAssembly.json().get("id"), "02ce6150ea2811e6a35a8d1e061a5b71");

        mockServerClient.reset();
    }

}