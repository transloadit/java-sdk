package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unit test for {@link Transloadit} class. Api-Responses are simulated by mocking the server's response.
 */
@ExtendWith(MockServerExtension.class)  // MockServerExtension is used to start and stop the MockServer
@MockServerSettings(ports = MockHttpService.PORT) // MockServerSettings is used to define the port of the MockServer
public class TransloaditTest extends MockHttpService {
    /**
     * MockServerClient makes HTTP requests to a MockServer instance.
     */
    private final MockServerClient mockServerClient = new MockServerClient("localhost", MockHttpService.PORT);

    /**
     * Runs after each test run and resets the mockServerClient.
     */
   @AfterEach
    public void tearDown() {
        mockServerClient.reset();
    }

    /**
     * Checks if the host URL set in the Transloadit-Client matches the expectation.
     */
    @Test
    public void getHostUrl() {
        Assertions.assertEquals(transloadit.getHostUrl(), "http://localhost:" + PORT);
    }

    /**
     * Tests if {@link Transloadit#getAssembly(String)} returns the specified Assembly's response
     * by verifying the assembly_id and host URL.
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "assembly.json" is missing.
     */

    /**
     * Verifies constructor overload that accepts a SignatureProvider enables signing.
     */
    @Test
    public void constructorWithSignatureProviderEnablesSigning() {
        SignatureProvider provider = params -> "signature";
        Transloadit client = new Transloadit("KEY", provider, "http://localhost:" + PORT);

        Assertions.assertSame(provider, client.getSignatureProvider());
        Assertions.assertTrue(client.shouldSignRequest);
        Assertions.assertNull(client.secret);
    }

    /**
     * Ensures setSignatureProvider flips signing state depending on secret availability.
     */
    @Test
    public void setSignatureProviderTogglesSigningBasedOnSecret() {
        Transloadit noSecret = new Transloadit("KEY", (String) null, 5 * 60, "http://localhost:" + PORT);
        Assertions.assertFalse(noSecret.shouldSignRequest);

        SignatureProvider provider = params -> "signature";
        noSecret.setSignatureProvider(provider);
        Assertions.assertTrue(noSecret.shouldSignRequest);

        noSecret.setSignatureProvider(null);
        Assertions.assertFalse(noSecret.shouldSignRequest);

        Transloadit withSecret = new Transloadit("KEY", "SECRET", "http://localhost:" + PORT);
        withSecret.setSignatureProvider(null);
        Assertions.assertTrue(withSecret.shouldSignRequest);
    }

    @Test
    public void getAssembly() throws LocalOperationException, RequestException, IOException {
        mockServerClient.when(HttpRequest.request()
                        .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        AssemblyResponse assembly = transloadit.getAssembly("76fe5df1c93a0a530f3e583805cf98b4");

        Assertions.assertEquals(assembly.getId(), "76fe5df1c93a0a530f3e583805cf98b4");
        Assertions.assertEquals(assembly.getUrl(), "http://localhost:9040/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
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

        Assertions.assertEquals(assembly.getId(), "76fe5df1c93a0a530f3e583805cf98b4");
        Assertions.assertEquals(assembly.getUrl(), "http://localhost:9040/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
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

        Assertions.assertEquals(assembly.json().getString("ok"), "ASSEMBLY_CANCELED");
    }

    /**
     * Proves the functionality of {@link Transloadit#listAssemblies()} by checking the size of the returned list.
     *
     * @throws LocalOperationException if building the request goes wrong.
     * @throws RequestException        if communication with the server goes wrong.
     * @throws IOException             if Test resource "assemblies.json" is missing.
     */
    /**
     * Checks listAssemblies parses the returned JSON into count and items correctly.
     */
    @Test
    public void listAssembliesParsesItems() throws RequestException, LocalOperationException, IOException {
        mockServerClient.when(HttpRequest.request()
                        .withPath("/assemblies").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assemblies_with_items.json")));

        ListResponse assemblies = transloadit.listAssemblies();
        Assertions.assertEquals(2, assemblies.size());
        Assertions.assertEquals(2, assemblies.getItems().length());
        Assertions.assertEquals("abcd1234", assemblies.getItems().getJSONObject(0).getString("assembly_id"));
        Assertions.assertEquals("efgh5678", assemblies.getItems().getJSONObject(1).getString("assembly_id"));
    }

    @Test
    public void listAssemblies() throws RequestException, LocalOperationException, IOException {

        mockServerClient.when(HttpRequest.request()
                        .withPath("/assemblies").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assemblies.json")));

        ListResponse assemblies = transloadit.listAssemblies();
        Assertions.assertEquals(assemblies.size(), 0);
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

        Assertions.assertEquals(template.json().get("template_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        Assertions.assertEquals(template.json().get("ok"), "TEMPLATE_FOUND");
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

        Assertions.assertEquals(template.json().get("ok"), "TEMPLATE_UPDATED");
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
        Assertions.assertEquals(deletedTemplate.json().get("ok"), "TEMPLATE_DELETED");
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
        Assertions.assertEquals(templates.size(), 0);
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
        Assertions.assertEquals(bill.json().get("invoice_id"), "76fe5df1c93a0a530f3e583805cf98b4");
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

        Assertions.assertEquals(1, transloadit.getQualifiedErrorsForRetry().size());
        ArrayList<String> exceptionsSet = transloadit.getQualifiedErrorsForRetry();
        exceptionsSet.add("Socket.blah.Exception");

        transloadit.setQualifiedErrorsForRetry(exceptionsSet);

        exceptionsSet = transloadit.getQualifiedErrorsForRetry();

        for (int i = 0; i < exceptionsSet.size(); i++) {
            Assertions.assertEquals(exceptionTemplate.get(i), exceptionsSet.get(i));
        }
    }

    /**
     * Test if timeout setting works properly.
     * @throws LocalOperationException if setting the timeout goes wrong.
     */
    @Test
    public void getAndSetTimeoutRetry() throws LocalOperationException {
        int timeout = 5;
        Assertions.assertEquals(0, transloadit.getRetryDelay());
        transloadit.setRetryDelay(timeout);
        Assertions.assertEquals(timeout, transloadit.getRetryDelay());
        Exception exception = new Exception();
        try {
            transloadit.setRetryDelay(-timeout);
        } catch (LocalOperationException e) {
            exception = e;
        }
        Assertions.assertInstanceOf(LocalOperationException.class, exception);

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
        Assertions.assertTrue(matcher.find());
    }

    /**
     * Test if the SDK can generate a correct signed Smart CDN URL.
     */
    @Test
    @SuppressWarnings("checkstyle:linelength")
    public void getSignedSmartCDNURL() throws LocalOperationException {
        Transloadit client = new Transloadit("foo_key", "foo_secret");
        Map<String, List<String>> params = new HashMap<>();
        params.put("foo", Collections.singletonList("bar"));
        params.put("aaa", Arrays.asList("42", "21")); // Must be sorted before `foo`

        String url = client.getSignedSmartCDNUrl(
                "foo_workspace",
                "foo_template",
                "foo/input",
                params,
                Instant.parse("2024-05-01T01:00:00.000Z").toEpochMilli()
        );
        Assertions.assertEquals("https://foo_workspace.tlcdn.com/foo_template/foo%2Finput?aaa=42&aaa=21&auth_key=foo_key&exp=1714525200000&foo=bar&sig=sha256%3A9a8df3bb28eea621b46ec808a250b7903b2546be7e66c048956d4f30b8da7519", url);
    }
}

