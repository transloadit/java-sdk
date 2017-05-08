package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;

import java.io.File;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("KEY", "SECRET");

        Assembly assembly = transloadit.newAssembly();
        assembly.addStep("encode", "/video/encode", new HashMap<String, Object>());
        assembly.addFile(new File("LICENSE"));

        try {
            transloadit.setRequestSigning(false);

            AssemblyResponse ass = assembly.save(true);

            System.out.println(ass.getId());
            System.out.println(ass.getUrl());
            System.out.println(ass.json());

            transloadit.setRequestSigning(true);

            ListResponse list = transloadit.listAssemblies();

            System.out.println(list.json());
            System.out.println(list.getItems().get(0));
            System.out.println(list.size());

        } catch (RequestException e) {
            e.printStackTrace();
        } catch (LocalOperationException e){
            e.printStackTrace();
        }
    }
}
