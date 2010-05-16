// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import java.io.*;

public class SbtRunner {

    private static final String PROMPT = "\n> ";
    private static final String PROMPT_AFTER_EMPTY_ACTION = "> ";

    private final ProcessRunner sbt;

    public SbtRunner(File workingDir, File launcherJar) {
        sbt = new ProcessRunner(workingDir, getCommand(launcherJar));
    }

    private static String[] getCommand(File launcherJar) {
        return new String[]{
                "java",
                "-Xmx512M",
                "-Dsbt.log.noformat=true",
                "-Djline.terminal=jline.UnsupportedTerminal",
                "-jar", launcherJar.getAbsolutePath()
        };
    }

    public OutputReader subscribeToOutput() throws IOException {
        return sbt.subscribeToOutput();
    }

    public void start() throws IOException {
        // TODO: detect if the directory does not have a project
        OutputReader output = sbt.subscribeToOutput();
        sbt.start();
        sbt.destroyOnShutdown();
        output.waitForOutput(PROMPT);
        output.close();
    }

    public void destroy() {
        sbt.destroy();
    }

    public void execute(String action) throws IOException {
        OutputReader output = sbt.subscribeToOutput();
        sbt.writeInput(action + "\n");

        if (action.trim().isEmpty()) {
            output.waitForOutput(PROMPT_AFTER_EMPTY_ACTION);
        } else {
            output.waitForOutput(PROMPT);
        }
        output.close();
    }
}
