package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.TransloaditRequestException;
import com.transloadit.sdk.exceptions.TransloaditSignatureException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import io.tus.java.client.ProtocolException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("key", "SECRET");

        try {
            Assembly assembly = transloadit.newAssembly();
            assembly.addStep("encode", "/video/encode", new HashMap());
            assembly.addFile(new File("LICENSE"));

            AssemblyResponse ass = assembly.save(true);

            System.out.println(ass.id);
            System.out.println(ass.url);
            System.out.println(ass.json());

            ListResponse list = transloadit.listAssemblies();

            System.out.println(list.json());
            System.out.println(list.items.get(0));
            System.out.println(list.size);

        } catch (TransloaditRequestException | TransloaditSignatureException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
