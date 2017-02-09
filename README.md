# transloadit-java-sdk
Java client for Transloadit upload service http://transloadit.com

## Getting started


```java
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.Transloadit;
...

Transloadit transloadit = new Transloadit("Auth_key", "Auth_secret", 3600);

// create an assembly
Assembly assembly = transloadit.newAssembly();

assembly.addStep("encode", "/video/encode", new HashMap());
assembly.addFile(new File("LICENSE"));

// or add template id
assembly.addOption("template_id", "3abcde3453ffccadb");

try {
    AssemblyResponse assemblyResponse = assembly.save();

    System.out.println(assemblyResponse.id);
    System.out.println(assemblyResponse.url);
    System.out.println(assemblyResponse.json());
} catch (TransloaditRequestException | TransloaditSignatureException e) {
  // handle exception here
}


// list all assemblies assemblies
ListResponse list = transloadit.listAssemblies();

// iterable json array
list.items;

// first element in list
list.items.get(0);

// list size
list.size;

```