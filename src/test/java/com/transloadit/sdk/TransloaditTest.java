package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.TransloaditRequestException;
import com.transloadit.sdk.exceptions.TransloaditSignatureException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.response.Response;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * unit test for Transloadit class
 */
public class TransloaditTest extends MockHttpService {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

    private MockServerClient mockServerClient;

    @Test
    public void testGetAssembly() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        AssemblyResponse assembly = transloadit.getAssembly("76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(assembly.id, "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(assembly.sslUrl, "https://api2.jane.transloadit.com/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
    }

    @Test
    public void testGetAssemblyByUrl() throws TransloaditSignatureException, TransloaditRequestException, IOException {

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json"))
        );

        AssemblyResponse assembly = transloadit
                .getAssemblyByUrl(transloadit.hostUrl + "/assemblies/76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(assembly.id, "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(assembly.sslUrl, "https://api2.jane.transloadit.com/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
    }

    @Test
    public void testListAssemblies() throws TransloaditRequestException, TransloaditSignatureException, IOException {
        mockServerClient.reset();

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assemblies.json")));

        ListResponse assemblies = transloadit.listAssemblies();
        assertEquals(assemblies.size, 0);
    }

    @Test
    public void testGetTemplate() throws TransloaditRequestException, TransloaditSignatureException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/templates/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("template.json")));

        Response template = transloadit.getTemplate("76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(template.json().get("template_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(template.json().get("ok"), "TEMPLATE_FOUND");
    }

    @Test
    public void testUpdateTemplate()
            throws TransloaditRequestException, TransloaditSignatureException, InterruptedException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/templates/55c965a063a311e6ba2d379ef10b28f7").withMethod("PUT"))
                .respond(HttpResponse.response().withBody(getJson("update_template.json")));

        Response template = transloadit.updateTemplate("55c965a063a311e6ba2d379ef10b28f7",
                new HashMap<String, Object>());

        assertEquals(template.json().get("ok"), "TEMPLATE_UPDATED");
    }

    @Test
    public void testDeleteTemplate()
            throws TransloaditSignatureException, TransloaditRequestException, InterruptedException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/templates/11148db0ec4f11e6a05ca3d04d2a53e6").withMethod("DELETE"))
                .respond(HttpResponse.response().withBody(getJson("delete_template.json")));

        Response deletedTemplate = transloadit.deleteTemplate("11148db0ec4f11e6a05ca3d04d2a53e6");
        assertEquals(deletedTemplate.json().get("ok"), "TEMPLATE_DELETED");
    }

    @Test
    public void testListTemplates() throws TransloaditRequestException, TransloaditSignatureException, IOException {
        mockServerClient.reset();

        mockServerClient.when(HttpRequest.request()
                .withPath("/templates").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("templates.json")));

        ListResponse templates = transloadit.listTemplates();
        assertEquals(templates.size, 0);
    }

    @Test
    public void testGetBill() throws TransloaditSignatureException, TransloaditRequestException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/bill/2016-09").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("bill.json")));

        Response bill = transloadit.getBill(9, 2016);
        assertEquals(bill.json().get("invoice_id"), "76fe5df1c93a0a530f3e583805cf98b4");
    }

}