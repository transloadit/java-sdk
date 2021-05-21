package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.response.Response;
import org.junit.After;
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
 * unit test for Transloadit class.
 */
public class TransloaditTest extends MockHttpService {

    /**
     * MockServer can be run using the MockServerRule.
     */
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

    /**
     * MockServerClient makes HTTP requests to a remote MockServer instance.
     */
    private MockServerClient mockServerClient;

    /**
     * Runs after finishing the Tests and resets the mockServerClient.
     */
    @After
    public void tearDown() {
        mockServerClient.reset();
    }

    /**
     * Checks if the host URL set in the Transloadit-Client matches the expectation.
     */
    @Test
    public void getHostUrl() {
        assertEquals(transloadit.getHostUrl(), "http://localhost:" + PORT);
    }

    /**
     * Tests if {@link Transloadit#getAssembly(String)} returns the specified Assembly's response
     * by verifying the assembly_id and host URL
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException if communication with the server goes wrong.
     * @throws IOException if Test resource "assembly.json" is missing.
     */
    @Test
    public void getAssembly() throws LocalOperationException, RequestException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        AssemblyResponse assembly = transloadit.getAssembly("76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(assembly.getId(), "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(assembly.getUrl(), "http://localhost:9040/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
    }

    /**
     * Tests if {@link Transloadit#getAssemblyByUrl(String)} returns the correct {@link AssemblyResponse} for specified
     * {@link Assembly}. The Test validates the assembly_id and host URL.
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException if communication with the server goes wrong.
     * @throws IOException if Test resource "assembly.json" is missing.
     */
    @Test
    public void getAssemblyByUrl() throws LocalOperationException, RequestException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json"))
        );

        AssemblyResponse assembly = transloadit
                .getAssemblyByUrl(transloadit.getHostUrl() + "/assemblies/76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(assembly.getId(), "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(assembly.getUrl(), "http://localhost:9040/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
    }

    /**
     * Tests if sending an cancel request for an running Assembly with {@link Transloadit#cancelAssembly(String)} works.
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException if communication with the server goes wrong.
     * @throws IOException if Test resource "cancel_assembly.json" is missing.
     */
    @Test
    public void cancelAssembly() throws LocalOperationException, RequestException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("DELETE"))
                .respond(HttpResponse.response().withBody(getJson("cancel_assembly.json")));

        AssemblyResponse assembly = transloadit
                .cancelAssembly(transloadit.getHostUrl() + "/assemblies/76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(assembly.json().getString("ok"), "ASSEMBLY_CANCELED");
    }

    /**
     * Proves the functionality of {@link Transloadit#listAssemblies()} by checking the size of the returned list.
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException if communication with the server goes wrong.
     * @throws IOException if Test resource "assemblies.json" is missing.
     */
    @Test
    public void listAssemblies() throws RequestException, LocalOperationException, IOException {

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assemblies.json")));

        ListResponse assemblies = transloadit.listAssemblies();
        assertEquals(assemblies.size(), 0);
    }

    @Test
    public void getTemplate() throws RequestException, LocalOperationException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/templates/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("template.json")));

        Response template = transloadit.getTemplate("76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(template.json().get("template_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(template.json().get("ok"), "TEMPLATE_FOUND");
    }

    @Test
    public void updateTemplate()
            throws RequestException, LocalOperationException, InterruptedException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/templates/55c965a063a311e6ba2d379ef10b28f7").withMethod("PUT"))
                .respond(HttpResponse.response().withBody(getJson("update_template.json")));

        Response template = transloadit.updateTemplate("55c965a063a311e6ba2d379ef10b28f7",
                new HashMap<String, Object>());

        assertEquals(template.json().get("ok"), "TEMPLATE_UPDATED");
    }

    @Test
    public void deleteTemplate()
            throws LocalOperationException, RequestException, InterruptedException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/templates/11148db0ec4f11e6a05ca3d04d2a53e6").withMethod("DELETE"))
                .respond(HttpResponse.response().withBody(getJson("delete_template.json")));

        Response deletedTemplate = transloadit.deleteTemplate("11148db0ec4f11e6a05ca3d04d2a53e6");
        assertEquals(deletedTemplate.json().get("ok"), "TEMPLATE_DELETED");
    }

    @Test
    public void listTemplates() throws RequestException, LocalOperationException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/templates").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("templates.json")));

        ListResponse templates = transloadit.listTemplates();
        assertEquals(templates.size(), 0);
    }

    @Test
    public void getBill() throws LocalOperationException, RequestException, IOException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/bill/2016-09").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("bill.json")));

        Response bill = transloadit.getBill(9, 2016);
        assertEquals(bill.json().get("invoice_id"), "76fe5df1c93a0a530f3e583805cf98b4");
    }
}
