package com.transloadit.sdk;

import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;

import java.io.File;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("KEY", "SECRET", 3600);
        AssemblyApi assemblyApi = transloadit.assemblyApi();

        try {
            AssemblyApi.Assembly assembly = assemblyApi.new_();
            assembly.addStep("encode", "/video/encode", new HashMap());
            assembly.addFile(new File("LICENSE"));

            AssemblyResponse ass = assembly.save();

            System.out.println(ass.id);
            System.out.println(ass.url);
            System.out.println(ass.json());

            ListResponse list = assemblyApi.list();

            System.out.println(list.json());
            System.out.println(list.items.get(0));
            System.out.println(list.size);

        } catch (TransloaditRequestException e) {
            e.printStackTrace();
        }
    }
}
