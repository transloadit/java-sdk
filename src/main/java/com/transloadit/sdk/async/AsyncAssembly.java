package com.transloadit.sdk.async;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusExecutor;
import io.tus.java.client.TusUploader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Deprecated because the {@link Assembly} is capable of asynchronous and pauseable / resumeable uploads now.
 * You can use {@link Assembly#pauseUploads()} and {@link Assembly#resumeUploads()} as a replacement.
 * This class represents a new assembly being created.
 * It is similar to {@link Assembly} but provides Asynchronous functionality.
 */
@Deprecated
public class AsyncAssembly extends Assembly {
    private AssemblyProgressListener progressListener;
    private UploadProgressListener uploadListener;
    private long uploadedBytes;
    private long totalUploadSize;
    private TusUploader lastTusUploader;

    @Nullable private String url;
    enum State {
        INIT,
        UPLOADING,
        PAUSED,
        UPLOAD_COMPLETE
    }
    State state;

    protected AsyncAssemblyExecutor executor;

    /**
     * Initializes a new {@link Assembly} object with asynchronous functionality.
     * @param transloadit {@link Transloadit} the transloadit client.
     * @param uploadListener {@link UploadProgressListener} tracks upload and completion of a background upload.
     */
    public AsyncAssembly(Transloadit transloadit, UploadProgressListener uploadListener) {
        super(transloadit);
        // make true by default to avoid breaking change
        shouldWaitForCompletion = true;
        this.uploadListener = uploadListener;
        state = State.INIT;
        uploadedBytes = 0;
        totalUploadSize = 0;
        lastTusUploader = null;
        url = null;
    }

    /**
     * Initializes a new {@link Assembly} object with asynchroneous functionality.
     * Calls {@link #AsyncAssembly(Transloadit, UploadProgressListener)}
     * @param transloadit {@link Transloadit} the transloadit client.
     * @param listener {@link AssemblyProgressListener} which gets converted to an {@link UploadProgressListener}.
     */
    public AsyncAssembly(Transloadit transloadit, final AssemblyProgressListener listener) {
        this(transloadit, toUploadProgressListener(listener));
        progressListener = listener;
    }

    /**
     * Converts an {@link AssemblyProgressListener} to an {@link UploadProgressListener}.
     * @param listener {@link AssemblyProgressListener} tracks upload and completion of a background upload and the
     * states of Assembly execution.
     * @return {@link UploadProgressListener} tracks upload and completion of a background upload.
     */
    private static UploadProgressListener toUploadProgressListener(final AssemblyProgressListener listener) {
       return new UploadProgressListener() {
            @Override
            public void onUploadFinished() {
                listener.onUploadFinished();
            }

            @Override
            public void onUploadProgress(long uploadedBytes, long totalBytes) {
                listener.onUploadProgress(uploadedBytes, totalBytes);
            }

            @Override
            public void onUploadFailed(Exception exception) {
                listener.onUploadFailed(exception);
            }

           @Override
           public void onParallelUploadsStarting(int parallelUploads, int uploadNumber) {

           }
       };
    }

    /**
     * Return the AssemblyProgresssListener that has been previously set.
     *
     * @return {@link AssemblyProgressListener}
     */
    public AssemblyProgressListener getListener() {
        return progressListener;
    }

    /**
     * Return the AssemblyProgresssListener that has been previously set.
     *
     * @return {@link UploadProgressListener}
     */
    public UploadProgressListener getUploadListener() {
        return uploadListener;
    }

    /**
     * Pauses the file upload. This is a blocking function that would try to wait till the assembly file uploads
     * have actually been paused if possible.
     *
     * @throws LocalOperationException if the method is called while no upload is going on.
     */
    public void pauseUpload() throws LocalOperationException {
        if (state == State.UPLOADING) {
            setState(State.PAUSED);
            executor.hardStop();
        } else {
            throw new LocalOperationException("Attempt to pause upload while assembly is not uploading");
        }
    }

    /**
     * Resumes the paused upload.
     *
     * @throws LocalOperationException if the upload hasn't been paused.
     */
    public void resumeUpload() throws LocalOperationException {
        if (state == State.PAUSED) {
            startExecutor();
        } else {
            throw new LocalOperationException("Attempt to resume un-paused upload");
        }
    }

    /**
     * Sets the state of the {@link AsyncAssembly} to the overhanded value.
     * @param state {@link State} represents states of Assembly execution
     */
    synchronized void setState(State state) {
        this.state = state;
    }

    /**
     * Returns always false to indicate to the {@link Assembly#save} method that it should never wait for the Assembly
     * to be complete by observing the HTTP - Response.
     * @return false
     * @see Assembly#shouldWaitWithoutSSE()
     * @see Assembly#save(boolean)
     */
    @Override
    protected boolean shouldWaitWithoutSSE() {
        return false;
    }

    /**
     * Runs intermediate check on the Assembly status until it is finished executing,
     * then returns it as a response.
     *
     * @return {@link AssemblyResponse}
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     * @throws RequestException if request to Transloadit server fails.
     */
    protected AssemblyResponse watchStatus() throws LocalOperationException, RequestException {
        return waitTillComplete(getClient().getAssemblyByUrl(url));
    }

    /**
     * Does the actual uploading of files (when tus is enabled).
     *
     * @throws IOException when there's a failure with file retrieval
     * @throws ProtocolException when there's a failure with tus upload
     */
    @Override
    protected void uploadTusFiles() throws IOException, ProtocolException {
        setState(State.UPLOADING);
        while (uploads.size() > 0) {
            final TusUploader tusUploader;
            // don't recreate uploader if it already exists.
            // this is to avoid multiple connections being open. And to avoid some connections left unclosed.
            if (lastTusUploader != null) {
                tusUploader = lastTusUploader;
                lastTusUploader = null;
            } else {
                tusUploader = tusClient.resumeOrCreateUpload(uploads.get(0));
                if (getUploadChunkSize() > 0) {
                    tusUploader.setChunkSize(getUploadChunkSize());
                }
            }

            TusExecutor tusExecutor = new TusExecutor() {
                @Override
                protected void makeAttempt() throws ProtocolException, IOException {
                    while (state == State.UPLOADING) {
                        int chunkUploaded = tusUploader.uploadChunk();
                        if (chunkUploaded > 0) {
                            uploadedBytes += chunkUploaded;
                            uploadListener.onUploadProgress(uploadedBytes, totalUploadSize);
                        } else {
                            // upload is complete
                            break;
                        }
                    }
                }
            };

            tusExecutor.makeAttempts();
            if (state != State.UPLOADING) {
                // if upload is paused, save the uploader so it can be reused on resume, then leave the method early.
                lastTusUploader = tusUploader;
                return;
            }

            // remove upload instance from list
            uploads.remove(0);
            tusUploader.finish();
        }

        setState(State.UPLOAD_COMPLETE);
    }

    /**
     * If tus uploads are enabled, this method would be called by {@link Assembly#save()} to handle the file uploads.
     *
     * @param response {@link AssemblyResponse}
     * @throws IOException when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     */
    @Override
    protected void handleTusUpload(AssemblyResponse response) throws IOException, ProtocolException {
        url = response.getSslUrl();
        totalUploadSize = getTotalUploadSize();
        processTusFiles(url, response.getTusUrl());
        startExecutor();
    }

    /**
     * Starts the executor that would manage the asynchronous submission of the assembly.
     */
    protected void startExecutor() {
        executor = new AsyncAssemblyExecutorImpl(new AssemblyRunnable());
        executor.execute();
    }

    class AssemblyRunnable implements Runnable {
        private AsyncAssemblyExecutorImpl executor;

        void setExecutor(AsyncAssemblyExecutorImpl executor) {
            this.executor = executor;
        }

        @Override
        public void run() {
            try {
                uploadTusFiles();
            } catch (ProtocolException e) {
                getUploadListener().onUploadFailed(e);
                executor.stop();
                return;
            } catch (IOException e) {
                getUploadListener().onUploadFailed(e);
                executor.stop();
                return;
            }

            if (state == State.UPLOAD_COMPLETE) {
                getUploadListener().onUploadFinished();
                if (!shouldWaitWithSSE() && shouldWaitForCompletion && (getListener() != null)) {
                    try {
                        getListener().onAssemblyFinished(watchStatus());
                    } catch (LocalOperationException | RequestException e) {
                        getListener().onAssemblyStatusUpdateFailed(e);
                    } finally {
                        executor.stop();
                    }
                } else {
                    executor.stop();
                }
            }
        }
    }

    // used for upload progress
    private long getTotalUploadSize() throws IOException {
        long size = 0;
        for (Map.Entry<String, File> entry : files.entrySet()) {
            size += entry.getValue().length();
        }

        for (Map.Entry<String, InputStream> entry : fileStreams.entrySet()) {
            size += entry.getValue().available();
        }
        return size;
    }

    /**
     * Provides a pattern for an AsyncAssemblyExecutor.
     */
    protected interface AsyncAssemblyExecutor {
        /**
         * starts the execution of the assembly on a separate thread.
         */
        void execute();

        /**
         * A blocking method that stops the execution of the assembly.
         * This method should wait till the execution is stopped if possible.
         */
        void hardStop();

        /**
         * A non-blocking method that stops the execution of the assembly.
         */
        void stop();
    }

    private class AsyncAssemblyExecutorImpl implements AsyncAssemblyExecutor {
        private final ExecutorService service;
        private Runnable runnable;

        AsyncAssemblyExecutorImpl(AsyncAssembly.AssemblyRunnable runnable) {
            this.runnable = runnable;
            runnable.setExecutor(this);
            service = Executors.newSingleThreadExecutor();
        }

        @Override
        public void execute() {
            service.execute(runnable);
        }

        @Override
        public void hardStop() {
            service.shutdown();
            boolean terminated = false;
            // wait till shutdown is done
            while (!terminated) {
                try {
                    terminated = service.awaitTermination(800, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void stop() {
            service.shutdown();
        }
    }
}
