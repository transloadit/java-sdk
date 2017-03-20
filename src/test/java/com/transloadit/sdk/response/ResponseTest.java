package com.transloadit.sdk.response;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.MockHttpService;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.junit.Assert.assertEquals;

public class ResponseTest extends MockHttpService {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

    private MockServerClient mockServerClient;

    @Test
    public void json() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        AssemblyResponse response = new Assembly(transloadit).save(false);
        assertEquals(response.json().getString("ok"), "ASSEMBLY_COMPLETED");
    }

}