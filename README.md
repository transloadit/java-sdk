# transloadit-java-sdk
Java client for Transloadit upload service http://transloadit.com

## Getting started


```java
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;

import java.io.File;
import java.util.HashMap;

Transloadit transloadit = new Transloadit("Auth_key", "Auth_secret");

// create an assembly
Assembly assembly = transloadit.newAssembly();

assembly.addStep("encode", "/video/encode", new HashMap());
assembly.addFile(new File("LICENSE"));

// or add template id
assembly.addOption("template_id", "3abcde3453ffccadb");

try {
    AssemblyResponse assemblyResponse = assembly.save();

    System.out.println(assemblyResponse.getId());
    System.out.println(assemblyResponse.getUrl());
    System.out.println(assemblyResponse.json());
} catch (RequestException | LocalOperationException e) {
  // handle exception here
}


// list all assemblies assemblies
ListResponse list = transloadit.listAssemblies();

// iterable json array
list.getItems();

// first element in list
list.getItems().get(0);

// list size
list.size();

```