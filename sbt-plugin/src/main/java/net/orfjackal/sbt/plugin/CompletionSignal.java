// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.util.concurrency.Semaphore;

import java.util.concurrent.atomic.AtomicBoolean;

public class CompletionSignal {
    private final Semaphore done = new Semaphore();
    private final AtomicBoolean result = new AtomicBoolean(false);

    public void begin() {
        done.down();
    }

    public void success() {
        result.set(true);
    }

    public void finished() {
        done.up();
    }

    public boolean waitForResult() {
        done.waitFor();
        return result.get();
    }
}
