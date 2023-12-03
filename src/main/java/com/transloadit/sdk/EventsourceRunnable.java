package com.transloadit.sdk;

import com.launchdarkly.eventsource.*;

import java.net.URI;

public class EventsourceRunnable implements Runnable {
    protected boolean assemblyFinished;
    protected AssemblyListener assemblyListener;
    protected EventSource eventSource;

    /**
     * Constructor for {@link EventsourceRunnable}. It creates a new {@link EventSource} instance, wrapped in a
     * {@link Runnable} in order to make it usable by a {@link java.util.concurrent.ExecutorService}.
     * This constructor uses the standard {@link ConnectStrategy} of the {@link EventSource} library.
     * @param assemblyListener The {@link AssemblyListener} to be notified about the assembly status
     * @param statusUri The {@link URI} to the status endpoint of the assembly
     */
    public EventsourceRunnable(AssemblyListener assemblyListener, URI statusUri) {
        this.assemblyListener = assemblyListener;
        this.eventSource = new EventSource.Builder(statusUri).build();
    }

    /**
     * Constructor for {@link EventsourceRunnable}. It creates a new {@link EventSource} instance, wrapped in a
     * {@link Runnable} in order to make it usable by a {@link java.util.concurrent.ExecutorService}.
     * This constructor lets the user define the used {@link ConnectStrategy} and does not need a {@link URI} to the
     * status endpoint of the assembly, as it is inculded in the {@link ConnectStrategy} object.
     * @param assemblyListener The {@link AssemblyListener} to be notified about the assembly status
     * @param connectStrategy The {@link ConnectStrategy} to be used by the {@link EventSource} instance.
     */
    public EventsourceRunnable(AssemblyListener assemblyListener, ConnectStrategy connectStrategy) {
        this.assemblyListener = assemblyListener;

        EventSource.Builder builder = new EventSource.Builder(connectStrategy);
        this.eventSource = builder.build();
    }

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
                        handleMessageEvent(streamEvent);
                    } else if (streamEvent instanceof CommentEvent) {
                        handleCommentEvent(streamEvent);
                    } else if (streamEvent instanceof StartedEvent) {
                        handleStartedEvent(streamEvent);
                    } else {
                        handleFaultEvent(streamEvent);
                    }
                }
            } catch (StreamException e) {
                assemblyListener.onError(e);
            }
        }
    }

    protected void handleMessageEvent(MessageEvent messageEvent) {
        break;
    }

    protected void handleCommentEvent(CommentEvent commentEvent) {
        break;
    }

    protected void handleStartedEvent(StartedEvent startedEvent) {
        break;
    }

    protected void handleFaultEvent(FaultEvent faultEvent) {
        break;
    }

}
