package com.transloadit.examples;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.AssemblyListener;
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
/*
    This example shows the usage of templates and the ability to override them.
    To use this example you need to add the following template to your Transloadit App:
*/

/*
    {
        "steps": {
            "screenshot": {
                "robot": "/html/convert",
                "url": "placeholder_which_will_be_overwritten",
                "result": true
            }
        }
    }
 */

public final class OverrideTemplate {
    /**
     * Runs a Transloadit assembly which screenshots a website.
     *
     * @param args
     */
    public static void main(String[] args) {
        Transloadit transloadit = new Transloadit(System.getenv("TRANSLOADIT_KEY"), System.getenv("TRANSLOADIT_SECRET"));

        Map<String, Object> stepOptions = new HashMap<>();
        stepOptions.put("url", "https://transloadit.com/");

        Assembly assembly = transloadit.newAssembly();
        assembly.addStep("screenshot", stepOptions);

        assembly.addOption("template_id", "YOUR_TEMPLATE_ID");


        assembly.setAssemblyListener(new AssemblyListener() {
            @Override
            public void onAssemblyFinished(AssemblyResponse response) {
                JSONArray result = response.getStepResult("screenshot");
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
            public void onAssemblyProgress(double combinedProgress, JSONObject progressPerOriginalFile) {

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
    private OverrideTemplate() {
        throw new IllegalStateException("Utility class");
    }
}
