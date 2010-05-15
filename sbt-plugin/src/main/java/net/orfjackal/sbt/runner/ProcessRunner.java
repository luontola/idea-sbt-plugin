// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;

public class ProcessRunner {

    private final ProcessBuilder builder;

    private Process process;
    private InputStreamReader stdout;
    private OutputStreamWriter stdin;

    public ProcessRunner(File workingDir, String... command) {
        builder = new ProcessBuilder(command);
        builder.directory(workingDir);
        builder.redirectErrorStream(true);
    }

    public void start() throws IOException {
        process = builder.start();
        stdout = new InputStreamReader(new BufferedInputStream(process.getInputStream()));
        stdin = new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()));
    }

    public void destroy() {
        process.destroy();
    }

    public boolean waitForOutput(String expected) throws IOException {
        CyclicCharBuffer buffer = new CyclicCharBuffer(expected.length());
        int ch;
        while ((ch = stdout.read()) != -1) {
            buffer.append((char) ch);
            if (buffer.contentEquals(expected)) {
                return true;
            }
        }
        return false;
    }

    public void skipBufferedOutput() throws IOException {
        while (stdout.ready()) {
            stdout.skip(1);
        }
    }

    public void writeInput(String s) throws IOException {
        stdin.write(s);
        stdin.flush();
    }
}
