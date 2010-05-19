// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import net.orfjackal.sbt.runner.*;

import java.io.*;

public class SbtProcessHandler extends ProcessHandler {

    private static final Logger LOG = Logger.getInstance(SbtProcessHandler.class.getName());

    private final SbtRunner sbt;
    private final OutputReader output;

    public SbtProcessHandler(SbtRunner sbt) {
        this.sbt = sbt;
        this.output = sbt.subscribeToOutput();
    }

    public void startNotify() {
        final NotifyWhenTextAvailable outputNotifier = new NotifyWhenTextAvailable(this, output);

        addProcessListener(new ProcessAdapter() {
            public void startNotified(ProcessEvent event) {
                Thread t = new Thread(outputNotifier);
                t.setDaemon(true);
                t.start();
            }
        });

        super.startNotify();
    }

    protected void destroyProcessImpl() {
        sbt.destroy();
    }

    protected void detachProcessImpl() {
        throw new UnsupportedOperationException("SBT cannot be detached");
    }

    public boolean detachIsDefault() {
        return false;
    }

    public OutputStream getProcessInput() {
        // TODO: how to allow sending input?
        return new OutputStream() {
            public void write(int b) {
                LOG.info("write: " + (char) b + "\t(" + b + ")");
                // do not allow sending commands through this
            }
        };
    }


    private static class NotifyWhenTextAvailable implements Runnable {
        private final ProcessHandler processHandler;
        private final Reader output;

        public NotifyWhenTextAvailable(ProcessHandler processHandler, Reader output) {
            this.processHandler = processHandler;
            this.output = output;
        }

        public void run() {
            try {
                char[] cbuf = new char[1024];
                int len;
                while ((len = output.read(cbuf)) != -1) {
                    String text = new String(cbuf, 0, len);
                    processHandler.notifyTextAvailable(text, ProcessOutputTypes.STDOUT);
                }
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }
}
