// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;

import java.io.*;
import java.util.*;

public class SbtRunner {

    private static final String PROMPT = "\n> ";
    private static final String FAILED_TO_COMPILE_PROMPT = "Hit enter to retry or 'exit' to quit:";
    private static final String PROMPT_AFTER_EMPTY_ACTION = "> ";
    private static final Logger LOG = Logger.getInstance("#orfjackal.sbt.runner.SbtRunner");

    private final ProcessRunner sbt;

    public SbtRunner(File workingDir, File launcherJar, String[] vmParameters) {
        if (!workingDir.isDirectory()) {
            throw new IllegalArgumentException("Working directory does not exist: " + workingDir);
        }
        if (!launcherJar.isFile()) {
            throw new IllegalArgumentException("Launcher JAR file does not exist: " + launcherJar);
        }
        sbt = new ProcessRunner(workingDir, getCommand(launcherJar, vmParameters));
    }

    private static String[] getCommand(File launcherJar, String[] vmParameters) {
        List<String> command = new ArrayList<String>();

        command.add("java");
        command.addAll(Arrays.asList(vmParameters));
        command.addAll(Arrays.asList(
                "-Dsbt.log.noformat=true",
                "-Djline.terminal=jline.UnsupportedTerminal",
                "-jar",
                launcherJar.getAbsolutePath()
        ));

        LOG.info("SBT command line: " + StringUtil.join(command, " "));
        return command.toArray(new String[command.size()]);
    }

    public OutputReader subscribeToOutput() {
        return sbt.subscribeToOutput();
    }

    public void start() throws IOException {
        // TODO: detect if the directory does not have a project
        sbt.start();
        sbt.destroyOnShutdown();
        // Don't wait for the prompt, as we are on the AWT Event Dispatch Thread. Startup can take
        // some time, if downloading Scala JAR files.
    }

    public void destroy() {
        sbt.destroy();
    }

    public boolean isAlive() {
        return sbt.isAlive();
    }

    public void execute(String action) throws IOException {
        OutputReader output = sbt.subscribeToOutput();
        sbt.writeInput(action + "\n");

        if (action.trim().equals("")) {
            output.waitForOutput(Arrays.asList(PROMPT_AFTER_EMPTY_ACTION, FAILED_TO_COMPILE_PROMPT));
        } else {
            output.waitForOutput(Arrays.asList(PROMPT, FAILED_TO_COMPILE_PROMPT));
        }
        output.close();
    }
}
