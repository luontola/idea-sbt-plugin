// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;

public class ProcessRunner {

    private final ProcessBuilder builder;
    private Process process;
    private final MulticastPipe outputMulticast = new MulticastPipe();
    private Writer input;

    public ProcessRunner(File workingDir, String... command) {
        builder = new ProcessBuilder(command);
        builder.directory(workingDir);
        builder.redirectErrorStream(true);
    }

    public OutputReader subscribeToOutput() throws IOException {
        return new OutputReader(outputMulticast.subscribe());
    }

    public void start() throws IOException {
        process = builder.start();

        InputStreamReader output = new InputStreamReader(new BufferedInputStream(process.getInputStream()));
        Thread t = new Thread(new ReaderToWriterCopier(output, outputMulticast));
        t.setDaemon(true);
        t.start();

        input = new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()));
    }

    public void destroy() {
        process.destroy();
    }

    public void writeInput(String s) throws IOException {
        input.write(s);
        input.flush();
    }
}
