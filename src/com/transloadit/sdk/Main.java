package com.transloadit.sdk;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("KEY", "SECRET", 3600);
        Assembly assemblyApi = transloadit.assembly();

        try {
            File[] files = new File[1];
            files[0] = new File("LICENSE");

            Map options = new HashMap();
            Steps steps = new Steps();
            steps.addStep("encode", "/video/encode", new HashMap());
            options.put("steps", steps.asHash());

            AssemblyResponse assembly = assemblyApi.create(options, files);

            System.out.println(assembly.id);
            System.out.println(assembly.url);
            System.out.println(assembly.json());

            ListResponse list = assemblyApi.list(new HashMap());

            System.out.println(list.json());
            System.out.println(list.items.get(0));
            System.out.println(list.size);

        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }
}
