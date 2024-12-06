package com.transloadit.examples;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This Example adds a watermark to an uploaded video file.
 * The watermark image is passed as a url to the "watermark_url" parameter.
 */
public final class Watermarker {

    /**
     * Runs a Watermarking Assembly.
     * @param args
     */
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit(System.getenv("TRANSLOADIT_KEY"), System.getenv("TRANSLOADIT_SECRET"));

        Map<String, Object> stepOptions = new HashMap<>();
        stepOptions.put("use", ":original");
        stepOptions.put("background", "aaaaaa");
        stepOptions.put("watermark_url",
                "https://github.com/transloadit/java-sdk/raw/main/examples/src/main/resources/watermark.png");
        stepOptions.put("result", true);

        Assembly assembly = transloadit.newAssembly();
        assembly.addStep("encode", "/video/encode", stepOptions);

        assembly.addFile(new File(ImageResizer.class.getResource("/sample_mpeg4.mp4").getFile()));

        try {
            System.out.println("Uploading ...");
            AssemblyResponse response = assembly.save();

            // wait for assembly to finish executing.
            System.out.println("waiting for assembly to finish ...");
            while (!response.isFinished()) {
                response = transloadit.getAssemblyByUrl(response.getSslUrl());

                // wait for 500ms before checking again in order to avoid hitting the rate limit
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            String resultUrl = response.getStepResult("encode").getJSONObject(0).getString("ssl_url");
            System.out.println("Here's your assembly result: " + resultUrl);
        } catch (RequestException | LocalOperationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prohibits instantiation of utility class.
     */
    private Watermarker() {
        throw new IllegalStateException("Utility class");
    }
}
