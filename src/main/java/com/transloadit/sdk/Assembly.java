package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.TransloaditRequestException;
import com.transloadit.sdk.exceptions.TransloaditSignatureException;
import com.transloadit.sdk.response.AssemblyResponse;
import io.tus.java.client.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a new assembly being created
 */
public class Assembly extends OptionsBuilder {
    Map<String, Object> files;

    public Assembly(Transloadit transloadit) {
        this(transloadit, new Steps(), new HashMap<>(), new HashMap<>());
    }

    /**
     *
     * @param steps {@link Steps} the steps to add to the assembly.
     * @param files is a map of file names and files that are meant to be uploaded.
     * @param options map of extra options to be sent along with the request.
     */
    public Assembly(Transloadit transloadit, Steps steps, Map<String, File> files, Map<String, Object> options) {
        this.transloadit = transloadit;
        this.steps = steps;
        this.files = new HashMap<>(files);
        this.options = options;
    }

    /**
     * Adds a file to your assembly.
     *
     * @param file {@link File} the file to be uploaded.
     * @param name {@link String} the name you the file to be given in transloadit
     */
    public void addFile(File file, String name){
        files.put(name, file);
    }

    /**
     * Adds a file to your assembly but automatically genarates the name of the file.
     *
     * @param file {@link File} the file to be uploaded.
     */
    public void addFile(File file){
        String name = "file_";

        for (int i = files.size(); files.containsKey(name); i++) {
            name += i;
        }
        files.put(name, file);
    }

    private void processResumables(String assemblyUrl) throws IOException, ProtocolException {
        for (Map.Entry<String, Object> entry :
                files.entrySet()) {
            resumableUpload((File) entry.getValue(), entry.getKey(), assemblyUrl);
        }
    }

    private void resumableUpload(File file, String name, String assemblyUrl)
            throws IOException, ProtocolException {
        TusClient client = new TusClient();
        client.setUploadCreationURL(new URL(transloadit.hostUrl + "/resumable/files/"));

        client.enableResuming(new TusURLMemoryStore());
        final TusUpload upload = new TusUpload(file);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("filename", name);
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", "file");

        upload.setMetadata(metadata);

        TusUploader uploader = client.resumeOrCreateUpload(upload);
        uploader.setChunkSize(1024);

        int uploadedChunk = 0;
        while (uploadedChunk > -1) {
            uploadedChunk = uploader.uploadChunk();
        }
        uploader.finish();
    }

    /**
     * Submits the configured assembly to Transloadit for processing.
     *
     * @return {@link AssemblyResponse}
     * @throws TransloaditRequestException
     */
    public AssemblyResponse save(boolean useTus)
            throws TransloaditRequestException, TransloaditSignatureException, IOException, ProtocolException {
        Request request = new Request(transloadit);
        options.put("steps", steps.toMap());

        if (!useTus) {
            return new AssemblyResponse(request.post("/assemblies", options, files));
        } else {
            Map<String, Object> tusOptions = new HashMap<>();
            tusOptions.put("tus_num_expected_upload_files", files.size());
            AssemblyResponse response = new AssemblyResponse(
                    request.post("/assemblies", options, tusOptions), true);
            processResumables(response.sslUrl);
            return response;
        }
    }

    public  AssemblyResponse save()
            throws ProtocolException, TransloaditSignatureException,
            TransloaditRequestException, IOException {
        return this.save(false);
    }
}
