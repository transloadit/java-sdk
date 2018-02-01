package com.transloadit.examples.async;

import com.transloadit.examples.ImageResizer;
import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.async.AsyncAssembly;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class AsyncExample {
    public static void main(String[] args) throws FileNotFoundException {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        Map<String, Object> stepOptions = new HashMap<String, Object>();
        stepOptions.put("width", 75);
        stepOptions.put("height", 75);
        stepOptions.put("resize_strategy", "pad");

        AsyncAssembly assembly = transloadit.newAssembly(new ProgressListener());
        assembly.addStep("resize", "/image/resize", stepOptions);

        assembly.addFile(new File(ImageResizer.class.getResource("/lol_cat.jpg").getFile()));
        assembly.addFile(new File(ImageResizer.class.getResource("/mona_lisa.jpg").getFile()));

        try {
            assembly.save();
            Thread.sleep(17000);
            System.out.println("about to pause ...");
            assembly.pauseUpload();
            System.out.println("upload just got paused ...");
            Thread.sleep(1000);
            assembly.resumeUpload();
            System.out.println("upload just got resumed ..");
            Thread.sleep(500);
            assembly.pauseUpload();
            System.out.println("upload just got paused ..");
            Thread.sleep(1000);
            System.out.println("will resume now ....");
            assembly.resumeUpload();

        } catch (RequestException | LocalOperationException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
