package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.response.Response;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
//CHECKSTYLE:OFF
import java.util.Map;  // Suppress warning as the Map import is needed for the JavaDoc Comments
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//CHECKSTYLE:ON

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for {@link Transloadit} class. Api-Responses are simulated by mocking the server's response.
 */
public class TransloaditTest extends MockHttpService {

    /**
     * MockServer can be run using the MockServerRule.
     */
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, true, PORT);


    /**
     * MockServerClient makes HTTP requests to a MockServer instance.
     */
    private MockServerClient mockServerClient;

    /**
     * Runs after each test run and resets the mockServerClient.
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
     * by verifying the assembly_id and host URL.
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "assembly.json" is missing.
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
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "assembly.json" is missing.
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
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "cancel_assembly.json" is missing.
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
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "assemblies.json" is missing.
     */
    @Test
    public void listAssemblies() throws RequestException, LocalOperationException, IOException {

        mockServerClient.when(HttpRequest.request()
                        .withPath("/assemblies").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assemblies.json")));

        ListResponse assemblies = transloadit.listAssemblies();
        assertEquals(assemblies.size(), 0);
    }

    /**
     * Proves functionality of {@link Transloadit#getTemplate(String)} by requesting a specific template and verifying
     * the mock-api-response's Assembly-ID and status ("TEMPLATE_FOUND").
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "template.json" is missing.
     */
    @Test
    public void getTemplate() throws RequestException, LocalOperationException, IOException {
        mockServerClient.when(HttpRequest.request()
                        .withPath("/templates/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("template.json")));

        Response template = transloadit.getTemplate("76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(template.json().get("template_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(template.json().get("ok"), "TEMPLATE_FOUND");
    }

    /**
     * Proves that {@link Transloadit#updateTemplate(String, Map)} sends a PUT - request and verifies that the
     * {@link Response} receives the mockserver's answer "TEMPLATE_UPDATED".
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "update_template.json" is missing.
     */
    @Test
    public void updateTemplate()
            throws RequestException, LocalOperationException, IOException {
        mockServerClient.when(HttpRequest.request()
                        .withPath("/templates/55c965a063a311e6ba2d379ef10b28f7").withMethod("PUT"))
                .respond(HttpResponse.response().withBody(getJson("update_template.json")));

        Response template = transloadit.updateTemplate("55c965a063a311e6ba2d379ef10b28f7",
                new HashMap<String, Object>());

        assertEquals(template.json().get("ok"), "TEMPLATE_UPDATED");
    }

    /**
     * Proves that {@link Transloadit#deleteTemplate(String)} sends a DELETE - request and verifies that the
     * {@link Response} receives the mockserver's answer "TEMPLATE_DELETED".
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "delete_template.json" is missing.
     */
    @Test
    public void deleteTemplate()
            throws LocalOperationException, RequestException, IOException {
        mockServerClient.when(HttpRequest.request()
                        .withPath("/templates/11148db0ec4f11e6a05ca3d04d2a53e6").withMethod("DELETE"))
                .respond(HttpResponse.response().withBody(getJson("delete_template.json")));

        Response deletedTemplate = transloadit.deleteTemplate("11148db0ec4f11e6a05ca3d04d2a53e6");
        assertEquals(deletedTemplate.json().get("ok"), "TEMPLATE_DELETED");
    }

    /**
     * Proves the functionality of {@link Transloadit#listTemplates()} by checking the size of the returned list.
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "templates.json" is missing.
     */
    @Test
    public void listTemplates() throws RequestException, LocalOperationException, IOException {
        mockServerClient.when(HttpRequest.request()
                        .withPath("/templates").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("templates.json")));

        ListResponse templates = transloadit.listTemplates();
        assertEquals(templates.size(), 0);
    }

    /**
     * Verifies that {@link Transloadit#getBill(int, int)} sends a GET request and checks the ID of the returned
     * invoice.
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "templates.json" is missing.
     */
    @Test
    public void getBill() throws LocalOperationException, RequestException, IOException {
        mockServerClient.when(HttpRequest.request()
                        .withPath("/bill/2016-09").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("bill.json")));

        Response bill = transloadit.getBill(9, 2016);
        assertEquals(bill.json().get("invoice_id"), "76fe5df1c93a0a530f3e583805cf98b4");
    }

    /**
     * Tests if the methods {@link Transloadit#getQualifiedErrorsForRetry()} and
     * {@link Transloadit#setQualifiedErrorsForRetry(ArrayList)} are working as supposed.
     */
    @Test
    public void getAndSetqualifiedErrorsForRetry() {
        ArrayList<String> exceptionTemplate = new ArrayList<String>();
        exceptionTemplate.add("java.net.SocketTimeoutException");
        exceptionTemplate.add("Socket.blah.Exception");

        assertTrue(transloadit.getQualifiedErrorsForRetry().size() == 1);
        ArrayList<String> exceptionsSet = transloadit.getQualifiedErrorsForRetry();
        exceptionsSet.add("Socket.blah.Exception");

        transloadit.setQualifiedErrorsForRetry(exceptionsSet);

        exceptionsSet = transloadit.getQualifiedErrorsForRetry();

        for (int i = 0; i < exceptionsSet.size(); i++) {
            assertEquals(exceptionTemplate.get(i), exceptionsSet.get(i));
        }
    }

    /**
     * Test if timeout setting works properly.
     * @throws LocalOperationException
     */
    @Test
    public void getAndSetTimeoutRetry() throws LocalOperationException {
        assertEquals(0, transloadit.getRetryDelay());
        transloadit.setRetryDelay(5);
        assertEquals(5, transloadit.getRetryDelay());
        Exception exception = new Exception();
        try {
            transloadit.setRetryDelay(-5);
        } catch (LocalOperationException e) {
            exception = e;
        }
      assertTrue(exception instanceof LocalOperationException);

    }

    /**
     * Test if AdditionalTransloaditHeaders are working properly. This concerns
     * {@link Transloadit#getAdditionalTransloaditClientHeaderContent()} and
     * {@link Transloadit#setAdditionalTransloaditClientHeaderContent(String, String)}
     */
    @Test
    public void getAndSetdditionalTransloaditClientHeaderContent() throws LocalOperationException {
        Transloadit testTransloadit = new Transloadit("key", "secret");
        testTransloadit.setAdditionalTransloaditClientHeaderContent(" Test-SDK ", "0.0.1");
        ArrayList headerList = testTransloadit.getAdditionalTransloaditClientHeaderContent();
        String header = (String) headerList.get(0);
        assertEquals("Test-SDK:0.0.1", header);

        // Test if exceptions are thrown
        Exception ex1 = new Exception();
        try {
            testTransloadit.setAdditionalTransloaditClientHeaderContent("Android-SDK", "3.3.3.4");
        } catch (Exception e) {
            ex1 = e;
        }

        Exception ex2 = new Exception();
        try {
            testTransloadit.setAdditionalTransloaditClientHeaderContent("Android,SDK", "3.3.3");
        } catch (Exception e) {
            ex2 = e;
        }
        assertTrue(ex1 instanceof LocalOperationException);
        assertTrue(ex2 instanceof LocalOperationException);
    }

    /**
     * Tests if the version Info is obtained correctly with {@link Transloadit#loadVersionInfo()}.
     */
    @Test
    public void loadVersionInfo() {
        String info = transloadit.loadVersionInfo();
        Pattern versionPattern = Pattern.compile(
                "^[a-z-]*[:]([0-9]+)\\.([0-9]+)\\.([0-9]+)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = versionPattern.matcher(info);
        assertTrue(matcher.matches());
    }

    /**
     * This test checks if the Tranlsoadit-CLient Header string is built with
     * {@link Transloadit#getTransloaditClientHeader()}.
     */
    @Test
    public void getTransloaditClientHeader() throws LocalOperationException {
        Transloadit testTransloadit = new Transloadit("Key", "Secret");
        String info = testTransloadit.loadVersionInfo();
        testTransloadit.setAdditionalTransloaditClientHeaderContent("android-sdk", "2.2.2");
        testTransloadit.setAdditionalTransloaditClientHeaderContent("Uppy", "11.0.123");

        String comparisonString = info + ", android-sdk:2.2.2, Uppy:11.0.123";
        assertEquals(comparisonString, testTransloadit.getTransloaditClientHeader());
    }

}

