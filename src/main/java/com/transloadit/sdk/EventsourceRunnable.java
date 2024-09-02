package com.transloadit.sdk;

import com.launchdarkly.eventsource.CommentEvent;
import com.launchdarkly.eventsource.ConnectStrategy;
import com.launchdarkly.eventsource.ErrorStrategy;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.FaultEvent;
import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.StartedEvent;
import com.launchdarkly.eventsource.StreamEvent;
import com.launchdarkly.eventsource.StreamException;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;

public class EventsourceRunnable implements Runnable {
    protected boolean assemblyFinished;
    protected AssemblyListener assemblyListener;

    protected AssemblyResponse response;
    protected EventSource eventSource;

    protected Transloadit transloadit;

    /**
     * Constructor for {@link EventsourceRunnable}. It creates a new {@link EventSource} instance, wrapped in a
     * {@link Runnable} in order to make it usable by a {@link java.util.concurrent.ExecutorService}.
     * This constructor uses the standard {@link ConnectStrategy} of the {@link EventSource} library.
     * @param transloadit The {@link Transloadit} instance to be used to fetch the assembly response.
     * @param response The {@link AssemblyResponse} object containing the assembly response.
     * @param assemblyListener The {@link AssemblyListener} to be notified about the assembly status
     * @param statusUri The {@link URI} to the status endpoint of the assembly
     */
    public EventsourceRunnable(Transloadit transloadit, AssemblyResponse response, AssemblyListener assemblyListener, URI statusUri) {
        this.transloadit = transloadit;
        this.response = response;
        this.assemblyListener = assemblyListener;
        this.eventSource = new EventSource.Builder(statusUri).build();
    }

    /**
     * Constructor for {@link EventsourceRunnable}. It creates a new {@link EventSource} instance, wrapped in a
     * {@link Runnable} in order to make it usable by a {@link java.util.concurrent.ExecutorService}.
     * This constructor lets the user define the used {@link ConnectStrategy} and does not need a {@link URI} to the
     * status endpoint of the assembly, as it is inculded in the {@link ConnectStrategy} object.
     * @param transloadit The {@link Transloadit} instance to be used to fetch the assembly response.
     * @param response The {@link AssemblyResponse} object containing the assembly response.
     * @param assemblyListener The {@link AssemblyListener} to be notified about the assembly status
     * @param connectStrategy The {@link ConnectStrategy} to be used by the {@link EventSource} instance.
     * @param errorStrategy The {@link ErrorStrategy} to be used by the {@link EventSource} instance.
     */
    public EventsourceRunnable(Transloadit transloadit, AssemblyResponse response, AssemblyListener assemblyListener,
                               ConnectStrategy connectStrategy, ErrorStrategy errorStrategy) {
        this.transloadit = transloadit;
        this.response = response;
        this.assemblyListener = assemblyListener;

        EventSource.Builder builder = new EventSource.Builder(connectStrategy).errorStrategy(errorStrategy);
        this.eventSource = builder.build();
    }

    /**
     * The {@link Runnable} implementation of the {@link EventsourceRunnable}. Runs a {@link EventSource} instance and
     * parses the incoming events. The events are then passed to the {@link AssemblyListener} to notify the user about them.
     */
    @Override
    public void run() {
        this.assemblyFinished = false;
        try {
            eventSource.start();
        } catch (StreamException e) {
            assemblyListener.onError(e);
        }

        while (!assemblyFinished) {
            try {
                StreamEvent streamEvent = eventSource.readAnyEvent();
                if (streamEvent != null) {
                    if (streamEvent instanceof MessageEvent) {
                        handleMessageEvent((MessageEvent) streamEvent);
                    } else if (streamEvent instanceof CommentEvent) {
                        handleCommentEvent((CommentEvent) streamEvent);
                    } else if (streamEvent instanceof StartedEvent) {
                        handleStartedEvent((StartedEvent) streamEvent);
                    } else {
                        handleFaultEvent((FaultEvent) streamEvent);
                    }
                }
            } catch (StreamException e) {
                assemblyListener.onError(e);
            }
        }
    }

    /**
     * This SDK uses the {@link EventSource} library to receive events from the Transloadit API.
     * It classifies Transloadit's events always as {@link MessageEvent}. The other event types are not used.
     * Therefore, this method handles all transloadit sent events.
     * <p>
     * Handles the {@link MessageEvent} received from the {@link EventSource}.
     * @param messageEvent The {@link MessageEvent} to be handled.
     */
    protected void handleMessageEvent(MessageEvent messageEvent) {

        String eventName = messageEvent.getEventName();
        String data = messageEvent.getData();

        // Check if the event is a message event without
        if (eventName.equals("message")) {
            switch (data) {
                case "assembly_finished":
                    assemblyFinished = true;
                    try {
                        assemblyListener.onAssemblyFinished(transloadit.getAssemblyByUrl(response.getSslUrl()));
                    } catch (RequestException  | LocalOperationException e) {
                        assemblyListener.onError(e);
                    }
                    this.eventSource.close();
                    break;
                case "assembly_upload_meta_data_extracted":
                    assemblyListener.onMetadataExtracted();
                    break;

                case "assembly_uploading_finished":
                    assemblyListener.onAssemblyUploadFinished();
                    break;
                default:
                    LocalOperationException e = new LocalOperationException("Unknown SSE message: " + data);
                    assemblyListener.onError(e);
            }
        } else {
            switch (eventName) {
                case "assembly_upload_finished":
                    JSONObject payload = new JSONObject(data);
                    String fileName = payload.getString("name");
                    assemblyListener.onFileUploadFinished(fileName, payload);
                    break;

                case "assembly_result_finished":
                    JSONArray result = new JSONArray(data);
                    String stepName = result.getString(0);
                    JSONObject stepResult = result.getJSONObject(1);
                    assemblyListener.onAssemblyResultFinished(stepName, stepResult);
                    break;

                case "assembly_error":
                    assemblyListener.onError(new RequestException(data));
                    this.eventSource.close();
                    System.exit(1);
                    break;

                case "assembly_execution_progress":
                    JSONObject executionProgress = new JSONObject(data);
                    double overallProgress;
                    // Address the case where the progress_combined key is not present in the JSON object
                    try {
                        overallProgress = executionProgress.getDouble("progress_combined");
                    } catch (Exception e) {
                        overallProgress = 0;
                    }

                    // Address the case where the progress_per_original_file key is not present in the JSON object
                    JSONObject progressPerOriginalFile;
                    try {
                        progressPerOriginalFile = executionProgress.getJSONObject("progress_per_original_file");
                    } catch (Exception e) {
                        progressPerOriginalFile = new JSONObject();
                    }

                    assemblyListener.onAssemblyProgress(overallProgress, progressPerOriginalFile);
                    break;
                default:
                    LocalOperationException e = new LocalOperationException("Unknown SSE message: " + data);
                    assemblyListener.onError(e);
            }
        }

    }

    protected void handleCommentEvent(CommentEvent commentEvent) {
        // Comment events are not used in this SDK, but can be handled here.
    }

    protected void handleStartedEvent(StartedEvent startedEvent) {
        // Debug output, uncomment if needed
        // String data = startedEvent.toString();
        // System.out.printf("Started: %s\n", data);
    }

    protected void handleFaultEvent(FaultEvent faultEvent) {
        // Debug output, uncomment if needed
        // String data = faultEvent.toString();
        // System.out.printf("Fault: %s\n", data);
        // System.out.println("Starting Over");
    }

}
