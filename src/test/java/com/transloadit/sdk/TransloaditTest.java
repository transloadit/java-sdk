package com.transloadit.sdk;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.transloadit.sdk.exceptions.TransloaditRequestException;
import com.transloadit.sdk.exceptions.TransloaditSignatureException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.response.Response;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

/**
 * unit test for Transloadit class
 */
public class TransloaditTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9040);

    public Transloadit transloadit = new Transloadit("KEY", "SECRET", "http://localhost:9040");


    @Test
    public void testGetAssembly() throws TransloaditSignatureException, TransloaditRequestException {
        stubFor(get(urlPathEqualTo("/assemblies/76fe5df1c93a0a530f3e583805cf98b4"))
                .willReturn(aResponse().withBodyFile("assembly.json")));

        AssemblyResponse assembly = transloadit.getAssembly("76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(assembly.id, "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(assembly.sslUrl, "https://api2.jane.transloadit.com/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
    }

    @Test
    public void testGetAssemblyByUrl() throws TransloaditSignatureException, TransloaditRequestException {
        stubFor(get(urlPathEqualTo("/assemblies/76fe5df1c93a0a530f3e583805cf98b4"))
                .willReturn(aResponse().withBodyFile("assembly.json")));

        AssemblyResponse assembly = transloadit
                .getAssemblyByUrl("http://localhost:9040/assemblies/76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(assembly.id, "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(assembly.sslUrl, "https://api2.jane.transloadit.com/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
    }

    @Test
    public void testListAssemblies() throws TransloaditRequestException, TransloaditSignatureException {
        stubFor(get(urlPathEqualTo("/assemblies"))
                .willReturn(aResponse().withBodyFile("assemblies.json")));

        ListResponse assemblies = transloadit.listAssemblies();
        assertEquals(assemblies.size, 0);
    }

    @Test
    public void testGetTemplate() throws TransloaditRequestException, TransloaditSignatureException{
        stubFor(get(urlPathEqualTo("/templates/76fe5df1c93a0a530f3e583805cf98b4"))
                .willReturn(aResponse().withBodyFile("template.json")));

        Response template = transloadit.getTemplate("76fe5df1c93a0a530f3e583805cf98b4");

        assertEquals(template.json().get("template_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(template.json().get("ok"), "TEMPLATE_FOUND");
    }

    @Test
    public void testUpdateTemplate()
            throws TransloaditRequestException, TransloaditSignatureException, InterruptedException {
        stubFor(put(urlPathEqualTo("/templates/55c965a063a311e6ba2d379ef10b28f7"))
                .willReturn(aResponse().withBodyFile("update_template.json")));

        // stub returns no response when this request is run too quickly.
        Thread.sleep(2000);

        Response template = transloadit.updateTemplate("55c965a063a311e6ba2d379ef10b28f7",
                new HashMap<String, Object>());

        assertEquals(template.json().get("ok"), "TEMPLATE_UPDATED");
    }

    @Test
    public void testDeleteTemplate()
            throws TransloaditSignatureException, TransloaditRequestException, InterruptedException {
        stubFor(delete(urlPathEqualTo("/templates/11148db0ec4f11e6a05ca3d04d2a53e6"))
                .willReturn(aResponse().withBodyFile("delete_template.json")));

        // stub returns no response when this request is run too quickly.
        Thread.sleep(2000);

        Response deletedTemplate = transloadit.deleteTemplate("11148db0ec4f11e6a05ca3d04d2a53e6");
        assertEquals(deletedTemplate.json().get("ok"), "TEMPLATE_DELETED");
    }

    @Test
    public void testListTemplates() throws TransloaditRequestException, TransloaditSignatureException{
        stubFor(get(urlPathEqualTo("/templates"))
                .willReturn(aResponse().withBodyFile("templates.json")));

        ListResponse templates = transloadit.listTemplates();
        assertEquals(templates.size, 0);
    }

    @Test
    public void testGetBill() throws TransloaditSignatureException, TransloaditRequestException{
        stubFor(get(urlPathEqualTo("/bill/2016-09"))
                .willReturn(aResponse().withBodyFile("bill.json")));

        Response bill = transloadit.getBill(9, 2016);
        assertEquals(bill.json().get("invoice_id"), "76fe5df1c93a0a530f3e583805cf98b4");
    }

}