package com.transloadit.sdk;

import com.launchdarkly.eventsource.ConnectStrategy;
import com.launchdarkly.eventsource.ErrorStrategy;
import com.launchdarkly.eventsource.RetryDelayStrategy;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusURLMemoryStore;
import io.tus.java.client.TusURLStore;
import io.tus.java.client.TusUpload;
// CHECKSTYLE:OFF
// Only needed for Java Doc
import io.tus.java.client.TusUploader;
// CHECKSTYLE:ON
import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class represents a new assembly being created.
 */
public class Assembly extends OptionsBuilder {
    private TusURLStore tusURLStore;
    protected String assemblyId;

    protected Map<String, File> files;
    protected Map<String, InputStream> fileStreams;
    protected TusClient tusClient;
    protected List<TusUpload> uploads;
    protected boolean shouldWaitForCompletion;
    protected AssemblyListener assemblyListener;
    protected AssemblyListener runnableAssemblyListener;

    protected ArrayList<TusUploadRunnable> threadList;
    private HashMap<String, Exception> threadExceptions;
    private int maxParallelUploads = 2;
    private ThreadPoolExecutor executor;

    private long uploadSize;
    private long uploadedBytes;
    protected int uploadChunkSize = 0;

    /**
     * Calls {@link #Assembly(Transloadit, Steps, Map, Map)} with the transloadit
     * client as parameter.
     *
     * @param transloadit {@link Transloadit} the transloadit client.
     */
    public Assembly(Transloadit transloadit) {
        this(transloadit, new Steps(), new HashMap<String, File>(), new HashMap<String, Object>());
    }

    /**
     * Constructs a new instance of the Assembly object.
     *
     * @param transloadit {@link Transloadit} the transloadit client.
     * @param steps       {@link Steps} the steps to add to the assembly.
     * @param files       is a map of file names and files that are meant to be
     *                    uploaded.
     * @param options     map of extra options to be sent along with the request.
     */
    public Assembly(Transloadit transloadit, Steps steps, Map<String, File> files, Map<String, Object> options) {
        this.transloadit = transloadit;
        this.steps = steps;
        this.files = files;
        this.options = options;
        tusClient = new TusClient();
        tusURLStore = new TusURLMemoryStore();
        uploads = new ArrayList<TusUpload>();
        fileStreams = new HashMap<String, InputStream>();
        shouldWaitForCompletion = false;
        threadList = new ArrayList<TusUploadRunnable>();
        threadExceptions = new HashMap<String, Exception>();
        assemblyId = generateAssemblyID();
    }

    /**
     * Adds a file to your assembly. If the field name specified already exists, it
     * will override the content of the
     * existing name. This also means that previously added and similarly named
     * {@link java.io.FileInputStream FileInputStreams} will be replaced.
     *
     * @param file {@link File} the file to be uploaded.
     * @param name {@link String} the field name of the file when submitted
     *             Transloadit.
     */
    public void addFile(File file, String name) {
        files.put(name, file);

        // remove duplicate key
        if (fileStreams.containsKey(name)) {
            fileStreams.remove(name);
        }
    }

    /**
     * Adds a file to your assembly but automatically generates the field name of
     * the file.
     *
     * @param file {@link File} the file to be uploaded.
     */
    public void addFile(File file) {
        String name = "file";
        files.put(normalizeDuplicateName(name), file);
    }

    /**
     * Adds a file to your assembly. If the field name specified already exists, it
     * will override the content of the
     * existing name. This also means that previously added and similarly named
     * {@link java.io.File Files} will be replaced.
     *
     * @param inputStream {@link InputStream} the file to be uploaded.
     * @param name        {@link String} the field name of the file when submitted
     *                    Transloadit.
     */
    public void addFile(InputStream inputStream, String name) {
        fileStreams.put(name, inputStream);

        // remove duplicate key
        if (files.containsKey(name)) {
            files.remove(name);
        }
    }

    /**
     * Adds a file to your assembly but automatically genarates the name of the
     * file.
     *
     * @param inputStream {@link InputStream} the file to be uploaded.
     */
    public void addFile(InputStream inputStream) {
        String name = "file";
        fileStreams.put(normalizeDuplicateName(name), inputStream);
    }

    /**
     * Removes file from your assembly.
     *
     * @param name field name of the file to remove.
     */
    public void removeFile(String name) {
        files.remove(name);

        fileStreams.remove(name);
    }

    /**
     * Sets a listener that should be called after the assembly is done executing.
     *
     * @param assemblyListener {@link AssemblyListener}
     */
    public void setAssemblyListener(AssemblyListener assemblyListener) {
        this.assemblyListener = assemblyListener;
        shouldWaitForCompletion = assemblyListener != null;
    }

    /**
     * Returns the listener that should be called after assembly executing has been
     * finished.
     *
     * @return {@link AssemblyListener}
     */
    public AssemblyListener getAssemblyListener() {
        return assemblyListener;
    }

    /**
     * Determine whether or not to wait till the assembly is complete after it is
     * saved.
     *
     * @deprecated use {@link #setAssemblyListener(AssemblyListener)} instead
     * @param shouldWaitForCompletion boolean value to determine whether or not to
     *                                wait till the assembly is complete
     */
    @Deprecated
    public void setShouldWaitForCompletion(boolean shouldWaitForCompletion) {
        this.shouldWaitForCompletion = shouldWaitForCompletion;
    }

    /**
     * Normalizes a duplicated filename by adding an underscore and a incrementing
     * number.
     *
     * @param name duplicated Filename
     * @return renamed filename
     */
    private String normalizeDuplicateName(String name) {
        for (int i = files.size(); files.containsKey(name); i++) {
            name += "_" + i;
        }

        for (int i = fileStreams.size(); fileStreams.containsKey(name); i++) {
            name += "_" + i;
        }

        return name;
    }

    /**
     * Returns the number of files that have been added for upload.
     *
     * @return the number of files that have been added for upload.
     */
    public int getNumberOfFiles() {
        return files.size() + fileStreams.size();
    }

    /**
     * Set custom Url Storage. This should be an implementation of
     * {@link TusURLStore}.
     *
     * @param store {@link TusURLStore} the storage instance.
     */
    public void setTusURLStore(TusURLStore store) {
        tusURLStore = store;
    }

    /**
     * Submits the configured assembly to Transloadit for processing.
     *
     * @param isResumable boolean value that tells the assembly whether or not to
     *                    use tus.
     * @return {@link AssemblyResponse} the response received from the Transloadit
     *         server.
     * @throws RequestException        if request to Transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running
     *                                 non-http operations.
     */
    public AssemblyResponse save(boolean isResumable)
            throws RequestException, LocalOperationException {
        Request request = new Request(getClient());
        if (!steps.toMap().isEmpty()) {
            options.put("steps", steps.toMap());
        }

        AssemblyResponse response;
        // only do tus uploads if files will be uploaded
        if (isResumable && getNumberOfFiles() > 0) {
            Map<String, String> tusOptions = new HashMap<String, String>();
            tusOptions.put("tus_num_expected_upload_files", Integer.toString(getNumberOfFiles()));

            response = new AssemblyResponse(
                    request.post(obtainUploadUrlSuffix(), options, tusOptions, null, null), true);

            // check if the assembly returned an error
            if (response.hasError()) {
                throw new RequestException("Request to Assembly failed: " + response.json().getString("error"));
            }

            if (shouldWaitWithSSE()) {
                listenToServerSentEvents(response);
            }

            try {
                handleTusUpload(response);
            } catch (IOException e) {
                throw new LocalOperationException(e);
            } catch (ProtocolException e) {
                throw new RequestException(e);
            }
        } else {
            response = new AssemblyResponse(request.post(obtainUploadUrlSuffix(), options, null, files, fileStreams));
            if (shouldWaitWithSSE() && !response.isFinished()) {
                listenToServerSentEvents(response);
            }
        }

        return shouldWaitWithoutSSE() ? waitTillComplete(response) : response;
    }

    /**
     * Calls {@link #save(boolean)} with boolean isResumable = true.
     *
     * @return {@link AssemblyResponse} the response received from the Transloadit
     *         server.
     * @throws RequestException        if request to Transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running
     *                                 non-http operations.
     */
    public AssemblyResponse save() throws LocalOperationException, RequestException {
        return this.save(true);
    }

    /**
     * If tus uploads are enabled, this method would be called by
     * {@link Assembly#save()} to handle the file uploads.
     *
     * @param response {@link AssemblyResponse}
     * @throws IOException       when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     */
    protected void handleTusUpload(AssemblyResponse response) throws IOException, ProtocolException {
        processTusFiles(response.getSslUrl(), response.getTusUrl());
        uploadTusFiles();
    }

    /**
     * Prepares all files added for tus uploads.
     *
     * @param assemblyUrl the assembly url affiliated with the tus upload.
     * @param tusUrl      the tus url affiliated with the tus upload.
     * @throws IOException when there's a failure with file retrieval.
     */
    protected void processTusFiles(String assemblyUrl, String tusUrl) throws IOException {
        tusClient.setUploadCreationURL(new URL(tusUrl));
        tusClient.enableResuming(tusURLStore);

        for (Map.Entry<String, File> entry : files.entrySet()) {
            processTusFile(entry.getValue(), entry.getKey(), assemblyUrl);
        }

        for (Map.Entry<String, InputStream> entry : fileStreams.entrySet()) {
            processTusFile(entry.getValue(), entry.getKey(), assemblyUrl);
        }
    }

    /**
     * Prepares all files added for tus uploads.
     *
     * @param assemblyUrl the assembly url affiliated with the tus upload.
     * @throws IOException       when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     * @deprecated ideally this method should make uploads to the tus url assigned
     *             to an assembly, but it doesn't
     */
    @Deprecated
    protected void processTusFiles(String assemblyUrl) throws IOException, ProtocolException {
        processTusFiles(assemblyUrl, getClient().getHostUrl() + "/resumable/files/");
    }

    /**
     * Prepares a file for tus upload.
     *
     * @param inputStream {@link InputStream}
     * @param fieldName   the form field name assigned to the file.
     * @param assemblyUrl the assembly url affiliated with the tus upload.
     * @throws IOException when there's a failure with reading the input stream.
     */
    protected void processTusFile(InputStream inputStream, String fieldName, String assemblyUrl) throws IOException {
        TusUpload upload = getTusUploadInstance(inputStream, fieldName, assemblyUrl);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("filename", fieldName);
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", fieldName);

        upload.setMetadata(metadata);

        uploads.add(upload);
    }

    /**
     * Prepares a file for tus upload.
     *
     * @param file        {@link File}
     * @param fieldName   the form field name assigned to the file.
     * @param assemblyUrl the assembly url affiliated with the tus upload.
     * @throws IOException when there's a failure with file retrieval.
     */

    protected void processTusFile(File file, String fieldName, String assemblyUrl) throws IOException {
        TusUpload upload = getTusUploadInstance(file);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("filename", file.getName());
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", fieldName);

        upload.setMetadata(metadata);

        uploads.add(upload);
    }

    /**
     * Returns the {@link TusUpload} instance that would be used to upload a file.
     *
     * @param inputStream {@link InputStream}
     * @param fieldName   {@link String} the field name assigned to the file
     * @param assemblyUrl {@link String} the assembly url
     * @return {@link TusUpload}
     * @throws IOException when there's a failure with reading the input stream.
     */
    protected TusUpload getTusUploadInstance(InputStream inputStream, String fieldName, String assemblyUrl)
            throws IOException {
        TusUpload tusUpload = new TusUpload();
        tusUpload.setInputStream(inputStream);
        tusUpload.setFingerprint(String.format("%s-%d-%s", fieldName, inputStream.available(), assemblyUrl));
        tusUpload.setSize(inputStream.available());

        return tusUpload;
    }

    /**
     * Returns the {@link TusUpload} instance that would be used to upload a file.
     *
     * @param file {@link File}
     * @return {@link TusUpload}
     * @throws FileNotFoundException when there's a failure with file retrieval.
     */
    protected TusUpload getTusUploadInstance(File file) throws FileNotFoundException {
        return new TusUpload(file);
    }

    /**
     * Calculates the expected uploadSize in Bytes.
     *
     * @return the expected cumulative upload size
     * @throws IOException Input Streams cannote be read
     */
    public long getUploadSize() throws IOException {
        long totalUploadSize = 0;
        for (Map.Entry<String, File> entry : files.entrySet()) {
            totalUploadSize += entry.getValue().length();
        }

        for (Map.Entry<String, InputStream> entry : fileStreams.entrySet()) {
            totalUploadSize += entry.getValue().available();
        }
        return totalUploadSize;
    }

    /**
     * Does the actual uploading of files (when tus is enabled).
     *
     * @throws IOException       when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     */
    protected void uploadTusFiles() throws IOException, ProtocolException {
        AssemblyListener localListener;
        if (assemblyListener == null) {
            runnableAssemblyListener = new AssemblyListener() {
                @Override
                public void onAssemblyFinished(AssemblyResponse response) {

                }

                @Override
                public void onError(Exception error) {

                }

                @Override
                public void onMetadataExtracted() {

                }

                @Override
                public void onAssemblyUploadFinished() {

                }

                @Override
                public void onFileUploadFinished(JSONObject uploadInformation) {

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
                public void onAssemblyProgress(JSONObject progressPerOriginalFile) {

                }

                @Override
                public void onAssemblyResultFinished(JSONArray result) {

                }
            };
        } else {
            runnableAssemblyListener = getAssemblyListener();
        }
        uploadSize = getUploadSize();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxParallelUploads);
        while (!uploads.isEmpty()) {
            final TusUpload tusUpload = uploads.remove(0);
            TusUploadRunnable tusUploadRunnable = new TusUploadRunnable(tusClient, tusUpload, uploadChunkSize, this);
            threadList.add(tusUploadRunnable);
            executor.execute(tusUploadRunnable);
        }
        executor.shutdown();
    }

    /**
     * Determines if the Client should wait until the Assembly execution is finished
     * by observing the
     * {@link AssemblyResponse} status.
     * <p>
     * Can only be {@code true} if <code>
     * {@link #shouldWaitForCompletion}  = true
     * </code> and no
     * {@link AssemblyListener} has been specified.
     * </p>
     *
     * @return
     *         <ul>
     *         <li>{@code true} if the client should wait for Assembly completion by
     *         observing the
     *         HTTP - Response;</li>
     *         <li>{@code false} if the client should not wait for completion by
     *         observing the HTTP - Response</li>
     *         </ul>
     * @see Assembly#save(boolean) Usage in Assembly.save()
     */
    protected boolean shouldWaitWithoutSSE() {
        return this.shouldWaitForCompletion && this.assemblyListener == null;
    }

    /**
     * Determines if the Client should wait until the Assembly execution is finished
     * by observing a server sent events (SSE).
     * <p>
     * Can only be {@code true} if
     * <code> {@link #shouldWaitForCompletion}  = true</code> and an
     * {@link AssemblyListener} has been specified.
     * </p>
     *
     * @return
     *         <ul>
     *         <li>{@code true} if the client should wait for Assembly completion by
     *         observing SSE</li>
     *         <li>{@code false} if the client should not wait for completion by
     *         observing the SSE.</li>
     *         </ul>
     * @see Assembly#save(boolean) Usage in Assembly.save()
     */
    protected boolean shouldWaitWithSSE() {
        return this.shouldWaitForCompletion && this.assemblyListener != null;
    }

    /**
     * Wait till the assembly is finished and then return the response of the
     * complete state.
     *
     * @param response         {@link AssemblyResponse}
     */
    private void listenToServerSentEvents(AssemblyResponse response) throws LocalOperationException {
        final URI sseUpdateStreamUrl = URI.create(response.getUpdateStreamUrl());
        final ConnectStrategy connectStrategy = ConnectStrategy.http(sseUpdateStreamUrl)
                .connectTimeout(30, TimeUnit.SECONDS).readTimeout(1, TimeUnit.MINUTES);
        final RetryDelayStrategy retryStrategy = RetryDelayStrategy.defaultStrategy().maxDelay(3, TimeUnit.SECONDS);
        final ErrorStrategy errorStrategy = ErrorStrategy.alwaysContinue();
        final EventsourceRunnable eventsourceRunnable =
                new EventsourceRunnable(transloadit, response, assemblyListener, connectStrategy, retryStrategy, errorStrategy, false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(eventsourceRunnable);
        executor.shutdown();
    }

    /**
     * Wait till the assembly is finished and then return the response of the
     * complete state.
     *
     * @param response {@link AssemblyResponse}
     * @return {@link AssemblyResponse}
     * @throws LocalOperationException if something goes wrong while running
     *                                 non-http operations.
     * @throws RequestException        if request to Transloadit server fails.
     */
    protected AssemblyResponse waitTillComplete(AssemblyResponse response)
            throws LocalOperationException, RequestException {
        try {
            // wait for assembly to finish executing.
            while (!response.isFinished()) {
                Thread.sleep(1000);
                response = transloadit.getAssemblyByUrl(response.getSslUrl());
            }
        } catch (InterruptedException e) {
            throw new LocalOperationException(e);
        }

        return response;
    }

    /**
     * Returns the uploadChunkSize which is used to determine after how many bytes
     * upload should the
     * {@link AssemblyListener#onFileUploadProgress(long, long)} callback be
     * triggered.
     *
     * @return uploadChunkSize
     */
    public int getUploadChunkSize() {
        return uploadChunkSize;
    }

    /**
     * Sets the uploadChunkSize which is used to determine after how many bytes
     * upload should the
     * {@link AssemblyListener#onFileUploadProgress(long, long)} callback be
     * triggered. If not set,
     * or if given the value of 0, the default set by {@link TusUploader} will be
     * used internally.
     *
     * @param uploadChunkSize the upload chunk size in bytes after which you want to
     *                        receive an upload progress
     */
    public void setUploadChunkSize(int uploadChunkSize) {
        this.uploadChunkSize = uploadChunkSize;
    }

    /**
     * This method sets how many uploads are performed simultaneously. If the number
     * of uploads exceeds the set value,
     * a queue is created and processed piece by piece.
     *
     * @param maxUploads maximum number of uploads, which are performed
     *                   simultaneously.
     */
    public void setMaxParallelUploads(int maxUploads) {
        this.maxParallelUploads = maxUploads;
    }

    /**
     * Returns current Assembly listener used by a {@link TusUploadRunnable}.
     *
     * @return {@link AssemblyListener}
     */
    public AssemblyListener getRunnableAssemblyListener() {
        return runnableAssemblyListener;
    }

    /**
     * Sets a custom Assembly listener used by a {@link TusUploadRunnable}.
     *
     * @param runnableAssemblyListener {@link AssemblyListener}
     */
    protected void setRunnableAssemblyListener(AssemblyListener runnableAssemblyListener) {
        this.runnableAssemblyListener = runnableAssemblyListener;
    }

    /***
     * Pause parallel file uploads.
     *
     * @throws LocalOperationException - If the pause operation fails inside the upload runnable.
     */
    public void pauseUploads() throws LocalOperationException {
        for (TusUploadRunnable thread : threadList) {
            int index = threadList.indexOf(thread);
            thread.setPaused();
        }
    }

    /***
     * Resume parallel file uploads.
     *
     * @throws LocalOperationException - If the resume operation fails inside the upload runnable.
     * @throws RequestException - If the resume operation fails on the server side.
     */
    public void resumeUploads() throws LocalOperationException, RequestException {
        for (TusUploadRunnable thread : threadList) {
            thread.setUnPaused();
        }
    }

    /**
     * Undocumented debug option, which is not intended for production use.
     * Providing custom Assembly IDs could lead to a security risk.
     *
     * @param assemblyId custom Assembly ID
     * @throws LocalOperationException if the provided id doesn't match the expected
     *                                 pattern.
     */
    @TestOnly
    public void setAssemblyId(String assemblyId) throws LocalOperationException {
        String id = assemblyId.toLowerCase();
        if (id.matches("[a-f0-9]{32}")) { // Check ID Format
            this.assemblyId = id;
        } else {
            throw new LocalOperationException("The provided Assembly ID doesn't match the expected pattern of "
                    + "\"[a-f0-9]{32}\"");
        }
    }

    /**
     * This Method is used to abort all parallel File uploads.
     * It informs the current {@link AssemblyListener} about the abortion.
     */
    public void abortUploads() {
        abortUploads(new LocalOperationException("Uploads aborted"));
    }

    /**
     * This Method is used to abort all parallel File uploads.
     * It informs the current {@link AssemblyListener} about the abortion.
     *
     * @param e {@link Exception that lead to the abortion}
     */
    protected void abortUploads(Exception e) {
        if (executor != null) {
            executor.shutdownNow();
        }
        runnableAssemblyListener.onError(e);
    }

    /**
     * This method removes finished Threads from the ThreadList.
     *
     * @param tusUploadThread a Upload Thread instance
     */
    synchronized void removeThreadFromList(TusUploadRunnable tusUploadThread) {
        threadList.remove(tusUploadThread);
    }

    /**
     * Updates the number of Bytes, which have been uploaded already.
     * Also triggers Upload finished if the uploads has been finished.
     *
     * @param uploadedBytes Number of bytes uploaded by the calling Thread.
     */
    protected synchronized void updateUploadProgress(long uploadedBytes) {
        this.uploadedBytes += uploadedBytes;
        runnableAssemblyListener.onFileUploadProgress(this.uploadedBytes, uploadSize);
    }

    /**
     * Takes a {@link LocalOperationException} from a running thread and stores it
     * in {@link #threadExceptions}.
     * Also stops the uploads and notifies the user.
     *
     * @param s Thread Name
     * @param e {@link LocalOperationException}
     */
    protected void threadThrowsLocalOperationException(String s, Exception e) {
        this.threadExceptions.put(s, new LocalOperationException(e));
        abortUploads(e);
    }

    /**
     * /**
     * Takes a {@link RequestException} from a running thread and stores it in
     * {@link #threadExceptions}.
     * Also stops the uploads and notifies the user.
     *
     * @param s Thread Name
     * @param e {@link RequestException}
     */
    protected void threadThrowsRequestException(String s, Exception e) {
        this.threadExceptions.put(s, new RequestException(e));
        abortUploads(e);
    }

    /**
     * Returns the assembly ID generated on client side to allow early logging. Be
     * aware the Assembly ID will change if
     * you use the {@link Assembly#wipeAssemblyID()} method.
     *
     * @return {@link String}AssemblyID
     */
    public String getClientSideGeneratedAssemblyID() {
        return assemblyId;
    }

    /**
     * Derives a new AssemblyID from an UUIDv4, without assigning it to the current
     * assembly.
     *
     * @return {@link String} Assembly ID
     */
    protected String generateAssemblyID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * Wipes the client side generated Assembly-ID. As a result, the assembly id is
     * assigned by the API after upload.
     * In this case you cannot obtain the Assembly ID before receiving a server
     * response. As a result every Assembly ID
     * obtained by {@link Assembly#getClientSideGeneratedAssemblyID()} would be
     * invalid.
     */
    public void wipeAssemblyID() {
        this.assemblyId = "";
    }

    /**
     * Obtains the suffix of the upload url depending on if a custom or client side
     * assembly ID has been set.
     *
     * @return upload url suffix
     */
    protected String obtainUploadUrlSuffix() {
        if (assemblyId == null || assemblyId.isEmpty()) {
            return "/assemblies";
        } else {
            return "/assemblies/" + assemblyId;
        }
    }

    /**
     * Returns the current Thread List.
     *
     * @return List of Type TusUploadRunnable
     */
    protected ArrayList<TusUploadRunnable> getThreadList() {
        return threadList;
    }
}
