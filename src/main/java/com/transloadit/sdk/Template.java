package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a new template being created
 */
public class Template extends OptionsBuilder {
    private String name;

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

    /**
     *
     * @param transloadit {@link Transloadit} an instance of transloadit client class.
     * @param name name of the template.
     */
    public Template(Transloadit transloadit, String name) {
        this(transloadit, name, new Steps(), new HashMap<String, Object>());
    }

    /**
     * Set the name of the template
     * @param name name of the template
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return name of the template
     */
    public String getName() {
        return name;
    }

    /**
     * Submits the configured template to Transloadit.
     *
     * @return {@link Response}
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public Response save() throws RequestException, LocalOperationException {
        Map<String, Object> templateData = new HashMap<String, Object>();
        templateData.put("name", name);

        options.put("steps", steps.toMap());

        templateData.put("template", options);
        Request request = new Request(transloadit);
        return new Response(request.post("/templates", templateData));
    }
}
