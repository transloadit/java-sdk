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

public class AsyncAssembly extends Assembly {
    private AssemblyProgressListener listener;
    private AsyncAssemblyExecutor executor;
    private long uploadedBytes;
    private long totalUploadSize;
    private TusUploader lastTusUploader;
    @Nullable private String url;
    enum State {
        INIT,
        UPLOADING,
        PAUSED,
        UPLOAD_COMPLETE,
        FINISHED  // this state is never really used, but it makes the flow more definite.
    }
    State state;

    public AsyncAssembly(Transloadit transloadit, AssemblyProgressListener listener) {
        super(transloadit);
        this.listener = listener;
        state = State.INIT;
        uploadedBytes = 0;
        totalUploadSize = 0;
        lastTusUploader = null;
        url = null;
    }

    public AssemblyProgressListener getListener() {
        return listener;
    }

    /**
     * Pauses the file upload. This is a blocking function that would wait till the assembly file uploads
     * have actually been paused.
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

    synchronized private void setState(State state) {
        this.state = state;
    }

    /**
     * Runs intermediate check on the Assembly status until it is finished executing,
     * then returns it as a response.
     *
     * @return {@link AssemblyResponse}
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     * @throws RequestException if request to transloadit server fails.
     */
    protected AssemblyResponse watchStatus() throws LocalOperationException, RequestException {
        AssemblyResponse response;
        do {
            response = getClient().getAssemblyByUrl(url);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new LocalOperationException(e);
            }
        } while (!response.isFinished());

        setState(State.FINISHED);
        return response;
    }

    /**
     * Does the actual uploading of files (when tus is enabled)
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
            }

            TusExecutor tusExecutor = new TusExecutor() {
                @Override
                protected void makeAttempt() throws ProtocolException, IOException {
                    while (state == State.UPLOADING && tusUploader.uploadChunk() > 0) {
                        uploadedBytes += tusUploader.getOffset();
                        listener.onUploadPogress(uploadedBytes, totalUploadSize);
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
     * If tus uploads are enabled, this method would be called by {@link Assembly#save()} to handle the file uploads
     *
     * @param response {@link AssemblyResponse}
     * @throws IOException when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     */
    @Override
    protected void handleTusUpload(AssemblyResponse response) throws IOException, ProtocolException {
        url = response.getSslUrl();
        totalUploadSize = getTotalUploadSize();
        processTusFiles(url);
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
                getListener().onUploadFailed(e);
                executor.stop();
                return;
            } catch (IOException e) {
                getListener().onUploadFailed(e);
                executor.stop();
                return;
            }

            if (state == State.UPLOAD_COMPLETE) {
                getListener().onUploadFinished();
                try {
                    getListener().onAssemblyFinished(watchStatus());
                } catch (LocalOperationException e) {
                    getListener().onAssemblyStatusUpdateFailed(e);
                } catch (RequestException e) {
                    getListener().onAssemblyStatusUpdateFailed(e);
                } finally {
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
}
