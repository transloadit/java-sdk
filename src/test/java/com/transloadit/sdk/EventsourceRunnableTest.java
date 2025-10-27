package com.transloadit.sdk;

import com.launchdarkly.eventsource.ConnectStrategy;
import com.launchdarkly.eventsource.ErrorStrategy;
import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.RetryDelayStrategy;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventsourceRunnableTest {

    @Test
    void assemblyErrorAfterFinishedDoesNotNotifyListener() throws Exception {
        Transloadit transloadit = mock(Transloadit.class);
        AssemblyResponse initialResponse = mock(AssemblyResponse.class);
        AssemblyResponse finalResponse = mock(AssemblyResponse.class);

        when(initialResponse.getSslUrl()).thenReturn("https://example.com/assemblies/123");
        when(transloadit.getAssemblyByUrl(anyString())).thenReturn(finalResponse);
        when(finalResponse.json()).thenReturn(new JSONObject().put("ok", "ASSEMBLY_COMPLETED"));

        ConnectStrategy connectStrategy = ConnectStrategy.http(URI.create("http://localhost/sse"));
        RetryDelayStrategy retryStrategy = RetryDelayStrategy.defaultStrategy();
        ErrorStrategy errorStrategy = ErrorStrategy.alwaysContinue();

        RecordingListener listener = new RecordingListener();

        EventsourceRunnable runnable = new EventsourceRunnable(
                transloadit,
                initialResponse,
                listener,
                connectStrategy,
                retryStrategy,
                errorStrategy,
                false
        );

        MessageEvent finishedEvent = new MessageEvent("assembly_finished");
        MessageEvent errorEvent = new MessageEvent("assembly_error", "{}", null, null);
        runnable.handleMessageEvent(finishedEvent);
        runnable.handleMessageEvent(errorEvent);

        assertTrue(listener.finishedCalled.get(), "Expected assembly_finished to notify listener");
        assertFalse(listener.errorCalled.get(), "Unexpected error callback after completion");
        assertNotNull(listener.finishedResponse.get(), "Final response missing");
        assertEquals(finalResponse, listener.finishedResponse.get());
    }

    private static final class RecordingListener implements AssemblyListener {
        private final AtomicBoolean finishedCalled = new AtomicBoolean(false);
        private final AtomicBoolean errorCalled = new AtomicBoolean(false);
        private final AtomicReference<AssemblyResponse> finishedResponse = new AtomicReference<>();

        @Override
        public void onAssemblyFinished(AssemblyResponse response) {
            finishedCalled.set(true);
            finishedResponse.set(response);
        }

        @Override
        public void onError(Exception error) {
            errorCalled.set(true);
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
        public void onAssemblyProgress(JSONObject progress) {
        }

        @Override
        public void onAssemblyResultFinished(JSONArray result) {
        }
    }
}
