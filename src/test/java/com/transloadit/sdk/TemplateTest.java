package com.transloadit.sdk;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.transloadit.sdk.response.Response;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

/**
 * test for template class.
 */
public class TemplateTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9040);

    @Test
    public void testSave() throws Exception {
        stubFor(post(urlPathEqualTo("/templates"))
                .willReturn(aResponse().withBodyFile("template.json")));

        // stub returns no response when this request is run too quickly.
        Thread.sleep(2000);

        Template template = new Template(new Transloadit("KEY", "SECRET", 3600, "http://localhost:9040"), "template_name");
        Response newTemplate = template.save();

        assertEquals(newTemplate.json().get("ok"), "TEMPLATE_FOUND");
    }
}