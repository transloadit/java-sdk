[![Build Status](https://travis-ci.org/transloadit/java-sdk.png?branch=master)](https://travis-ci.org/transloadit/java-sdk)

## java-sdk
A **Java** Integration for [Transloadit](https://transloadit.com)'s file uploading and encoding service


## Intro

[Transloadit](https://transloadit.com) is a service that helps you handle file uploads, resize, crop and watermark your images, make GIFs, transcode your videos, extract thumbnails, generate audio waveforms, and so much more. In short, [Transloadit](https://transloadit.com) is the Swiss Army Knife for your files.

This is a **Java** SDK to make it easy to talk to the [Transloadit](https://transloadit.com) REST API.

## Install

The JARs can be downloaded manually from our [Bintray project](https://bintray.com/transloadit/maven/transloadit/view#files),
or can be installed from the Maven and Jcenter repositories.

**Gradle:**

```groovy
compile 'com.transloadit.sdk:transloadit:0.0.2'
```

**Maven:**

```xml
<dependency>
  <groupId>com.transloadit.sdk</groupId>
  <artifactId>transloadit</artifactId>
  <version>0.0.2</version>
</dependency>
```

## Usage

All interactions with the SDK begin with the `com.transloadit.sdk.Transloadit` class.


### Create an Assembly

To create an assembly, you use the `newAssembly` method.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;

import java.io.File;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        Assembly assembly = transloadit.newAssembly();

        Map<String, Object> stepOptions = new HashMap<>();
        stepOptions.put("width", 75);
        stepOptions.put("height", 75);
        assembly.addStep("resize", "/image/resize", stepOptions);

        assembly.addFile(new File("PATH/TO/FILE.jpg"));
        try {
            AssemblyResponse response = assembly.save();

            // wait for assembly to finish executing.
            while (!response.isFinished()) {
                response = transloadit.getAssemblyByUrl(response.getSslUrl());
            }

            System.out.println(response.getId());
            System.out.println(response.getUrl());
            System.out.println(response.json());

        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```


### Get an Assembly

To get an assembly, you use the `getAssembly` method, passing the assembly id as a parameter.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        try {
            AssemblyResponse response = transloadit.getAssembly("ASSEMBLY_ID");

            System.out.println(response.getUrl());
            System.out.println(response.json());

        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```


You may also get an assembly by url with the `getAssemblyByUrl` method.

```java
    AssemblyResponse response = transloadit.getAssemblyByUrl("ASSEMBLY_URL");
```


### Cancel an Assembly

To cancel an executing assembly, you use the `cancelAssembly` method, passing the assembly id as a parameter.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        try {
            AssemblyResponse response = transloadit.cancelAssembly("ASSEMBLY_ID");

            System.out.println(response.isCancelled()); // prints true
        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```


### List Assemblies

To get a list of all assemblies under your account, you use the `listAssemblies` method.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.ListResponse;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        try {
            ListResponse response = transloadit.listAssemblies();

            System.out.println(response.size());  // number of assemblies on the list.
            System.out.println(response.getItems());  // returns an iterable json array

        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```


### Create a Template

To create a new template, you use the `newTemplate` method, passing the template name as a parameter.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.Response;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        Template template = transloadit.newTemplate("MY_TEMPLATE_NAME");

        Map<String, Object> stepOptions = new HashMap<>();
        stepOptions.put("width", 75);
        stepOptions.put("height", 75);
        template.addStep("resize", "/image/resize", stepOptions);

        try {
            Response response = template.save(true);

            System.out.println(response.json());
            System.out.println(response.json().getString("id")); // gets the template id.
        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```


### Update a Template

To update a template, you use the `updateTemplate` method, passing the template id and options to update as a parameters.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.Steps;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.Response;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        Steps steps = new Steps();

        Map<String, Object> stepOptions = new HashMap<>();
        stepOptions.put("width", 150);
        stepOptions.put("height", 150);
        steps.addStep("resize", "/image/resize", stepOptions);

        Map<String, Object> templateOptions = new HashMap<>();
        templateOptions.put("steps", steps.toMap());
        templateOptions.put("name", "MY_NEW_TEMPLATE_NAME");

        try {
            Response response = transloadit.updateTemplate("TEMPLATE_ID", templateOptions);

        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```


### Delete a Template

To delete a template, you use the `deleteTemplate` method, passing the template id as a parameter.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.Response;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        try {
            Response response = transloadit.deleteTemplate("TEMPLATE_ID");

        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```


### List Templates

To get a list of all templates under your account, you use the `listTemplates` method.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.ListResponse;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        try {
            ListResponse response = transloadit.listTemplates();

            System.out.println(response.size());  // number of assemblies on the list.
            System.out.println(response.getItems());  // returns an iterable json array

        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```


### Get a template

To get a particular template, you use the `getTemplate` method, passing the template id as a parameter.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.Response;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        try {
            AssemblyResponse response = transloadit.getTemplate("TEMPLATE_ID");
            System.out.println(response.json());
        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```


### Get bill

To get your account billing details for a particular month, you use the `getBill` method, passing the month and year as parameters.

```java
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.Response;

public class Main {
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        try {
            ListResponse response = transloadit.getBill(3, 2017);

            System.out.println(response.json());
        } catch (RequestException | LocalOperationException e) {
            // handle exception here
        }
    }
}
```

## Example

For fully working examples take a look at [examples/](https://github.com/transloadit/java-sdk/tree/master/examples).

## Documentation

See [Javadoc](http://javadoc.io/doc/com.transloadit.sdk/transloadit/0.0.2) for full API documentation.


## License

[The MIT License](LICENSE).
