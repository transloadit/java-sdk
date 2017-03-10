package com.transloadit.sdk;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class MockHttpService {
    protected final int PORT = 9040;
    protected final Transloadit transloadit = new Transloadit("KEY", "SECRET", "http://localhost:" + PORT);

    protected String getJson (String name) throws IOException {
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
