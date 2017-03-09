package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.TransloaditRequestException;
import com.transloadit.sdk.exceptions.TransloaditSignatureException;
import com.transloadit.sdk.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a new template being created
 */
public class Template extends OptionsBuilder {
    public String name;

    /**
     *
     * @param transloadit {@link Transloadit} an instance of transloadit client class.
     * @param name name of the template.
     * @param steps {@link Steps} the steps to add to the template.
     * @param options map of extra options to be sent along with the request.
     */
    public Template(Transloadit transloadit, String name, Steps steps, Map<String, Object> options) {
        this.transloadit = transloadit;
        this.name = name;
        this.steps = steps;
        this.options = options;
    }

    public Template(Transloadit transloadit, String name) {
        this(transloadit, name, new Steps(), new HashMap<String, Object>());
    }

    /**
     * Submits the configured template to Transloadit.
     *
     * @return {@link Response}
     * @throws TransloaditRequestException
     * @throws TransloaditSignatureException
     */
    public Response save() throws TransloaditRequestException, TransloaditSignatureException {
        Map<String, Object> templateData = new HashMap<String, Object>();
        templateData.put("name", name);

        options.put("steps", steps.toMap());

        templateData.put("template", options);
        Request request = new Request(transloadit);
        return new Response(request.post("/templates", templateData));
    }
}
