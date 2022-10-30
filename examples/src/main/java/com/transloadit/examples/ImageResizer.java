package com.transloadit.examples;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.AssemblyListener;
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This example resizes 2 uploaded files, both to 75x75.
 */
public final class ImageResizer {
    /**
     * Runs a Transloadit assembly which resizes two examples images.
     * @param args
     */
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        Map<String, Object> stepOptions = new HashMap<>();
        stepOptions.put("width", 75);
        stepOptions.put("height", 75);
        stepOptions.put("resize_strategy", "pad");

        Assembly assembly = transloadit.newAssembly();
        assembly.addStep("resize", "/image/resize", stepOptions);

        assembly.addFile(new File(ImageResizer.class.getResource("/lol_cat.jpg").getFile()));
        assembly.addFile(new File(ImageResizer.class.getResource("/mona_lisa.jpg").getFile()));
        assembly.setAssemblyListener(new AssemblyListener() {
            @Override
            public void onAssemblyFinished(AssemblyResponse response) {
                JSONArray result = response.getStepResult("resize");
                System.out.println("Resize result:");
                for (int i = 0; i < result.length(); i++) {
                    JSONObject item = result.getJSONObject(i);
                    System.out.println(String.format("%s.%s: %s",
                            item.getString("basename"), item.getString("ext"), item.getString("ssl_url")));
                }
            }

            @Override
            public void onError(Exception error) {
                error.printStackTrace();
            }

            @Override
            public void onMetadataExtracted() {
                System.out.println("Metadata Extracted");
            }

            @Override
            public void onAssemblyUploadFinished() {
                System.out.println("Assembly Upload complete, Executing ...");

            }

            @Override
            public void onFileUploadFinished(String fileName, JSONObject uploadInformation) {
                System.out.println("File uploaded: " + fileName);
            }

            @Override
            public void onFileUploadPaused(String name) {

            }

            @Override
            public void onFileUploadResumed(String name) {

            }

            @Override
            public void onFileUploadProgress(long uploadedBytes, long totalBytes) {

            }

            @Override
            public void onAssemblyResultFinished(String stepName, JSONObject result) {

            }

        });

        try {
            System.out.println("Uploading ...");
            assembly.save();
        } catch (RequestException | LocalOperationException e) {
            e.printStackTrace();
        }
    }
    /**
     * Prohibits instantiation of utility class.
     */
    private ImageResizer() {
        throw new IllegalStateException("Utility class");
    }
}
