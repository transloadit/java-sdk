[![Build Status](https://travis-ci.org/transloadit/java-sdk.png?branch=master)](https://travis-ci.org/transloadit/java-sdk)

## java-sdk
A **Java** Integration for [Transloadit](https://transloadit.com)'s file uploading and encoding service


## Intro

[Transloadit](https://transloadit.com) is a service that helps you handle file uploads, resize, crop and watermark your images, make GIFs, transcode your videos, extract thumbnails, generate audio waveforms, and so much more. In short, [Transloadit](https://transloadit.com) is the Swiss Army Knife for your files.

This is a **Java** SDK to make it easy to talk to the [Transloadit](https://transloadit.com) REST API.

## Install

The JARs can be downloaded manually from our [Bintray project](https://bintray.com/transloadit/maven/transloadit/view#files).

**Gradle:**

```groovy
compile 'com.transloadit.sdk:transloadit:0.0.1'
```

**Maven:**

```xml
<dependency>
  <groupId>com.transloadit.sdk</groupId>
  <artifactId>transloadit</artifactId>
  <version>0.0.1</version>
</dependency>
```

## Usage


```java
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;

import java.io.File;
import java.util.HashMap;

// create an assembly
public class Main {

    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        Assembly assembly = transloadit.newAssembly();
        assembly.addStep("encode", "/video/encode", new HashMap<String, Object>());
        assembly.addFile(new File("PATH/TO/FILE.mp4"));
        
        try {
            AssemblyResponse ass = assembly.save(true);

            System.out.println(ass.getId());
            System.out.println(ass.getUrl());
            System.out.println(ass.json());

        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```

## License

[The MIT License](LICENSE).
