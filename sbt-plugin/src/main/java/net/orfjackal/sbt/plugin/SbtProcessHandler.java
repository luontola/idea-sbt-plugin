// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.plugin;

import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import net.orfjackal.sbt.runner.OutputReader;

import java.io.*;

public class SbtProcessHandler extends ProcessHandler {

    private static final Logger logger = Logger.getInstance(SbtProcessHandler.class.getName());

    private final SbtRunnerComponent sbt;
    private final OutputReader output;

    public SbtProcessHandler(SbtRunnerComponent sbt, OutputReader output) {
        this.sbt = sbt;
        this.output = output;
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

    public OutputStream getProcessInput() {
        return new ExecuteUserEnteredActions(sbt);
    }

    protected void destroyProcessImpl() {
        sbt.destroyProcess();
    }

    protected void detachProcessImpl() {
        throw new UnsupportedOperationException("SBT cannot be detached");
    }

    public boolean detachIsDefault() {
        return false;
    }


    private static class NotifyWhenTextAvailable implements Runnable {
        private final SbtProcessHandler process;
        private final Reader output;

        public NotifyWhenTextAvailable(SbtProcessHandler process, Reader output) {
            this.process = process;
            this.output = output;
        }

        public void run() {
            try {
                char[] cbuf = new char[100];
                int len;
                while ((len = output.read(cbuf)) != -1) {
                    String text = new String(cbuf, 0, len);
                    String withoutCr = text.replace("\r", "");
                    process.notifyTextAvailable(withoutCr, ProcessOutputTypes.STDOUT);
                }
            } catch (IOException e) {
                logger.error(e);
            } finally {
                process.notifyProcessTerminated(0);
            }
        }
    }

    private static class ExecuteUserEnteredActions extends OutputStream {
        private final SbtRunnerComponent sbt;
        private final StringBuilder commandBuffer = new StringBuilder();

        public ExecuteUserEnteredActions(SbtRunnerComponent sbt) {
            this.sbt = sbt;
        }

        public void write(int b) {
            char ch = (char) b;
            if (ch == '\n') {
                sbt.executeInBackground(buildCommand());
            } else {
                commandBuffer.append(ch);
            }
        }

        private String buildCommand() {
            String command = commandBuffer.toString().trim();
            commandBuffer.setLength(0);
            return command;
        }
    }
}
