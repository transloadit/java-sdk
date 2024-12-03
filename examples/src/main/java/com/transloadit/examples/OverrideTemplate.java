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
                System.out.println("Assembly finished");
                System.out.println("Result: " + response.json().getJSONObject("results")
                        .getJSONArray("screenshot").getJSONObject(0).getString("ssl_url"));
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
                System.out.println("Assembly upload complete. Executing...");
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
                System.out.println("Uploaded " + uploadedBytes + "/" + totalBytes + " bytes");

            }

            @Override
            public void onAssemblyProgress(JSONObject progressPerOriginalFile) {
                double combinedProgress = progressPerOriginalFile.getDouble("bytes_processed")
                        / progressPerOriginalFile.getDouble("bytes_expected");
                System.out.println("Assembly Execution Progress: " + combinedProgress + "%");
            }

            @Override
            public void onAssemblyResultFinished(JSONArray result) {
                String stepName = result.getString(0);
                JSONObject resultData = result.getJSONObject(1);
                System.out.println("\n ---- Step Result for Step: ---- ");
                System.out.println("Step name: " + stepName + "\nFile: " + resultData.get("name") + ".");
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
    private OverrideTemplate() {
        throw new IllegalStateException("Utility class");
    }
}
