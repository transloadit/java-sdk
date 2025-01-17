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
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * This Example demonstrates a multi-step Assembly.
 *  - Step1 converts two given audio files to mp3 with a bitrate of 128 kbit/s (128000 bits/s)
 *  - Step2 concatenates Step1's results to one file
 *  - Step3 derives a Waveform Image from Step2's output
 */
public final class MultiStepProcessing {
    /**
     * Runs a multistep Transloadit assembly.
     * @param args
     */
    public static void main(String[] args) throws FileNotFoundException {
        // New Transloadit Instance
        Transloadit transloadit = new Transloadit(System.getenv("TRANSLOADIT_KEY"), System.getenv("TRANSLOADIT_SECRET"));
        Assembly assembly = transloadit.newAssembly();

        // Add Files and define Field name
        assembly.addFile(new File(Objects.requireNonNull(MultiStepProcessing.class.getResource(
                "/dutch-anthem.mp3")).getFile()), "file_1");
        assembly.addFile(new File(Objects.requireNonNull(MultiStepProcessing.class.getResource(
                "/german-anthem-0.mp3")).getFile()), "file_2");

        // Step1 Reduce File's Bitrates
        Map<String, Object> step1 = new HashMap<>();
        step1.put("preset", "opus");
        step1.put("bitrate", 128000);

        assembly.addStep("encode", "/audio/encode", step1);

        // Step2 Concatenation
            /* Building "use" parameter as JSONObject
                -   name = Name of previous step
                -   fields = Name of outfile from previous step
                -   as = audio_<number> defines oder of Concatenation
                => Needs to be stored under key "steps" as it defines every substep
             */
        JSONObject outerJsonObject = new JSONObject();
        outerJsonObject.append(
                "steps",
                new JSONObject()
                        .put("name", "encode")
                        .put("fields", "file_1")
                        .put("as", "audio_2"));  // invert order of files in the concat for demonstration purpose

        outerJsonObject.append(
                "steps",
                new JSONObject()
                        .put("name", "encode")
                        .put("fields", "file_2")
                        .put("as", "audio_1"));

        Map<String, Object> step2 = new HashMap<>();

        step2.put("preset", "mp3");
        step2.put("use", outerJsonObject);

        assembly.addStep("concat", "/audio/concat", step2);

        // Step 3 Waveform
        Map<String, Object> step3 = new HashMap<>();
        step3.put("use", "concat");
        step3.put("width", 1920);
        step3.put("height", 720);
        step3.put("outer_color", "ff00c7ff");
        assembly.addStep("waveform", "/audio/waveform", step3);

        // Register AssemblyListener => Informs User on Assembly completion
        assembly.setAssemblyListener(new AssemblyListener() {
            @Override
            public void onAssemblyFinished(AssemblyResponse response) {
                System.out.println("Assembly finished");
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
                System.out.println("Assembly Execution Progress: " + combinedProgress + "%");
            }

            @Override
            public void onAssemblyResultFinished(JSONArray result) {
                String stepName = result.getString(0);
                JSONObject resultData = result.getJSONObject(1);
                System.out.println("\n ---- Step Result for Step: ---- ");
                System.out.println("Step name: " + stepName + "\nFile: " + resultData.get("name"));
                System.out.println("Download link: " + resultData.getString("ssl_url") + "\n");
            }
        });
        try {
            System.out.println("Processing... ");
            assembly.setMaxParallelUploads(2);
            assembly.setUploadChunkSize(150);
            System.out.println("Assembly ID: " + assembly.getClientSideGeneratedAssemblyID());
            assembly.save(true);

            Thread.sleep(50);
            assembly.pauseUploads();
            Thread.sleep(3000);
            assembly.resumeUploads();

         } catch (LocalOperationException | RequestException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    /**
     * Prohibits instantiation of utility class.
     */
    private MultiStepProcessing() {
        throw new IllegalStateException("Utility class");
    }

}
