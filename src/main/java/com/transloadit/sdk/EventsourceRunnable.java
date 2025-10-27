package com.transloadit.sdk;

import com.launchdarkly.eventsource.CommentEvent;
import com.launchdarkly.eventsource.ConnectStrategy;
import com.launchdarkly.eventsource.ErrorStrategy;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.FaultEvent;
import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.RetryDelayStrategy;
import com.launchdarkly.eventsource.StartedEvent;
import com.launchdarkly.eventsource.StreamEvent;
import com.launchdarkly.eventsource.StreamException;
import com.launchdarkly.logging.LDLogger;
import com.launchdarkly.logging.Logs;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

class EventsourceRunnable implements Runnable {
    protected boolean assemblyFinished;
    protected AssemblyListener assemblyListener;

    protected AssemblyResponse response;
    protected EventSource eventSource;

    protected Transloadit transloadit;

    /**
     * Constructor for {@link EventsourceRunnable}. It creates a new {@link EventSource} instance, wrapped in a
     * {@link Runnable} in order to make it usable by a {@link java.util.concurrent.ExecutorService}.
     * This constructor lets the user define the used {@link ConnectStrategy} and does not need an URI to the
     * status endpoint of the assembly, as it is inculded in the {@link ConnectStrategy} object.
     *
     * @param transloadit      The {@link Transloadit} instance to be used to fetch the assembly response.
     * @param response         The {@link AssemblyResponse} object containing the assembly response.
     * @param assemblyListener The {@link AssemblyListener} to be notified about the assembly status
     * @param connectStrategy  The {@link ConnectStrategy} to be used by the {@link EventSource} instance.
     * @param retryStrategy    The {@link RetryDelayStrategy} to be used by the {@link EventSource} instance.
     * @param errorStrategy    The {@link ErrorStrategy} to be used by the {@link EventSource} instance.
     * @param printDebug       Boolean to enable debug output.
     */
    EventsourceRunnable(Transloadit transloadit, AssemblyResponse response, AssemblyListener assemblyListener,
                               ConnectStrategy connectStrategy, RetryDelayStrategy retryStrategy, ErrorStrategy errorStrategy, Boolean printDebug) {
        this.transloadit = transloadit;
        this.response = response;
        this.assemblyListener = assemblyListener;

        EventSource.Builder builder = new EventSource.Builder(connectStrategy).retryDelayStrategy(retryStrategy).errorStrategy(errorStrategy);
        if (printDebug) {
            builder.logger(LDLogger.withAdapter(Logs.toConsole(), "SSELogger"));
        }

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
            Iterable<StreamEvent> events = eventSource.anyEvents();
            Iterator<StreamEvent> eventIterator = events.iterator();
            if (eventIterator.hasNext()) {
                StreamEvent streamEvent = eventIterator.next();
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

        if (assemblyFinished) {
            shutdownEventSource();
            return;
        }

        // Check if the event is a message event without
        if (eventName.equals("message")) {
            switch (data) {
                case "assembly_finished":
                    assemblyFinished = true;
                    try {
                        assemblyListener.onAssemblyFinished(transloadit.getAssemblyByUrl(response.getSslUrl()));
                    } catch (RequestException  | LocalOperationException e) {
                        assemblyListener.onError(e);
                    } finally {
                        shutdownEventSource();
                    }
                    break;
                case "assembly_upload_meta_data_extracted":
                    assemblyListener.onMetadataExtracted();
                    break;

                case "assembly_uploading_finished":
                    assemblyListener.onAssemblyUploadFinished();
                    break;
                default:
                    // Default - Do nothing with unknown events.

                    // Debug output, uncomment if necessary:
                    // System.out.printf("Unknown Message: %s\n", data);
            }
        } else {
            switch (eventName) {
                case "assembly_upload_finished":
                    JSONObject payload = new JSONObject(data);
                    assemblyListener.onFileUploadFinished(payload);
                    break;

                case "assembly_result_finished":
                    JSONArray resultArray = new JSONArray(data);
                    assemblyListener.onAssemblyResultFinished(resultArray);
                    break;

                case "assembly_error":
                    if (assemblyFinished) {
                        shutdownEventSource();
                        break;
                    }
                    assemblyListener.onError(new RequestException(data));
                    shutdownEventSource();
                    break;

                case "assembly_execution_progress":
                    JSONObject executionProgress = new JSONObject(data);

                    assemblyListener.onAssemblyProgress(executionProgress);
                    break;
                default:
                    // Debug output, uncomment if needed
                    // System.out.printf("Unknown Event: %s\n", data);
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
        if (assemblyFinished) {
            shutdownEventSource();
        }
        // Debug output, uncomment if needed
        // String data = faultEvent.toString();
        // System.out.printf("Fault: %s\n", data);
        // System.out.println("Starting Over");
    }

    private void shutdownEventSource() {
        if (this.eventSource == null) {
            return;
        }
        try {
            this.eventSource.stop();
        } catch (Exception ignore) {
            // Ignore cleanup exceptions
        }
        try {
            this.eventSource.close();
        } catch (Exception ignore) {
            // Ignore cleanup exceptions
        }
    }

}
