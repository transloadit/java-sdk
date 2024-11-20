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
        Transloadit transloadit = new Transloadit(System.getenv("TRANSLOADIT_KEY"), System.getenv("TRANSLOADIT_SECRET"));

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
                System.out.println("Assembly finished");
                System.out.println("Result: " + response.getSslUrl());
            }

            @Override
            public void onError(Exception error) {
                System.out.println("Error");
                error.printStackTrace();
            }

            @Override
            public void onMetadataExtracted() {
                System.out.println("Metadata extracted");
            }

            @Override
            public void onAssemblyUploadFinished() {
                System.out.println("Assembly Upload complete. Executing ...");
            }

            @Override
            public void onFileUploadFinished(JSONObject uploadInformation) {
                String fileName = uploadInformation.getString("name");
                System.out.println("File uploaded: " + fileName);
            }

            @Override
            public void onFileUploadPaused(String name) {
                System.out.println("File upload paused: " + name);
            }

            @Override
            public void onFileUploadResumed(String name) {
                System.out.println("File upload resumed: " + name);

            }

            @Override
            public void onFileUploadProgress(long uploadedBytes, long totalBytes) {
                System.out.println("Uploaded " + uploadedBytes + "/" + totalBytes + " Bytes");

            }

            @Override
            public void onAssemblyProgress(JSONObject progressPerOriginalFile) {
                double combinedProgress = progressPerOriginalFile.getDouble("bytes_processed")
                        / progressPerOriginalFile.getDouble("bytes_expected");
                System.out.println("Assembly Execution Progress: " + combinedProgress);
            }

            @Override
            public void onAssemblyResultFinished(JSONArray result) {
                String stepName = result.getString(0);
                JSONObject resultData = result.getJSONObject(1);
                System.out.println("\n ---- Step Result for Step: ---- ");
                System.out.println("StepName: " + stepName + "\nFile: " + resultData.get("basename") + "."
                        + resultData.get("ext"));
                System.out.println("Download link: " + resultData.getString("ssl_url") + "\n");
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
