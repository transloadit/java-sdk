package com.transloadit.sdk;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class provides an universal Template for running mock-http Test with identical settings.
 */
public class MockHttpService {
    //CHECKSTYLE:OFF
    // Checkstyle was turned off here to enable the upper case variable name of the "PORT" variable.
    protected final int PORT = 9040;
    protected final Transloadit transloadit = new Transloadit("KEY", "SECRET",
            "http://localhost:" + PORT);
    //CHECKSTYLE:ON

    /**
     * Provides an Assembly object without Assembly ID.
     * @return Assembly object without Assembly ID
     */
    protected Assembly newAssemblyWithoutID() {
        Assembly assembly = new Assembly(transloadit);
        assembly.wipeAssemblyID();
        return assembly;
    }
    /**
     * Loads test resources from src/test/resources/__files/ with the specified name and provides them as a JSON String.
     * @param name name of test resource.
     * @return JSON string of test resource
     * @throws IOException if specified test resource can't be found.
     */
    protected String getJson(String name) throws IOException {
        String filePath = "src/test/resources/__files/" + name;

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line).append("\n");
            line = br.readLine();
        }

        return sb.toString();
    }
}
