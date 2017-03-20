package com.transloadit.sdk;

import com.transloadit.sdk.response.Response;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.junit.Assert.assertEquals;

/**
 * test for template class.
 */
public class TemplateTest extends MockHttpService {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

    private MockServerClient mockServerClient;

    @Test
    public void getName() throws Exception {
        Template template = transloadit.newTemplate("foo");
        assertEquals(template.getName(), "foo");
    }

    @Test
    public void setName() throws Exception {
        Template template = transloadit.newTemplate("foo");
        assertEquals(template.getName(), "foo");

        template.setName("bar");
        assertEquals(template.getName(), "bar");
    }

    @Test
    public void save() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/templates").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("template.json")));

        Template template = new Template(transloadit, "template_name");
        Response newTemplate = template.save();

        assertEquals(newTemplate.json().get("ok"), "TEMPLATE_FOUND");

        mockServerClient.reset();
    }
}